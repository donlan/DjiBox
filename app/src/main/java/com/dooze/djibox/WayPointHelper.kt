package com.dooze.djibox

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.collection.ArrayMap
import androidx.core.view.forEach
import androidx.core.view.forEachIndexed
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.amap.api.maps.AMapUtils
import com.amap.api.maps.MapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.dooze.djibox.events.HotPointMissionConfigEvent
import com.dooze.djibox.extensions.showSnack
import com.dooze.djibox.flow.FlowLayout
import com.dooze.djibox.map.IPickPointMarker
import com.dooze.djibox.utils.toDJILocation
import com.dooze.djibox.widgets.*
import dji.common.error.DJIError
import dji.common.mission.waypoint.*
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener
import dji.sdk.sdkmanager.DJISDKManager
import pdb.app.base.extensions.dpInt
import pdb.app.base.extensions.removeFromParent
import pdb.app.base.extensions.updatePadding
import java.util.*
import kotlin.collections.ArrayList

/**
 * @author 梁桂栋
 * @date 2022/9/25  16:51.
 * e-mail 760625325@qq.com
 * GitHub: https://github.com/donlan
 * description: com.dooze.djibox.internal
 * @version 1.0
 */
class WayPointHelper : IPickPointMarker {

    private var mapView: MapView? = null
    private var activity: ControllerActivity? = null
    private var flowLayout: ViewGroup? = null

    private var actionView: ActionView? = null

    private var index  = 1

    private var isInConfig = false
    set(value) {
        field = value
        if(!value) {
            index = 1
        }
    }

    private val markers = ArrayList<Marker>()
    private val pointConfigs = ArrayMap<Marker, Waypoint>()

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
                activity?.showSnack(
                    activity?.getString(
                        R.string.waypoint_operator_error,
                        err.errorCode.toString(),
                        err.description
                    ) ?: return
                )
            }
        }

    }


    fun init(mapView: MapView, activity: ControllerActivity) {
        this.mapView = mapView
        this.activity = activity
    }

    fun markStartWapPoint() {
        val mapView = mapView ?: return
        val activity = activity ?: return
        markers.forEach { it.remove() }
        markers.clear()
        pointConfigs.clear()
        AppAlert(
            activity, activity.getString(R.string.map_long_press_to_pick),
            activity.getString(R.string.waypoint_start_pick_content)
        ) {
            if (it.id == R.id.commonStart) {
                activity.changeMapViewMode(true)
                isInConfig = true
            }
        }.addActions(AppAlert.cancelAction(activity), AppAlert.startAction(activity)).show()
    }

    @SuppressLint("SetTextI18n")
    override fun onPickPoint(point: LatLng): Boolean {
        if (!isInConfig) return false
        val mapView = mapView ?: return false
        val context = mapView.context ?: return false
        if (markers.isNotEmpty()) {
            val p = markers.last().position
            val distance = AMapUtils.calculateLineDistance(p, point)
            if (distance > 1000) {
                mapView.showSnack(context.getString(R.string.waypoint_distance_too_far))
                return true
            }
        }
        val id = UUID.randomUUID().toString()
        val indexTex = (index++).toString()
        val markerView = MarkerIndexView(context).apply {
            text = indexTex
            this.id = id
        }
        markerView.setOnLongClickListener { mv ->
            val index = markers.indexOfFirst { it.title == (mv as MarkerIndexView).id }
            val marker = markers.removeAt(index)
            marker.remove()
            mv.removeFromParent()
            pointConfigs.remove(marker)
            if(markers.isEmpty()) {
                this@WayPointHelper.index = 1
            }
            true
        }
        requireActionView()
        requireNotNull(flowLayout).addView(
            markerView,
            ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        markers.add(mapView.map.addMarker(MarkerOptions().apply {
            this.title(id)
            position(point)
            this.icon(BitmapDescriptorFactory.fromView(MarkerIndexView(context).apply {
                text = indexTex
            }))
        }))
        return true
    }

    private fun requireActionView(): ActionView? {
        val activity = activity ?: return null
        val parent = mapView?.parent as? RelativeLayout ?: return null
        if (actionView == null) {
            actionView = ActionView(activity).apply {
                onCloseClick = {
                    markers.forEach { it.remove() }
                    markers.clear()
                    pointConfigs.clear()
                    actionView?.isVisible = false
                    flowLayout?.removeAllViews()
                    isInConfig = false
                }
                onOkClick = {
                    onDoneImpl()
                }
                setupCancel()
            }
            val contentLayout = LinearLayout(activity)
            contentLayout.orientation = LinearLayout.VERTICAL
            val content = FlowLayout(activity)
            flowLayout = content
            content.itemSpacing = 8.dpInt(activity)
            content.lineSpacing = 10.dpInt(activity)
            contentLayout.updatePadding(vertical = 10.dpInt(activity))
            contentLayout.addView(
                content,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            actionView!!.wrap(contentLayout)
        }
        if (actionView!!.parent == null) {
            actionView!!.attachBottom(parent)
        }
        actionView?.isVisible = true
        return actionView!!
    }

    private fun onDoneImpl() {
        val activity = activity ?: return
        showConfigSheet(activity)
    }

    private fun showConfigSheet(activity: ControllerActivity) {

        if (markers.size < 2) {
            activity.showSnack(activity.getString(R.string.waypoint_warning_add_point_before_config))
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
                    activity.showSnack(
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
                        activity.showSnack(getString(R.string.waypoint_config_success))
                        uploadMission(activity)
                    } else {
                        activity.showSnack(
                            getString(
                                R.string.waypoint_config_error,
                                error.errorCode.toString()
                            )
                        )
                    }
                }
            }
        }.show(activity.supportFragmentManager, ConfigSheet::class.java.simpleName)
    }

    private fun uploadMission(activity: ControllerActivity) {
        if (waypointMissionOperator.loadedMission == null) {
            activity.showSnack(activity.getString(R.string.waypoint_upload_need_configed))
            return
        }
        activity.showSnack(activity.getString(R.string.waypoint_start_task_upload))
        waypointMissionOperator.uploadMission {
            if (it == null) {
                activity.showSnack(activity.getString(R.string.waypoint_upload_success))
                startMission(activity)
            } else {
                activity.showSnack(
                    activity.getString(
                        R.string.waypoint_upload_error_and_retry,
                        it.errorCode.toString()
                    )
                )
                waypointMissionOperator.retryUploadMission(null)
            }
        }
    }

    private fun startMission(activity: ControllerActivity) {
        activity.showSnack(activity.getString(R.string.waypoint_start_task))
        waypointMissionOperator.startMission {
            if (it == null) {
                activity.showSnack(activity.getString(R.string.waypoint_start_mission_success))
            } else {
                activity.showSnack(it.description)
            }
        }
    }

    private fun stopMission(activity: ControllerActivity) {
        waypointMissionOperator.stopMission {
            if (it == null) {
                activity.showSnack(activity.getString(R.string.waypoint_stop_mission_success))
            } else {
                activity.showSnack(it.description)
            }
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val activity = activity ?: return false
        if (isInConfig && markers.find { it == marker } != null) {
            PointConfigSheet.show(activity.supportFragmentManager, pointConfigs[marker]) {
                pointConfigs[marker] = it
            }
        }
        return isInConfig
    }
}