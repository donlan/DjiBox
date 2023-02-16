package com.dooze.djibox

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.amap.api.maps.model.LatLng
import com.dooze.djibox.databinding.FragmentHotPointConfigBinding
import com.dooze.djibox.events.HotPointMissionEvent
import com.dooze.djibox.extensions.lazyFast
import com.dooze.djibox.extensions.showSnack
import com.dooze.djibox.internal.controller.App
import dji.common.error.DJIError
import dji.common.mission.hotpoint.HotpointHeading
import dji.common.mission.hotpoint.HotpointMission
import dji.common.mission.hotpoint.HotpointMissionEvent
import dji.common.mission.hotpoint.HotpointStartPoint
import dji.common.model.LocationCoordinate2D
import dji.keysdk.FlightControllerKey
import dji.keysdk.KeyManager
import dji.sdk.mission.MissionControl
import dji.sdk.mission.hotpoint.HotpointMissionOperator
import dji.sdk.mission.hotpoint.HotpointMissionOperatorListener
import dji.sdk.mission.timeline.TimelineElement
import dji.sdk.mission.timeline.TimelineEvent
import dji.sdk.mission.timeline.actions.GoHomeAction
import dji.sdk.mission.timeline.actions.HotpointAction
import dji.sdk.mission.timeline.actions.ShootPhotoAction
import dji.sdk.mission.timeline.actions.TakeOffAction
import dji.sdk.sdkmanager.DJISDKManager
import kotlinx.coroutines.launch
import pdb.app.base.extensions.roundedCorner

/**
 * @author: liangguidong
 * @date: 2022/8/26 21:39
 * @lastModifyUser: liangguidong
 * @lastModifyDate: 2022/8/26 21:39
 * @description:
 */
