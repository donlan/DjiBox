package com.dooze.djibox

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.collection.ArrayMap
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.maps.AMap
import com.amap.api.maps.AMapUtils
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.*
import com.dooze.djibox.databinding.ActivityWayPointBinding
import com.dooze.djibox.databinding.FragmentWayPointConfigBinding
import com.dooze.djibox.databinding.SheetWaypointConfigBinding
import com.dooze.djibox.extensions.behavior
import com.dooze.djibox.extensions.makeVibrate
import com.dooze.djibox.extensions.showSnack
import com.dooze.djibox.utils.toDJILocation
import com.dooze.djibox.widgets.MarkerIndexView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dji.common.error.DJIError
import dji.common.mission.waypoint.*
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener
import dji.sdk.sdkmanager.DJISDKManager
import pdb.app.base.extensions.toggleVisible

/**
 * @author: liangguidong
 * @date: 2022/8/6 14:10
 * @lastModifyUser: liangguidong
 * @lastModifyDate: 2022/8/6 14:10
 * @description:
 */
class WapPointActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityWayPointBinding

    private var isPickPointMode = false

    private val markers = ArrayList<Marker>()
    private val pointConfigs = ArrayMap<Marker, Waypoint>()

    private var locationClient: AMapLocationClient? = null

    private var currentMission: WaypointMission? = null
    private var waypointMissionBuilder: WaypointMission.Builder? = null

    private val waypointMissionOperator by lazy {
        DJISDKManager.getInstance().missionControl.waypointMissionOperator
    }

    private val waypointMissionOperatorListener = object : WaypointMissionOperatorListener {
        override fun onDownloadUpdate(p0: WaypointMissionDownloadEvent) {

        }

        override fun onUploadUpdate(p0: WaypointMissionUploadEvent) {
        }

        override fun onExecutionUpdate(p0: WaypointMissionExecutionEvent) {
        }

        override fun onExecutionStart() {
        }

        override fun onExecutionFinish(err: DJIError?) {
            if (err != null) {
                getString(
                    R.string.waypoint_operator_error,
                    err.errorCode.toString(),
                    err.description
                )
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_way_point)
        binding = ActivityWayPointBinding.bind(findViewById(R.id.root_layout))
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.mapView.onCreate(savedInstanceState)
        binding.tvPickPoint.setOnClickListener(this)
        binding.tvPopPoint.setOnClickListener(this)
        binding.tvConfig.setOnClickListener(this)
        binding.tvUpload.setOnClickListener(this)
        binding.tvStart.setOnClickListener(this)
        binding.tvStop.setOnClickListener(this)
        binding.ivMyLocation.setOnClickListener(this)
        binding.ivLayer.setOnClickListener(this)

        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.actionMenuToggle -> {
                    binding.layoutMenus.toggleVisible()
                    it.icon = ContextCompat.getDrawable(
                        this, if (binding.layoutMenus.isVisible) {
                            R.drawable.ic_cancel
                        } else {
                            R.drawable.ic_apps
                        }
                    )
                }
            }
            true
        }


        binding.mapView.map.setOnMapLongClickListener {
            if (isPickPointMode) {
                if (markers.isNotEmpty()) {
                    val point = markers.last().position
                    val distance = AMapUtils.calculateLineDistance(point, it)
                    if (distance > 1000) {
                        showSnack(getString(R.string.waypoint_distance_too_far))
                        return@setOnMapLongClickListener
                    }
                }
                markers.add(binding.mapView.map.addMarker(MarkerOptions().apply {
                    position(it)
                    this.icon(BitmapDescriptorFactory.fromView(MarkerIndexView(this@WapPointActivity).apply {
                        text = (markers.size + 1).toString()
                    }))
                }))
                makeVibrate()
            }
        }
        binding.mapView.map.addOnMarkerClickListener { marker ->
            PointConfigSheet.show(supportFragmentManager, pointConfigs[marker]) {
                pointConfigs[marker] = it
            }
            true
        }
        binding.mapView.map.myLocationStyle = MyLocationStyle().apply {
            this.showMyLocation(true)
            radiusFillColor(Color.TRANSPARENT)
            myLocationType(MyLocationStyle.LOCATION_TYPE_SHOW)
        }

        AMapLocationClient.updatePrivacyShow(this, true, true)
        AMapLocationClient.updatePrivacyAgree(this, true)

        binding.mapView.map.isMyLocationEnabled = true
        val locationClient = AMapLocationClient(this)


        this.locationClient = locationClient
        locationClient.setLocationOption(AMapLocationClientOption().apply {
            locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
        })
        var firstLocation = true
        locationClient.setLocationListener {
            binding.mapView.map.myLocation.set(it)
            if (firstLocation) {
                zoomTo(it.point)
            }
            firstLocation = false
        }
        locationClient.startLocation()

        waypointMissionOperator.addListener(waypointMissionOperatorListener)

        onClick(binding.ivMyLocation)
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationClient?.onDestroy()
        locationClient = null
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tvPickPoint -> {
                v.isSelected = !isPickPointMode
                isPickPointMode = !isPickPointMode
            }
            R.id.tvPopPoint -> {
                if (markers.isEmpty()) {
                    showSnack(getString(R.string.waypoint_no_more_ponit))
                    return
                }
                markers.removeLast().run {
                    pointConfigs.remove(this)
                    remove()
                }
                makeVibrate()
            }
            R.id.tvConfig -> {
                showConfigSheet()
            }
            R.id.tvUpload -> {
                uploadMission()
            }
            R.id.tvStart -> {
                startMission()
            }
            R.id.tvStop -> {
                stopMission()
            }
            R.id.ivMyLocation -> {
                locationClient?.lastKnownLocation?.let {
                    zoomTo(it.point, 17f)
                }
            }
            R.id.ivLayer -> {
                PopupMenu(this, v, Gravity.TOP).apply {
                    this.menuInflater.inflate(R.menu.map_layer_menu, menu)
                    setOnMenuItemClickListener { menu ->
                        val type = when (menu.itemId) {
                            R.id.mayLayerNavi -> {
                                AMap.MAP_TYPE_NAVI
                            }
                            R.id.mayLayerSatellite -> {
                                AMap.MAP_TYPE_SATELLITE
                            }
                            else -> {
                                AMap.MAP_TYPE_NORMAL
                            }
                        }
                        binding.mapView.map.mapType = type
                        true
                    }
                }.show()
            }
        }
    }

    private fun zoomTo(point: LatLng, zoom: Float = 17f) {
        binding.mapView.map.moveCamera(
            CameraUpdateFactory.newLatLngZoom(point, zoom)
        )
    }

    private fun showConfigSheet() {

        if (markers.size < 2) {
            showSnack(getString(R.string.waypoint_warning_add_point_before_config))
            return
        }


        ConfigSheet().apply {
            confirmCallback = { speed: Int,
                                altitude: Float,
                                finishAction: WaypointMissionFinishedAction,
                                headingMode: WaypointMissionHeadingMode,
                                pathMode: WaypointMissionFlightPathMode ->

                val builder = (waypointMissionBuilder ?: WaypointMission.Builder())
                    .finishedAction(finishAction)
                    .autoFlightSpeed(speed.toFloat())
                    .maxFlightSpeed(speed.toFloat())
                    .headingMode(headingMode)
                    .flightPathMode(pathMode)
                    .repeatTimes(1)
                    .gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY)
                    .setMissionID(1)
                    .waypointList(markers.map {
                        val latLng = it.position.toDJILocation()
                        val point = pointConfigs[it]?.apply {
                            this.coordinate = latLng
                        } ?: Waypoint(latLng.latitude, latLng.longitude, altitude)
                        point
                    })
                    .waypointCount(markers.size)


                val precheckError = builder.checkParameters()
                if (precheckError != null) {
                    showSnack(
                        getString(
                            R.string.waypoint_config_error,
                            precheckError.description
                        )
                    )
                } else {
                    waypointMissionBuilder = builder
                    currentMission = builder.build()
                    val error = waypointMissionOperator.loadMission(currentMission!!)
                    if (error == null) {
                        showSnack(getString(R.string.waypoint_config_success))
                    } else {
                        showSnack(
                            getString(
                                R.string.waypoint_config_error,
                                error.errorCode.toString()
                            )
                        )
                    }
                }
            }
        }.show(supportFragmentManager, ConfigSheet::class.java.simpleName)
    }

    private fun uploadMission() {
        if (waypointMissionOperator.loadedMission == null) {
            showSnack(getString(R.string.waypoint_upload_need_configed))
            return
        }
        waypointMissionOperator.uploadMission {
            if (it == null) {
                showSnack(getString(R.string.waypoint_upload_success))
            } else {
                showSnack(
                    getString(
                        R.string.waypoint_upload_error_and_retry,
                        it.errorCode.toString()
                    )
                )
                waypointMissionOperator.retryUploadMission(null)
            }
        }
    }

    private fun startMission() {
        waypointMissionOperator.startMission {
            if (it == null) {
                showSnack(getString(R.string.waypoint_start_mission_success))
            } else {
                showSnack(it.description)
            }
        }
    }

    private fun stopMission() {
        waypointMissionOperator.stopMission {
            if (it == null) {
                showSnack(getString(R.string.waypoint_stop_mission_success))
            } else {
                showSnack(it.description)
            }
        }
    }
}

