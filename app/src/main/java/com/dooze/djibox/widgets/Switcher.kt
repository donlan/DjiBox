package com.dooze.djibox.widgets

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.dooze.djibox.R
import pdb.app.base.extensions.dpInt
import pdb.app.base.extensions.getColorCompat

class Switcher @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    style: Int = 0
) : View(context, attributeSet, style) {

    private var thumbWidth = 20.dpInt(context)
    private var thumbColor = Color.WHITE
    private var checkedTrackColor = context.getColorCompat(R.color.colorPrimary)
    private var uncheckedTrackColor = context.getColorCompat(R.color.colorPrimaryDisableBg)
    private val thumbPadding = 2.dpInt(context)

    private val colorEvaluator = ArgbEvaluator.getInstance()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var progress = 0f

    private var animator: Animator? = null

    var isChecked: Boolean = false
        set(value) {
            if (uiIsCheckedState == value) return
            progress = if (value) {
                1f
            } else {
                0f
            }
            field = value
            uiIsCheckedState = value
            invalidate()
        }
        get() = uiIsCheckedState

    private var uiIsCheckedState = isChecked

    init {
        val ta = context.obtainStyledAttributes(attributeSet, R.styleable.Switcher)
        thumbWidth = ta.getDimensionPixelSize(R.styleable.Switcher_thumbWidth, thumbWidth)
        thumbColor = ta.getColor(R.styleable.Switcher_thumbColor, thumbColor)
        checkedTrackColor = ta.getColor(R.styleable.Switcher_checkedTrackColor, checkedTrackColor)
        uncheckedTrackColor =
            ta.getColor(R.styleable.Switcher_uncheckedTrackColor, uncheckedTrackColor)

        ta.recycle()
    }

    fun setChecked(isChecked: Boolean, withAnim: Boolean = false) {
        if (this.uiIsCheckedState == isChecked) return
        if (!withAnim) {
            this.isChecked = isChecked
            return
        }
        this.uiIsCheckedState = isChecked
        if (isChecked) {
            startCheckedAnimate()
        } else {
            startUncheckAnimate()
        }
    }


    fun toggleChecked() {
        setChecked(!uiIsCheckedState, true)
    }

    private fun startCheckedAnimate() {
        animator?.cancel()
        startAnimateImpl(progress, 1f)
    }

    private fun startUncheckAnimate() {
        animator?.cancel()
        startAnimateImpl(progress, 0f)
    }

    private fun startAnimateImpl(startProgress: Float, endProgress: Float) {
        val animator = ValueAnimator.ofFloat(startProgress, endProgress)
        animator.duration = 150L
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener {
            progress = it.animatedValue as Float
            invalidate()
        }
        this.animator = animator
        animator.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        progress = if (uiIsCheckedState) 1f else 0f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = paddingStart + paddingEnd + thumbWidth * 2 + thumbPadding * 2
        val height = paddingTop + paddingBottom + thumbWidth + thumbPadding * 2
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        val bgHeight = thumbWidth + thumbPadding * 2f
        val bgRadius = bgHeight / 2f

        val trackProgress = progress

        val trackColor =
            colorEvaluator.evaluate(trackProgress, uncheckedTrackColor, checkedTrackColor) as Int

        paint.color = trackColor
        canvas.drawRoundRect(
            paddingStart.toFloat(),
            paddingTop.toFloat(),
            width - paddingEnd.toFloat(),
            paddingTop + bgHeight,
            bgRadius,
            bgRadius,
            paint
        )

        val startX = paddingStart + thumbPadding + thumbWidth / 2f
        val endCx = width - paddingEnd - thumbPadding - thumbWidth / 2f
        val cx = startX + trackProgress * (endCx - startX)
        val cy = paddingTop + bgHeight / 2
        val radius = thumbWidth / 2f
        paint.color = thumbColor
        canvas.drawCircle(cx, cy, radius, paint)
    }

}