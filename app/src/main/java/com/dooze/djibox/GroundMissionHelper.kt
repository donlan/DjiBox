package com.dooze.djibox

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.amap.api.maps.MapView
import com.amap.api.maps.model.*
import com.dooze.djibox.extensions.mapTo
import com.dooze.djibox.extensions.showSnack
import com.dooze.djibox.map.IPickPointMarker
import com.dooze.djibox.utils.MapUtils
import com.dooze.djibox.utils.toDJILocation
import com.dooze.djibox.widgets.*
import dji.common.error.DJIError
import dji.common.mission.hotpoint.HotpointMission
import dji.sdk.mission.MissionControl
import dji.sdk.mission.timeline.TimelineElement
import dji.sdk.mission.timeline.TimelineEvent
import dji.sdk.mission.timeline.actions.GoHomeAction
import dji.sdk.mission.timeline.actions.HotpointAction
import pdb.app.base.extensions.dp
import pdb.app.base.extensions.dpInt
import pdb.app.base.extensions.getColorCompat
import pdb.app.base.extensions.removeFromParent
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min

/**
 * @author 梁桂栋
 * @date 2022/10/9  23:32.
 * e-mail 760625325@qq.com
 * GitHub: https://github.com/donlan
 * description: com.dooze.djibox
 * @version 1.0
 */
class GroundMissionHelper : IPickPointMarker, MissionControl.Listener {

    private var isInConfig: Boolean = false
    private var mapView: MapView? = null
    private var activity: ControllerActivity? = null

    private var currentGroupPolygon: Polygon? = null
    private var currentText: Text? = null

    private val markers = ArrayList<Marker>()
    private val circles = ArrayList<Circle>()

    fun init(mapView: MapView, activity: ControllerActivity) {
        this.mapView = mapView
        this.activity = activity
    }

    fun markStartWapPoint() {
        val mapView = mapView ?: return
        val activity = activity ?: return
        activity.changeMapViewMode(true)
        markers.forEach { it.remove() }
        markers.clear()
        AppAlert(
            activity, activity.getString(R.string.map_long_press_to_pick),
            activity.getString(R.string.ground_mission_start_pick_content)
        ) {
            if (it.id == R.id.commonStart) {
                activity.changeMapViewMode(true)
                isInConfig = true
            }
        }.addActions(AppAlert.cancelAction(activity), AppAlert.startAction(activity)).show()
    }

    override fun onPickPoint(point: LatLng): Boolean {
        if (!isInConfig) return false
        val mapView = mapView ?: return false
        val activity = activity ?: return false
        val context = mapView.context ?: return false

        val marker = mapView.map.addMarker(MarkerOptions().apply {
            position(point)
            val drawable = ResourcesCompat.getDrawable(context.resources, R.drawable.ic_loc, null)!!
            drawable.setTint(ContextCompat.getColor(context, R.color.alert))
            this.icon(BitmapDescriptorFactory.fromBitmap(drawable.toBitmap()))
        })
        if (markers.size >= 2) {
            markers.onEach { it.remove() }
            markers.clear()
            currentGroupPolygon?.remove()
            currentText?.remove()
            circles.onEach { it.remove() }
            circles.clear()
        }
        markers.add(marker)

        if (markers.size == 2) {
            currentGroupPolygon?.remove()
            currentText?.remove()
            circles.onEach { it.remove() }
            val groundWidth = MapUtils.horizontalDistance(
                markers[0].position,
                markers[1].position
            ).toInt()
            val groundHeight = MapUtils.verticalDistance(markers[0].position, markers[1].position)
                .toInt()
            currentText = mapView.map.addText(
                TextOptions().position(MapUtils.center(markers[0].position, markers[1].position))
                    .fontSize(20)
                    .backgroundColor(context.getColorCompat(R.color.map_radius_fill))
                    .fontColor(context.getColorCompat(R.color.textColorSecondary))
                    .text(
                        "长宽:${groundWidth} * $groundHeight (${groundWidth * groundHeight}平方)"
                    )
            )
            val points = buildRect()!!
            currentGroupPolygon = mapView.map.addPolygon(
                PolygonOptions().addAll(points)
                    .fillColor(context.getColorCompat(R.color.map_radius_fill))
                    .strokeColor(context.getColorCompat(R.color.map_radius_stoker))
                    .strokeWidth(2.dp(context))
            )

            val len = min(groundWidth, groundHeight) / 2
            val circle = addCircle(context, markers.first().position, 0.0)
            circles.add(circle)
            SeekBarAction(activity, 5, len.coerceAtLeast(6).coerceAtMost(500),
                context.getString(R.string.fun_ground_mission_set_radius_title),
                onProgressChanged = {
                    circle.center = createPoint(it.toInt(), -it.toInt())
                    circle.radius = it.toDouble()
                }, onOkClick = {
                    autoFillCircles()
                }).attachBottom(activity.contentRoot)
        }

        return true
    }