class ConfigSheet : BottomSheetDialogFragment() {

    var confirmCallback: ((
        speed: Int,
        altitude: Float,
        finishAction: WaypointMissionFinishedAction,
        headingMode: WaypointMissionHeadingMode,
        pathMode: WaypointMissionFlightPathMode
    ) -> Unit)? = null

    private var binding: SheetWaypointConfigBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return SheetWaypointConfigBinding.inflate(layoutInflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding!!
        binding.speedSeekBar.progress = 5
        binding.tvSpeedValue.text = "${binding.speedSeekBar.progress} m/s"
        binding.tvConfirm.setOnClickListener {
            confirmCallback?.invoke(
                binding.speedSeekBar.progress,
                binding.etAltitude.text.toString().toFloat(),
                when (binding.finishActionGroup.checkedRadioButtonId) {
                    R.id.tvActionGoHome -> WaypointMissionFinishedAction.GO_HOME
                    R.id.tvActionToFirstPoint -> WaypointMissionFinishedAction.GO_FIRST_WAYPOINT
                    R.id.tvActionAutoLand -> WaypointMissionFinishedAction.AUTO_LAND
                    else -> WaypointMissionFinishedAction.NO_ACTION
                },
                when (binding.headingActionGroup.checkedRadioButtonId) {
                    R.id.tvRemoteController -> WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER
                    R.id.tvInterestPoint -> WaypointMissionHeadingMode.TOWARD_POINT_OF_INTEREST
                    R.id.tvInitialDirection -> WaypointMissionHeadingMode.USING_INITIAL_DIRECTION
                    R.id.tvWaypointHeading -> WaypointMissionHeadingMode.USING_WAYPOINT_HEADING
                    else -> WaypointMissionHeadingMode.AUTO
                },
                when (binding.pathModeGroup.checkedRadioButtonId) {
                    R.id.tvPathModeCurved -> WaypointMissionFlightPathMode.CURVED
                    else -> WaypointMissionFlightPathMode.NORMAL
                }
            )
            confirmCallback = null
            dismissAllowingStateLoss()
        }
        binding.speedSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                binding.tvSpeedValue.text = "$progress m/s"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
    }

