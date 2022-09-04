package com.dooze.djibox.widgets

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.doOnPreDraw
import androidx.core.view.updatePadding
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.maps.AMapUtils
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.*
import com.dooze.djibox.R
import com.dooze.djibox.extensions.makeVibrate
import com.dooze.djibox.internal.controller.DJISampleApplication
import com.dooze.djibox.map.point
import com.dooze.djibox.map.zoomTo
import com.dooze.djibox.point
import dji.sdk.flightcontroller.FlightController
import dji.sdk.products.Aircraft
import pdb.app.base.extensions.dpInt
import pdb.app.base.extensions.roundedCorner
import pdb.app.base.extensions.showSnack


/**
 * @author 梁桂栋
 * @date 2022/9/3  21:00.
 * e-mail 760625325@qq.com
 * GitHub: https://github.com/donlan
 * description: com.dooze.djibox.widgets
 * @version 1.0
 */
class FlightLocationView @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null,
    defStyle: Int = -1
) : FrameLayout(context, attr, defStyle) {

    val stateView = AppCompatImageView(context)
    val mapView = MapView(context)

    var isExpended: Boolean = false
        private set

    private var mapShapeAnimator: Animator? = null
    private var flightController: FlightController? = null
    private var flightMarker: Marker? = null

    private var locationClient: AMapLocationClient? = null

    init {
        roundedCorner(8)
        stateView.setImageResource(R.drawable.ic_flight)

        val pad = 8.dpInt(context)
        stateView.updatePadding(pad, pad, pad, pad)
        stateView.setBackgroundResource(R.drawable.bg_action_view)
        stateView.roundedCorner(8)
        mapView.roundedCorner(8)
        addView(mapView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(stateView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        mapView.pivotX = 0f
        mapView.pivotY = 0f
        stateView.setOnClickListener {
            isExpended = !isExpended
            mapShapeAnim(isExpended)
            if (isExpended) {
                initFlightController()
            }
        }
        doOnPreDraw {
            mapShapeAnim(isExpended)
        }

        mapView.map.setOnMarkerClickListener {
            val distance = AMapUtils.calculateLineDistance(
                it.position,
                LatLng(mapView.map.myLocation.latitude, mapView.map.myLocation.longitude)
            )
            showSnack(context.getString(R.string.flight_distance_to_me, distance.toString()))
            true
        }

    }


    private fun initFlightController() {
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
                val point = LatLng(droneLocationLat, droneLocationLng)
                post {
                    mapView.map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            point,
                            17f
                        )
                    )

                    (flightMarker ?: mapView.map.addMarker(MarkerOptions().apply {
                        position(point)
                        val drawable =
                            ResourcesCompat.getDrawable(resources, R.drawable.ic_flight, null)!!
                        drawable.setTint(ContextCompat.getColor(context, dji.ux.R.color.debug_1))
                        this.icon(BitmapDescriptorFactory.fromBitmap(drawable.toBitmap()))
                    })).position = point
                }
            }
        } else {
            showSnack(context.getString(R.string.flight_not_connected))
        }
    }

    private fun mapShapeAnim(isExpend: Boolean) {
        mapShapeAnimator?.cancel()
        val targetScaleX =
            if (isExpended) 1f else stateView.measuredWidth / mapView.measuredWidth.toFloat() - 0.1f
        val targetScaleY =
            if (isExpended) 1f else stateView.measuredHeight / mapView.measuredHeight.toFloat() - 0.1f
        val anim = ObjectAnimator.ofPropertyValuesHolder(
            mapView,
            PropertyValuesHolder.ofFloat(View.SCALE_X, mapView.scaleX, targetScaleX),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, mapView.scaleY, targetScaleY)
        )
        anim.duration = 250L
        anim.interpolator = AccelerateDecelerateInterpolator()
        anim.start()
        mapShapeAnimator = anim
    }

    fun onCreate(state: Bundle?) {
        AMapLocationClient.updatePrivacyShow(context, true, true)
        AMapLocationClient.updatePrivacyAgree(context, true)
        mapView.onCreate(state)
        mapView.map.uiSettings.apply {
            isZoomControlsEnabled = false
        }
        if (locationClient != null) return
        val locationClient = AMapLocationClient(context)
        this.locationClient = locationClient
        locationClient.setLocationOption(AMapLocationClientOption().apply {
            locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
        })
        mapView.map.isMyLocationEnabled = true
        mapView.map.myLocationStyle = MyLocationStyle().apply {
            myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_my_location_circle))
            myLocationType(MyLocationStyle.LOCATION_TYPE_SHOW)
        }
        var firstLocation = true
        locationClient.setLocationListener {
            mapView.map.myLocation.set(it)
            if (firstLocation) {
                mapView.zoomTo(it.point)
            }
            firstLocation = false
        }
        mapView.map.setOnMapLongClickListener {
            mapView.zoomTo(mapView.map.myLocation?.point ?: return@setOnMapLongClickListener)
            makeVibrate()
        }
    }

    fun onSaveInstance(state: Bundle?) {
        mapView.onCreate(state)
    }

    fun onResume() {
        mapView.onResume()
        locationClient?.startLocation()
    }

    fun onPause() {
        mapView.onPause()
        locationClient?.stopLocation()
    }

    fun onDestroy() {
        mapView.map.setOnMarkerClickListener(null)
        locationClient?.onDestroy()
        locationClient = null
    }

}