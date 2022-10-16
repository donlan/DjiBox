package com.dooze.djibox

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.amap.api.maps.model.LatLng
import com.dooze.djibox.databinding.FragmentHotPointConfigBinding
import com.dooze.djibox.extensions.lazyFast
import com.dooze.djibox.extensions.showSnack
import com.dooze.djibox.internal.controller.DJISampleApplication
import dji.common.error.DJIError
import dji.common.mission.hotpoint.HotpointHeading
import dji.common.mission.hotpoint.HotpointMission
import dji.common.mission.hotpoint.HotpointMissionEvent
import dji.common.mission.hotpoint.HotpointStartPoint
import dji.common.model.LocationCoordinate2D
import dji.sdk.mission.hotpoint.HotpointMissionOperator
import dji.sdk.mission.hotpoint.HotpointMissionOperatorListener
import dji.sdk.sdkmanager.DJISDKManager
import pdb.app.base.extensions.roundedCorner

/**
 * @author: liangguidong
 * @date: 2022/8/26 21:39
 * @lastModifyUser: liangguidong
 * @lastModifyDate: 2022/8/26 21:39
 * @description:
 */
class HotPointConfigFragment : Fragment(R.layout.fragment_hot_point_config), View.OnClickListener,
    HotpointMissionOperatorListener {

    private val binding by lazyFast {
        FragmentHotPointConfigBinding.bind(requireView())
    }

    private var hotPointOperator: HotpointMissionOperator? = null

    private val point: LatLng by lazyFast { requireArguments().getParcelable("point")!! }
    private val radius by lazyFast { requireArguments().getDouble("radius") }

    var configReceiver: ((mission: HotpointMission) -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivClose.roundedCorner(-1)
        binding.ivClose.setOnClickListener(this)
        binding.ibDone.setOnClickListener(this)
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
                DJISampleApplication.getEventBus().post(ResetHotPoint)
                dismiss()
            }
            R.id.ibDone -> {

                val mission = HotpointMission().apply {
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
                val operator = hotPointOperator
                    ?: DJISDKManager.getInstance().missionControl.hotpointMissionOperator.also {
                        hotPointOperator = it
                        it.addListener(this)
                    }
                operator.startMission(mission) {
                    if (it != null) {
                        binding.root.showSnack(
                            getString(
                                R.string.hot_pooint_start_mission_error,
                                "${it.description}(${it.errorCode})"
                            )
                        )
                    }
                }
            }
        }
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
}

