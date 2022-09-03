package com.dooze.djibox.widgets

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.updatePadding
import com.dooze.djibox.R
import dji.common.mission.waypoint.Waypoint
import pdb.app.base.extensions.dpInt
import pdb.app.base.extensions.getColorCompat
import pdb.app.base.extensions.roundedCorner

/**
 * @author 梁桂栋
 * @date 2022/9/3  14:40.
 * e-mail 760625325@qq.com
 * GitHub: https://github.com/donlan
 * description: com.dooze.djibox.widgets
 * @version 1.0
 */
class MarkerIndexView @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null,
    defStyle: Int = -1
) : AppCompatTextView(context, attr, defStyle) {

    var index:Int = 0

    init {
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        setBackgroundResource(R.drawable.bg_marker)
        setTextColor(ColorStateList.valueOf(context.getColorCompat(R.color.white)))
        gravity = Gravity.CENTER
        val pad = 6.dpInt(context)
        updatePadding(pad, pad, pad, pad)
    }
}