package com.dooze.djibox

import android.graphics.Color
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import com.amap.api.maps.AMapUtils
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.dooze.djibox.internal.controller.DJISampleApplication
import com.dooze.djibox.map.point
import com.dooze.djibox.utils.MapConvertUtils
import dji.sdk.flightcontroller.FlightController
import dji.sdk.products.Aircraft
import pdb.app.base.extensions.showSnack

/**
 * @author 梁桂栋
 * @date 2022/9/25  9:53.
 * e-mail 760625325@qq.com
 * GitHub: https://github.com/donlan
 * description: com.dooze.djibox
 * @version 1.0
 */
class FlightStateHelper {

    private var flightController: FlightController? = null

    private var mapView: MapView? = null

    private var flightMarker: Marker? = null

    private var debugInfoView: TextView? = null


    fun init(mapView: MapView) {
        this.mapView = mapView
        initFlightController()
    }

    private fun requireDebugInfoView(): TextView? {
        val context = mapView?.context ?: return null
        val parent = mapView?.parent as? RelativeLayout ?: return null
        if (debugInfoView == null) {
            debugInfoView = TextView(context).apply {
                setTextColor(Color.WHITE)
                setBackgroundColor(
                    ColorUtils.setAlphaComponent(
                        Color.BLACK,
                        (0.3 * 255).toInt()
                    )
                )
                parent.addView(
                    this,
                    RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        addRule(RelativeLayout.CENTER_IN_PARENT)
                    })
            }
            debugInfoView!!.setOnLongClickListener {
                it.isVisible = false
                true
            }
        }

        return debugInfoView
    }


    private fun initFlightController() {
        val mapView = mapView ?: return
        val context = mapView.context
        val product = DJISampleApplication.getProductInstance()
        if (product != null && product.isConnected) {
            if (product is Aircraft) {
                val controller = product.flightController
                if (controller == flightController) {
                    return
                }
                flightController = controller
            }
        }
        if (flightController != null) {
            flightController?.setStateCallback { djiFlightControllerCurrentState ->
                val droneLocationLat = djiFlightControllerCurrentState.aircraftLocation.latitude
                val droneLocationLng = djiFlightControllerCurrentState.aircraftLocation.longitude
                val direction = djiFlightControllerCurrentState.aircraftHeadDirection
                val point = MapConvertUtils.WGS2GCJ(droneLocationLat, droneLocationLng)
                mapView.post {
                    mapView.map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            point,
                            17f
                        )
                    )

                    val marker = (flightMarker ?: mapView.map.addMarker(MarkerOptions().apply {
                        position(point)

                        val drawable = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.ic_flight,
                            null
                        )!!.mutate()
                        drawable.setTint(ContextCompat.getColor(context, dji.ux.R.color.debug_1))
                        this.icon(BitmapDescriptorFactory.fromBitmap(drawable.toBitmap()))
                    }))
                    marker.position = point
                    marker.rotateAngle = direction.toFloat()

                    if (BuildConfig.DEBUG && requireDebugInfoView()?.isVisible == true) {
                        requireDebugInfoView()?.text = buildString {
                            append("Lat:")
                            append(droneLocationLat)
                            append("\n")
                            append("Lng", droneLocationLng)
                            append("\n")
                            append("Time:${djiFlightControllerCurrentState.flightTimeInSeconds}s\n")
                            mapView.map.myLocation?.let {
                                append("MyLocation:\n")
                                append("Lat:${it.latitude}\n")
                                append("Lng${it.longitude}\n")
                                append(
                                    "Distance:${
                                        AMapUtils.calculateLineDistance(
                                            it.point,
                                            point
                                        )
                                    }"
                                )
                            }
                        }
                    }
                }
            }
        } else {
            mapView.showSnack(context.getString(R.string.flight_not_connected))
        }
    }
}

object ResetHotPoint