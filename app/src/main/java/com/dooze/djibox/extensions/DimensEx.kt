package pdb.app.base.extensions

import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Display
import android.view.WindowManager
import kotlin.math.roundToInt

private var applicationContext: Context? = null

fun setupAppContext(appContext: Context) {
    applicationContext = appContext.applicationContext
}

fun Int.dp(context: Context? = applicationContext): Float {
    val c = context ?: requireNotNull(applicationContext)
    return c.resources.displayMetrics.density * this
}

fun Int.dpInt(context: Context? = applicationContext): Int {
    val c = context ?: requireNotNull(applicationContext)
    return (c.resources.displayMetrics.density * this).roundToInt()
}

fun Number.dp(context: Context? = applicationContext): Float {
    val c = context ?: requireNotNull(applicationContext)
    return c.resources.displayMetrics.density * this.toFloat()
}

fun Number.dpInt(context: Context? = applicationContext): Int {
    val c = context ?: requireNotNull(applicationContext)
    return (c.resources.displayMetrics.density * this.toFloat()).roundToInt()
}

fun Number.toPercent(): String {
    val x = (this.toDouble() * 100).toInt().coerceAtMost(100)
    return "$x%"
}

fun Number.spInt(context: Context? = applicationContext): Int {
    val c = context ?: requireNotNull(applicationContext)
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        this.toFloat(),
        c.resources.displayMetrics
    ).toInt()
}

fun Number.sp(context: Context? = applicationContext): Float {
    val c = context ?: requireNotNull(applicationContext)
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        this.toFloat(),
        c.resources.displayMetrics
    )
}

private var sPortraitRealSizeCache: IntArray? = null
private var sLandscapeRealSizeCache: IntArray? = null

/**
 *  获取屏幕的真实宽高
 */
fun getRealScreenSize(context: Context): IntArray {
//    if (DeviceUtil.isEssentialPhone && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//        // Essential Phone 8.0版本后，Display size 会根据挖孔屏的设置而得到不同的结果，不能信任 cache
//        return doGetRealScreenSize(context)
//    }
    val orientation = context.resources.configuration.orientation
    var result: IntArray?
    // 根据 横竖屏不同状态，获取不同的屏幕宽高值，并保存缓存
    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
        result = sLandscapeRealSizeCache
        if (result == null) {
            result = doGetRealScreenSize(context)
            if (result[0] > result[1]) {
                // the result may be wrong sometimes, do not cache !!!!
                sLandscapeRealSizeCache = result
            }
        }
        return result
    } else {
        result = sPortraitRealSizeCache
        if (result == null) {
            result = doGetRealScreenSize(context)
            if (result[0] < result[1]) {
                // the result may be wrong sometimes, do not cache !!!!
                sPortraitRealSizeCache = result
            }
        }
        return result
    }
}

private fun doGetRealScreenSize(context: Context): IntArray {
    val size = IntArray(2)
    var widthPixels: Int
    var heightPixels: Int
    val w = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val d = w.defaultDisplay
    val metrics = DisplayMetrics()
    d.getMetrics(metrics)
    // since SDK_INT = 1;
    widthPixels = metrics.widthPixels
    heightPixels = metrics.heightPixels
    try {
        // used when SDK_INT >= 17; includes window decorations (statusbar bar/menu bar)
        val realSize = Point()
        d.getRealSize(realSize)
        Display::class.java.getMethod("getRealSize", Point::class.java).invoke(d, realSize)
        widthPixels = realSize.x
        heightPixels = realSize.y
    } catch (ignored: Exception) {
    }
    size[0] = widthPixels
    size[1] = heightPixels
    return size
}