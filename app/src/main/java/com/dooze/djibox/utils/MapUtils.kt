package com.dooze.djibox.utils

import com.amap.api.maps.AMapUtils
import com.amap.api.maps.model.LatLng

/**
 * @author 梁桂栋
 * @date 2022/10/15  18:00.
 * e-mail 760625325@qq.com
 * GitHub: https://github.com/donlan
 * description: com.dooze.djibox.utils
 * @version 1.0
 */
object MapUtils {

    fun verticalDistance(p: LatLng, p1: LatLng): Float {
        return AMapUtils.calculateLineDistance(LatLng(p.latitude, p1.longitude), p1)
    }

    fun horizontalDistance(p: LatLng, p1: LatLng): Float {
        return AMapUtils.calculateLineDistance(LatLng(p1.latitude, p.longitude), p1)
    }

    fun center(p: LatLng, p1: LatLng): LatLng {
        return LatLng((p.latitude + p1.latitude) / 2, (p.longitude + p1.longitude) / 2)
    }
}