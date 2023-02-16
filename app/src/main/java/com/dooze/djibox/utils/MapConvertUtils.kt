package com.dooze.djibox.utils

import android.content.Context
import com.amap.api.location.CoordinateConverter
import com.amap.api.location.DPoint
import com.amap.api.maps.model.LatLng
import com.dooze.djibox.utils.MapConvertUtils
import dji.common.model.LocationCoordinate2D

class MapConvertUtils {
    // 坐标转换-转为高德坐标系(高德API)
    fun getGDLatLng(context: Context, lat: Double, lng: Double): LatLng {
        val converter = CoordinateConverter(context)
        // CoordType.GPS 待转换坐标类型
        converter.from(CoordinateConverter.CoordType.GPS)
        // sourceLatLng待转换坐标点 DPoint类型
        converter.coord(DPoint(lat, lng))
        // 执行转换操作
        val point = converter.convert()
        return LatLng(point.latitude, point.longitude)
    }

    companion object {
        private const val PI = Math.PI // 圆周率
        private const val AXIS = 6378245.0 //轴
        private const val OFFSET = 0.00669342162296594323 //偏移量 (a^2 - b^2) / a^2


        fun isValidPoint(lat: Double, lng: Double): Boolean {
            return !(lat < -90.0 || lat > 90.0 || lng < -90.0 || lng > 90.0)
        }

        // WGS84=》GCJ02   地球坐标系=>火星坐标系
        fun wgs2GCJ(wgLat: Double, wgLon: Double): DoubleArray {
            val latlon = DoubleArray(2)
            //        if (outOfChina(wgLat, wgLon)) {
//            latlon[0] = wgLat;
//            latlon[1] = wgLon;
//            return latlon;
//        }
            val deltaD = delta(wgLat, wgLon)
            latlon[0] = wgLat + deltaD[0]
            latlon[1] = wgLon + deltaD[1]
            return latlon
        }

        // WGS84=》GCJ02   地球坐标系=>火星坐标系
        fun WGS2GCJ(wgLat: Double, wgLon: Double): LatLng {
            val latlon = DoubleArray(2)
            //        if (outOfChina(wgLat, wgLon)) {
//            latlon[0] = wgLat;
//            latlon[1] = wgLon;
//            return latlon;
//        }
            val deltaD = delta(wgLat, wgLon)
            latlon[0] = wgLat + deltaD[0]
            latlon[1] = wgLon + deltaD[1]
            return LatLng(latlon[0], latlon[1])
        }

        // 坐标转换-转为大疆坐标系
        //GCJ02=>WGS84   火星坐标系=>地球坐标系（精确）
        fun getDJILatLng(gcjLat: Double, gcjLon: Double): LatLng {
            val initDelta = 0.01
            val threshold = 0.000000001
            var dLat = initDelta
            var dLon = initDelta
            var mLat = gcjLat - dLat
            var mLon = gcjLon - dLon
            var pLat = gcjLat + dLat
            var pLon = gcjLon + dLon
            var wgsLat: Double
            var wgsLon: Double
            var i = 0.0
            while (true) {
                wgsLat = (mLat + pLat) / 2
                wgsLon = (mLon + pLon) / 2
                val tmp = wgs2GCJ(wgsLat, wgsLon)
                dLat = tmp[0] - gcjLat
                dLon = tmp[1] - gcjLon
                if (Math.abs(dLat) < threshold && Math.abs(dLon) < threshold) break
                if (dLat > 0) pLat = wgsLat else mLat = wgsLat
                if (dLon > 0) pLon = wgsLon else mLon = wgsLon
                if (++i > 10000) break
            }
            return LatLng(wgsLat, wgsLon)
        }

        // 转换函数
        private fun delta(wgLat: Double, wgLon: Double): DoubleArray {
            val latlng = DoubleArray(2)
            var dLat = transformLat(wgLon - 105.0, wgLat - 35.0)
            var dLon = transformLon(wgLon - 105.0, wgLat - 35.0)
            val radLat = wgLat / 180.0 * PI
            var magic = Math.sin(radLat)
            magic = 1 - OFFSET * magic * magic
            val sqrtMagic = Math.sqrt(magic)
            dLat = dLat * 180.0 / (AXIS * (1 - OFFSET) / (magic * sqrtMagic) * PI)
            dLon = dLon * 180.0 / (AXIS / sqrtMagic * Math.cos(radLat) * PI)
            latlng[0] = dLat
            latlng[1] = dLon
            return latlng
        }

        // 是否超出国界
        private fun outOfChina(lat: Double, lon: Double): Boolean {
            if (lon < 72.004 || lon > 137.8347) return true
            return if (lat < 0.8293 || lat > 55.8271) true else false
        }

        // 转换纬度
        private fun transformLat(x: Double, y: Double): Double {
            var ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(
                Math.abs(x)
            )
            ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0
            ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0
            ret += (160.0 * Math.sin(y / 12.0 * PI) + 320 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0
            return ret
        }

        // 转换经度
        private fun transformLon(x: Double, y: Double): Double {
            var ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x))
            ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0
            ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0
            ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0
            return ret
        }
    }
}

fun LatLng.toDJILocation(): LocationCoordinate2D {
    val point = MapConvertUtils.getDJILatLng(latitude, longitude)
    return LocationCoordinate2D(point.latitude, point.longitude)
}