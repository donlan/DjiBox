package com.dooze.djibox

import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.updatePadding
import com.amap.api.maps.MapView
import com.amap.api.maps.model.*
import com.dooze.djibox.extensions.mapTo
import com.dooze.djibox.internal.controller.App
import com.dooze.djibox.map.IPickPointMarker
import com.dooze.djibox.utils.toDJILocation
import com.dooze.djibox.widgets.*
import com.squareup.otto.Subscribe
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


}