class HotPointConfigFragment : Fragment(R.layout.fragment_hot_point_config), View.OnClickListener,
    HotpointMissionOperatorListener, MissionControl.Listener {

    private val binding by lazyFast {
        FragmentHotPointConfigBinding.bind(requireView())
    }

    private val point: LatLng by lazyFast { requireArguments().getParcelable("point")!! }
    private val radius by lazyFast { requireArguments().getDouble("radius") }

    var configReceiver: ((mission: HotpointMission) -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivClose.roundedCorner(-1)
        binding.ivClose.setOnClickListener(this)
        binding.ibDone.setOnClickListener(this)
        binding.ibDone.setOnLongClickListener {
            fallback2(createMission())
            true
        }
        requireArguments().getString("title")?.let {
            binding.tvHint.text = it
        }
        updateSpeedProgress()
        binding.speedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateSpeedProgress()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })


    }


    fun dismiss() {
        parentFragmentManager.beginTransaction()
            .remove(this)
            .commit()
    }

    private fun updateSpeedProgress() {
        val progress = binding.speedSeekBar.progress
        binding.tvSpeedValue.text = "$progress m/s"
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.ivClose -> {
                App.getEventBus().post(ResetHotPoint)
                dismiss()
            }
            R.id.ibDone -> {
                val mission = createMission()
                val error = mission.checkParameters()
                if (error != null) {
                    requireActivity().showSnack(
                        getString(
                            R.string.hot_pooint_config_error,
                            "${error.description}(${error.errorCode})"
                        )
                    )
                    return
                }
                if (configReceiver != null) {
                    configReceiver?.invoke(mission)
                    dismiss()
                    return
                }
                val elements = ArrayList<TimelineElement>()
                elements.add(TakeOffAction())
                elements.add(ShootPhotoAction.newShootIntervalPhotoAction(10, 5))
                elements.add(HotpointAction(mission, 360f))
                elements.add(GoHomeAction())
                App.getEventBus().post(HotPointMissionEvent(elements))
            }
        }
    }

    private fun createMission(): HotpointMission {
        return HotpointMission().apply {
            this.hotpoint = LocationCoordinate2D(point.latitude, point.longitude)
            this.radius = this@HotPointConfigFragment.radius
            this.altitude = binding.etAltitude.text.toString().toDoubleOrNull() ?: 0.0
            this.heading = when (binding.rgHeading.checkedRadioButtonId) {
                R.id.headingLookingForward -> HotpointHeading.ALONG_CIRCLE_LOOKING_FORWARDS
                R.id.headingLookingBackward -> HotpointHeading.ALONG_CIRCLE_LOOKING_BACKWARDS
                R.id.headingTowardHotPoint -> HotpointHeading.TOWARDS_HOT_POINT
                R.id.headingAwayFromHotPoint -> HotpointHeading.AWAY_FROM_HOT_POINT
                R.id.headingRemoteController -> HotpointHeading.CONTROLLED_BY_REMOTE_CONTROLLER
                else -> HotpointHeading.USING_INITIAL_HEADING
            }
            this.angularVelocity = binding.speedSeekBar.progress.toFloat()
            this.isClockwise = binding.switchClockwise.isChecked
            this.startPoint = when (binding.rgStartPoint.checkedRadioButtonId) {
                R.id.startPointEast -> HotpointStartPoint.EAST
                R.id.startPointNorth -> HotpointStartPoint.NORTH
                R.id.startPointSouth -> HotpointStartPoint.SOUTH
                R.id.startPointWest -> HotpointStartPoint.WEST
                else -> HotpointStartPoint.NEAREST
            }
        }
    }

    private fun fallback2(mission: HotpointMission) {
        var isRun = false
        App.getAircraftInstance().flightController?.run {
            this.setStateCallback {
                val homeLatitude = it.homeLocation.latitude
                val homeLongitude = it.homeLocation.longitude
                this.setStateCallback(null)
                if (!isRun) {
                    timeline(homeLatitude, homeLongitude, mission)
                }
                isRun = true
            }
        } ?: kotlin.run {
            var baseLatitude = point.latitude
            var baseLongitude = point.longitude
            val latitudeValue = KeyManager.getInstance()
                .getValue(FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LATITUDE))
            val longitudeValue = KeyManager.getInstance()
                .getValue(FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LONGITUDE))

            if (latitudeValue != null && latitudeValue is Double) {
                baseLatitude = latitudeValue
            }
            if (longitudeValue != null && longitudeValue is Double) {
                baseLongitude = longitudeValue
            }
            timeline(baseLatitude, baseLongitude, mission)
        }


    }

    private fun timeline(lat: Double, lng: Double, mission: HotpointMission) {
        val missionControl = MissionControl.getInstance()
        val elements = ArrayList<TimelineElement>()
        val hotpointMission = HotpointMission()
        hotpointMission.hotpoint = LocationCoordinate2D(lat, lng)
        hotpointMission.altitude = mission.altitude
        hotpointMission.radius = mission.radius
        hotpointMission.angularVelocity = mission.angularVelocity
        val startPoint = HotpointStartPoint.NEAREST
        hotpointMission.startPoint = startPoint
        val heading = HotpointHeading.TOWARDS_HOT_POINT
        hotpointMission.heading = heading
        elements.add(HotpointAction(hotpointMission, 360f))
        val error = missionControl.scheduleElements(elements)
        missionControl.addListener(this)
        if (error != null && error.errorCode != 0) {
            binding.root.showSnack(
                getString(
                    R.string.hot_pooint_execution_error,
                    "Fallback2:${error.description}(${error.errorCode})"
                )
            )
            return
        }
        dismiss()
    }

    override fun onExecutionUpdate(p0: HotpointMissionEvent) {
        binding.root.showSnack(
            getString(
                R.string.hot_pooint_execution_update,
                "state = ${p0.currentState}"
            )
        )
    }

    override fun onExecutionStart() {
    }

    override fun onExecutionFinish(p0: DJIError?) {
        if (p0 == null) {
            binding.root.showSnack(
                getString(
                    R.string.hot_pooint_execution_finsish_success
                )
            )
        } else {
            binding.root.showSnack(
                getString(
                    R.string.hot_pooint_execution_error,
                    "${p0.description}(${p0.errorCode})"
                )
            )
        }
    }

    override fun onDestroyView() {
        MissionControl.getInstance().removeAllListeners()
        DJISDKManager.getInstance().missionControl.hotpointMissionOperator.removeListener(this)
        super.onDestroyView()
    }


    companion object {
        fun newInstance(
            point: LatLng,
            radius: Double,
            title: String? = null,
            configReceiver: ((mission: HotpointMission) -> Unit)? = null
        ): HotPointConfigFragment {
            val fragment = HotPointConfigFragment()
            fragment.configReceiver = configReceiver
            fragment.arguments = Bundle().apply {
                putParcelable("point", point)
                putDouble("radius", radius)
                putString("title", title)
            }
            return fragment
        }
    }

    override fun onEvent(p0: TimelineElement?, p1: TimelineEvent?, p2: DJIError?) {
        viewLifecycleOwner.lifecycleScope.launch {
            kotlin.runCatching {
                binding.root.showSnack(
                    "Timeline Update $p1 (${p2?.description}:${p2?.errorCode})"
                )
            }
        }
    }
}

