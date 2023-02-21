package com.dooze.djibox

import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.amap.api.maps.MapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.Circle
import com.amap.api.maps.model.CircleOptions
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.dooze.djibox.events.HotPointMissionConfigEvent
import com.dooze.djibox.extensions.mapTo
import com.dooze.djibox.extensions.message
import com.dooze.djibox.extensions.showSnack
import com.dooze.djibox.internal.controller.App
import com.dooze.djibox.internal.utils.ModuleVerificationUtil
import com.dooze.djibox.internal.utils.ToastUtils
import com.dooze.djibox.map.IPickPointMarker
import com.dooze.djibox.utils.toDJILocation
import com.dooze.djibox.widgets.ActionView
import com.dooze.djibox.widgets.AppAlert
import com.dooze.djibox.widgets.SeekbarText
import com.dooze.djibox.widgets.cancelAction
import com.dooze.djibox.widgets.startAction
import com.squareup.otto.Subscribe
import dji.common.camera.SettingsDefinitions
import dji.common.error.DJIError
import dji.common.util.CommonCallbacks
import dji.waypointv2.natives.util.NativeCallbackUtils.CommonCallback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pdb.app.base.extensions.dp
import pdb.app.base.extensions.dpInt
import pdb.app.base.extensions.removeFromParent

/**
 * @author 梁桂栋
 * @date 2022/9/25  10:22.
 * e-mail 760625325@qq.com
 * GitHub: https://github.com/donlan
 * description: com.dooze.djibox
 * @version 1.0
 */
class HotPointHelper : IPickPointMarker {

    private var isStartedPick = false

    private var hotPointMarker: Marker? = null

    private var mapView: MapView? = null
    private var activity: ControllerActivity? = null

    private var radiusCircle: Circle? = null


    private var radiusSeekbarText: SeekbarText? = null
    private var radiusActionView: ActionView? = null

    fun init(mapView: MapView, activity: ControllerActivity) {
        this.mapView = mapView
        this.activity = activity
        App.getEventBus().register(this)
    }

    @Subscribe
    fun onResetHotPoint(reset: ResetHotPoint) {
        isStartedPick = false
        radiusCircle?.remove()
        hotPointMarker?.remove()
    }


    fun markStartHotPoint(activity: ControllerActivity) {
        val mapView = mapView ?: return
        AppAlert(
            activity, activity.getString(R.string.map_long_press_to_pick),
            activity.getString(R.string.hotpoint_start_pick_content)
        ) {
            if (it.id == R.id.commonStart) {
                activity.changeMapViewMode(true)
                isStartedPick = true
            }
        }.addActions(AppAlert.cancelAction(activity), AppAlert.startAction(activity)).show()
    }

    override fun onPickPoint(point: LatLng): Boolean {
        if (!isStartedPick) return false
        val mapView = mapView ?: return false
        val context = mapView.context ?: return false
        hotPointMarker?.remove()
        radiusCircle?.remove()
        hotPointMarker = mapView.map.addMarker(MarkerOptions().apply {
            position(point)
            val drawable = ResourcesCompat.getDrawable(context.resources, R.drawable.ic_loc, null)!!
            drawable.setTint(ContextCompat.getColor(context, R.color.alert))
            this.icon(BitmapDescriptorFactory.fromBitmap(drawable.toBitmap()))
        })

        requireRadiusSeekView()
        return true
    }

    private fun requireRadiusSeekView() {
        val mapView = mapView ?: return
        val parent = mapView.parent as? RelativeLayout ?: return
        if (radiusActionView == null) {
            radiusActionView = ActionView(parent.context).apply {
                wrap(SeekbarText(parent.context).apply {
                    radiusSeekbarText = this
                    this.progress
                    setTitle(parent.context.getString(R.string.hotpoint_set_radius))
                    updatePadding(16.dpInt(parent.context), 16.dpInt(parent.context))
                    setup(5, 500, 1f)
                    progress = 50f
                    onProgressChanged = { radius ->
                        setValuesText(radius.toString())
                        hotPointMarker?.let { marker ->
                            val circle =
                                radiusCircle ?: mapView.map.addCircle(CircleOptions().apply {
                                    center(marker.position)
                                    radius(radius.toDouble())
                                    fillColor(
                                        ContextCompat.getColor(
                                            parent.context,
                                            R.color.map_radius_fill
                                        )
                                    )
                                    strokeWidth(1.dp(parent.context))
                                    strokeColor(
                                        ContextCompat.getColor(
                                            parent.context,
                                            R.color.map_radius_stoker
                                        )
                                    )
                                }).also {
                                    radiusCircle = it
                                }
                            circle.center = marker.position
                            circle.radius = radius.toDouble()
                        }


                    }
                    onProgressChanged?.invoke(progress)
                })
                onOkClick = {
                    isStartedPick = false
                    removeFromParent()
                    showConfig()
                }
            }
        }
        if (radiusActionView!!.parent == null) {
            radiusActionView!!.attachBottom(parent)
        }
    }

    private fun showConfig() {
        val point = hotPointMarker?.position ?: return
        val radius = radiusCircle?.radius ?: return
        val configFragment = HotPointConfigFragment.newInstance(point.toDJILocation().mapTo {
            LatLng(latitude, longitude)
        }, radius)
        requireNotNull(activity).supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, configFragment)
            .show(configFragment)
            .commit()
    }

    fun startCapture(event: HotPointMissionConfigEvent, takePhotoAction: () -> Unit) {
        val ac = activity ?: return
        val mission = event.mission
        val len = mission.radius * Math.PI * 2
        val v = mission.angularVelocity
        val timeSec = len / v
        val intervalToTake = timeSec / event.takePhotoCount
        ac.showSnack("开始拍照：飞行距离：$len,角速度：$v,时长：$timeSec, 拍照数量：${event.takePhotoCount}($intervalToTake)")
        ac.lifecycleScope.launch {
            kotlin.runCatching {
                if (event.takePhotoByApi) {
                    val camera = App.getAircraftInstance().camera
                    if (ModuleVerificationUtil.isMatrice300RTK() || ModuleVerificationUtil.isMavicAir2()) {
                        camera.setFlatMode(
                            SettingsDefinitions.FlatCameraMode.PHOTO_SINGLE
                        ) { djiError: DJIError? ->
                            if (djiError != null) {
                                ac.showSnack("拍照失败：${djiError.message}")
                            }
                        }
                    } else {
                        camera.setMode(
                            SettingsDefinitions.CameraMode.SHOOT_PHOTO
                        ) { djiError: DJIError? ->
                            if (djiError != null) {
                                ac.showSnack("拍照失败：${djiError.message}")
                            }
                        }
                    }
                    camera.setShootPhotoMode(
                        SettingsDefinitions.ShootPhotoMode.INTERVAL
                    ) {
                        ac.showSnack("setShootPhotoMode：${it.message}")
                    }
                    delay(100L)
                    camera.setPhotoTimeIntervalSettings(
                        SettingsDefinitions.PhotoTimeIntervalSettings(
                            event.takePhotoCount,
                            intervalToTake.toInt()
                        )
                    ) {
                        ac.showSnack("setPhotoTimeIntervalSettings：${it.message}")
                    }
                    delay(100L)
                    camera.setMediaFileCallback {
                        ac.showSnack("setMediaFileCallback ${it.fileName}")
                    }
                    return@launch
                }
                var times = event.takePhotoCount
                while (times > 0) {
                    takePhotoAction.invoke()
                    ac.showSnack("Take Photo action:${event.takePhotoCount - times}")
                    delay(intervalToTake.toLong())
                    --times
                }
            }
        }
    }


}