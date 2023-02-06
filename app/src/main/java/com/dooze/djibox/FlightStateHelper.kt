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
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.dooze.djibox.extensions.mapTo
import com.dooze.djibox.internal.controller.DJISampleApplication
import com.dooze.djibox.map.point
import com.dooze.djibox.utils.MapConvertUtils
import dji.common.flightcontroller.RTKState
import dji.sdk.flightcontroller.FlightController
import dji.sdk.products.Aircraft
import pdb.app.base.extensions.showSnack
import java.text.DecimalFormat

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

    private var distanceView: TextView? = null

    private var flightMarker: Marker? = null

    var rtkState: RTKState? = null
        set(value) {
            field = value
            value?.run {
                updateUIState(
                    fusionMobileStationLocation.latitude,
                    fusionMobileStationLocation.longitude,
                    homePointLocation?.mapTo {
                        MapConvertUtils.WGS2GCJ(this.latitude, this.longitude)
                    },
                    heading
                )
            }
        }


    fun init(mapView: MapView, distanceView: TextView? = null) {
        this.mapView = mapView
        this.distanceView = distanceView
        initFlightController()
    }


    private fun updateUIState(
        aircraftLat: Double, aircraftLng: Double,
        selfPoint: LatLng?,
        direction: Float
    ) {
        val mapView = mapView ?: return
        val context = mapView.context
        val point = MapConvertUtils.WGS2GCJ(aircraftLat, aircraftLng)
        mapView.post {
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
            flightMarker = marker
            marker.position = point
            marker.rotateAngle = direction

            selfPoint?.let {
                val distance = AMapUtils.calculateLineDistance(
                    selfPoint,
                    point
                )
                distanceView?.text = buildString {
                    append(distance.toInt())
                    append("米")
                }
            }
        }
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

                if (rtkState != null) return@setStateCallback
                val myLocation = mapView.map.myLocation ?: return@setStateCallback

                val droneLocationLat = djiFlightControllerCurrentState.aircraftLocation.latitude
                val droneLocationLng = djiFlightControllerCurrentState.aircraftLocation.longitude
                val direction = djiFlightControllerCurrentState.aircraftHeadDirection
                updateUIState(
                    droneLocationLat, droneLocationLng,
                    myLocation.point, direction.toFloat()
                )
            }
        } else {
            mapView.showSnack(context.getString(R.string.flight_not_connected))
        }
    }
}

object ResetHotPoint