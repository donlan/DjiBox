package com.dooze.djibox.map

import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker

/**
 * @author 梁桂栋
 * @date 2022/9/25  17:14.
 * e-mail 760625325@qq.com
 * GitHub: https://github.com/donlan
 * description: com.dooze.djibox.map
 * @version 1.0
 */
interface IPickPointMarker {

    fun onPickPoint(point: LatLng): Boolean

    fun onMarkerClick(marker: Marker): Boolean {
        return false
    }
}