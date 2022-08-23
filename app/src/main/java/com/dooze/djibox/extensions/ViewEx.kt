package pdb.app.base.extensions

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.graphics.*
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.FloatRange
import androidx.annotation.IdRes
import androidx.annotation.IntRange
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.applyCanvas
import androidx.core.view.ViewCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.dooze.djibox.extensions.lazyFast
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min




fun View.roundedCorner(radiusDP: Int = -1) {
    outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View?, outline: Outline?) {
            outline ?: return
            view ?: return
            val r = if (radiusDP < 0) view.height / 2f else radiusDP.dp(context)
            outline.setRoundRect(0, 0, view.width, view.height, r)
        }
    }
    clipToOutline = true
}

fun View.topRoundedCorner(radius: Float = -1f) {
    outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View?, outline: Outline?) {
            outline ?: return
            view ?: return
            val r = if (radius < 0) view.height / 2f else radius
            outline.setRoundRect(0, 0, view.width, view.height + r.toInt(), r)
        }
    }
    clipToOutline = true
}

fun View.showSnack(@StringRes resId: Int, duration: Int = BaseTransientBottomBar.LENGTH_SHORT) {
    Snackbar.make(this, resId, duration).show()
}

fun View.showSnack(text: CharSequence, duration: Int = BaseTransientBottomBar.LENGTH_SHORT) {
    Snackbar.make(this, text, duration).show()
}


fun Context.getColorCompat(@ColorRes id: Int) = ContextCompat.getColor(this, id)

fun <T : View> Activity.bindView(@IdRes idRes: Int): Lazy<T> {
    return lazyFast { findViewById<T>(idRes) }
}

fun <T : View> View.bindView(@IdRes idRes: Int): Lazy<T> {
    return lazyFast { findViewById<T>(idRes) }
}

fun <T : View> Fragment.bindView(@IdRes idRes: Int): Lazy<T> {
    return lazyFast { requireView().findViewById<T>(idRes) }
}



fun EditText.endSelection() {
    setSelection(length())
}

fun EditText.endSelection(text: CharSequence?) {
    setText(text)
    setSelection(length())
}

fun BadgeDrawable.update(number: Int) {
    this.number = number
    isVisible = number > 0
}

fun ViewGroup.safeGetChildAt(index: Int): View? {
    if (index in 0 until childCount) {
        return getChildAt(index)
    }
    return null
}


fun TextView.setTextEmptyGone(text: CharSequence?) {
    this.text = text
    isVisible = !text.isNullOrEmpty()
}

fun View.removeFromParent() {
    (parent as? ViewGroup)?.removeView(this)
}

var View.isEnable: Boolean
    get() {
        return isEnabled
    }
    set(value) {
        alpha = if (value) 1f else 0.3f
        isEnabled = value
    }


fun View.toBitmap(config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap {
    if (!ViewCompat.isLaidOut(this)) {
        throw IllegalStateException("View needs to be laid out before calling drawToBitmap()")
    }
    return Bitmap.createBitmap(width, height, config).applyCanvas {
        translate(-scrollX.toFloat(), -scrollY.toFloat())
        draw(this)
    }
}

fun runAfterLayout(child: View, runnable: Runnable) {
    if (isLayouting(child)) {
        child.post(runnable)
    } else {
        runnable.run()
    }
}

private fun isLayouting(child: View): Boolean {
    val parent = child.parent
    return parent != null && parent.isLayoutRequested && ViewCompat.isAttachedToWindow(child)
}

fun ImageView.blurCompat(
    bitmap: Bitmap,
    @FloatRange(from = 2.0, to = 30.0) level: Float = 10.0f,
    blurBmp: (bitmap: Bitmap) -> Bitmap
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        setImageBitmap(blurBmp.invoke(bitmap))
        return
    }
    if (isLaidOut) {
        val r = min(measuredWidth, measuredHeight) / level
        setRenderEffect(RenderEffect.createBlurEffect(r, r, Shader.TileMode.MIRROR))
        setImageBitmap(bitmap)
    } else {
        doOnPreDraw {
            val r = min(measuredWidth, measuredHeight) / level
            setRenderEffect(RenderEffect.createBlurEffect(r, r, Shader.TileMode.MIRROR))
            setImageBitmap(bitmap)
        }
    }
}


fun View.blur(@FloatRange(from = 2.0, to = 30.0) level: Float = 10.0f) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    if (isLaidOut) {
        val r = min(measuredWidth, measuredHeight) / level
        setRenderEffect(RenderEffect.createBlurEffect(r, r, Shader.TileMode.MIRROR))
    } else {
        doOnPreDraw {
            val r = min(measuredWidth, measuredHeight) / level
            setRenderEffect(RenderEffect.createBlurEffect(r, r, Shader.TileMode.MIRROR))
        }
    }
}

fun View.unBlur() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    doOnPreDraw {
        setRenderEffect(null)
    }
}

fun View.fadeIn(): ObjectAnimator? {
    val animator = ObjectAnimator.ofFloat(this, View.ALPHA, 0f, 1f)
    animator.duration = 250L
    animator.start()
    return animator
}