    override fun onStart() {
        super.onStart()
        behavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }
}

class PointConfigSheet : BottomSheetDialogFragment(), View.OnClickListener {

    private var waypoint: Waypoint? = null

    private var binding: FragmentWayPointConfigBinding? = null

    private var doneAction: ((waypoint: Waypoint) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return FragmentWayPointConfigBinding.inflate(layoutInflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding!!) {
            ivClose.setOnClickListener(this@PointConfigSheet)
            ibDone.setOnClickListener(this@PointConfigSheet)
            waypoint?.let {
                etAltitude.setText(it.altitude.toString())
                speedSeek.progress = it.speed
                headingSeek.progress = it.heading.toFloat()
                gimbalPitchSeek.progress = it.gimbalPitch
                cornerRadiusSeek.progress = it.cornerRadiusInMeters
                repeatTimesSeek.progress = it.actionRepeatTimes.toFloat()
                shootIntervalSeek.progress = it.shootPhotoTimeInterval
            }
        }
    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.ivClose -> {
                dismissAllowingStateLoss()
            }
            R.id.ibDone -> {
                val binding = binding ?: return
                val waypoint = (waypoint ?: Waypoint()).apply {
                    altitude = binding.etAltitude.text.toString().toFloatOrNull() ?: 5f
                    speed = binding.speedSeek.progress
                    heading = binding.headingSeek.progress.toInt()
                    gimbalPitch = binding.gimbalPitchSeek.progress
                    cornerRadiusInMeters = binding.cornerRadiusSeek.progress
                    actionRepeatTimes = binding.repeatTimesSeek.progress.toInt()
                    shootPhotoTimeInterval = binding.shootIntervalSeek.progress
                }
                doneAction?.invoke(waypoint)
                dismissAllowingStateLoss()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        behavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    companion object {
        fun show(
            fm: FragmentManager,
            waypoint: Waypoint? = null,
            doneAction: (waypoint: Waypoint) -> Unit
        ) {
            val sheet = PointConfigSheet()
            sheet.waypoint = waypoint
            sheet.doneAction = doneAction
            sheet.show(fm, PointConfigSheet::class.java.simpleName)
        }
    }


}

val AMapLocation.point: LatLng
    get() = LatLng(this.latitude, this.longitude)