    private fun autoFillCircles() {
        val diff = 3 / 4f
        val context = requireNotNull(activity)
        val startCircle = circles.first()
        val start = markers[0].position
        val end = markers[1].position
        val w = MapUtils.horizontalDistance(start, end)
        val h = MapUtils.verticalDistance(start, end)
        val col = ceil(w / (startCircle.radius * 2 * diff)).toInt()
        val row = ceil(h / (startCircle.radius * 2 * diff)).toInt()
        circles.onEach { it.remove() }
        circles.clear()
        val baseOffset = (startCircle.radius * 2 * diff).toInt()
        val hAdditional = (row * baseOffset - h)
        val wAdditional = (col * baseOffset - w)
        val fromPoint = createPoint(
            -(wAdditional).toInt(),
            (hAdditional).toInt(),
            startCircle.center
        )

        for (r in 0 until row) {
            for (l in 0 until col) {
                circles.add(
                    addCircle(
                        context,
                        createPoint(baseOffset * l, -baseOffset * r, fromPoint),
                        startCircle.radius
                    )
                )
            }
        }
        startCircle.remove()
        showConfig()

    }

    private fun showConfig() {
        val point = circles.first().center ?: return
        val radius = circles.first().radius
        val configFragment = HotPointConfigFragment.newInstance(
            point.toDJILocation().mapTo {
                LatLng(latitude, longitude)
            },
            radius,
            activity?.getString(R.string.fun_ground_mission_config_title),
            configReceiver = { mission ->
                val missionControl = MissionControl.getInstance()
                missionControl.scheduleElements(circles.map {
                    val hotPointMission = HotpointMission()
                    hotPointMission.resetMissionWithData(mission)
                    hotPointMission.hotpoint = it.center.toDJILocation()
                    HotpointAction(hotPointMission, 360f)
                }.toMutableList<TimelineElement>().apply {
                    add(GoHomeAction())
                }.toList())
                missionControl.addListener(this)
                missionControl.startTimeline()
                setupTaskUI()
            })
        requireNotNull(activity).supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, configFragment)
            .show(configFragment)
            .commit()
    }

    private fun clear() {
        currentText?.remove()
        currentGroupPolygon?.remove()
        markers.onEach { it.remove() }
        markers.clear()
        circles.onEach { it.remove() }
        circles.clear()
        isInConfig = false
    }

    private fun setupTaskUI() {
        val activity = requireNotNull(activity)
        ActionView(activity).apply {
            wrap(LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(TextView(activity).apply {
                    minHeight = 40.dpInt(context)
                    gravity= Gravity.CENTER_VERTICAL
                    text = context.getString(R.string.fun_groundMission_point_count_hint, circles.size)
                })
            })
            setupCancel()
            onCloseClick = {
                this.removeFromParent()
                clear()
                MissionControl.getInstance().stopTimeline()
            }
            onOkClick = {

            }
        }.attachBottom(activity.contentRoot)
    }

    private fun addCircle(context: Context, center: LatLng, radius: Double): Circle {
        return mapView!!.map.addCircle(CircleOptions().apply {
            center(center)
            radius(radius)
            fillColor(
                ContextCompat.getColor(
                    context,
                    R.color.map_highlight_radius_fill
                )
            )
            strokeWidth(1.dp(context))
            strokeColor(
                ContextCompat.getColor(
                    context,
                    R.color.map_highlight_radius_stoker
                )
            )
        })
    }

    private fun createPoint(offsetX: Int, offsetY: Int, fromPoint: LatLng? = null): LatLng {
        val start = markers[0].position
        val end = markers[1].position
        val w = MapUtils.horizontalDistance(start, end)
        val h = MapUtils.verticalDistance(start, end)
        val wd = abs(start.longitude - end.longitude) / w
        val hd = abs(start.latitude - end.latitude) / h
        val point = fromPoint ?: start
        return LatLng(point.latitude + offsetY * hd, point.longitude + offsetX * wd)
    }

    private fun buildRect(): List<LatLng>? {
        if (markers.size < 2) return null
        val p0 = markers[0].position
        val p2 = markers[1].position
        val p1 = LatLng(p0.latitude, p2.longitude)
        val p3 = LatLng(p2.latitude, p0.longitude)
        return listOf(p0, p1, p2, p3)
    }

    override fun onEvent(p0: TimelineElement?, p1: TimelineEvent?, p2: DJIError?) {
        p2?.let {
            activity?.showSnack("环绕飞行任务异常：${it.description}(${it.errorCode})")
        }
    }
}