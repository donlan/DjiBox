package com.dooze.djibox.map

import android.location.Location
import com.amap.api.location.AMapLocation
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng

/**
 * @author 梁桂栋
 * @date 2022/9/4  15:25.
 * e-mail 760625325@qq.com
 * GitHub: https://github.com/donlan
 * description: com.dooze.djibox.map
 * @version 1.0
 */

fun MapView.zoomTo(point:LatLng, zoom:Float = 17f) {
    map.moveCamera(
        CameraUpdateFactory.newLatLngZoom(point, zoom)
    )
}

val Location.point: LatLng
    get() = LatLng(this.latitude, this.longitude)