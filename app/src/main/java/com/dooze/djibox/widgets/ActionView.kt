package com.dooze.djibox.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.updatePadding
import com.dooze.djibox.R
import pdb.app.base.extensions.*

/**
 * @author 梁桂栋
 * @date 2022/9/25  11:02.
 * e-mail 760625325@qq.com
 * GitHub: https://github.com/donlan
 * description: com.dooze.djibox.widgets
 * @version 1.0
 */

class ActionView @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null,
    defStyle: Int = com.google.android.material.R.attr.seekBarStyle
) : RelativeLayout(context, attr, defStyle) {

    private val ok = TextView(context).apply {
        updateAllPadding(12.dpInt(context))
        id = R.id.commonOk
        textSize = 16f
        setText(R.string.common_done)
        setTextColor(context.getColorCompat(R.color.colorAccent))
    }

    private val close = TextView(context).apply {
        updateAllPadding(12.dpInt(context))
        id = R.id.commonClose
        textSize = 16f
        setText(R.string.common_cancel)
        setTextColor(context.getColorCompat(R.color.alert))
    }

    var onOkClick: (() -> Unit)? = null

    var onCloseClick: (() -> Unit)? = null

    init {
        roundedCorner(12)
        setBackgroundResource(R.color.white)
        elevation = 10.dp(context)
        translationZ = 30.dp(context)

        addView(ok, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            addRule(CENTER_VERTICAL)
            addRule(ALIGN_PARENT_END)
        })

        ok.setOnClickListener {
            onOkClick?.invoke()
        }
        close.setOnClickListener {
            onCloseClick?.invoke()
        }
    }

    fun setupCancel() {
        addView(
            close,
            0,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                addRule(START_OF, R.id.commonOk)
                addRule(CENTER_VERTICAL)
            })
    }

    fun wrap(content: View) {
        addView(
            content,
            0,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                minimumHeight = 40.dpInt(context)
                addRule(START_OF, close.takeIf { it.parent != null }?.id ?: R.id.commonOk)
            })
    }

    fun attachBottom(parent: RelativeLayout) {
        parent.addView(
            this,
            RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(ALIGN_PARENT_BOTTOM)
                addRule(CENTER_HORIZONTAL)
                marginStart = 80.dpInt(context)
                marginEnd = marginStart
                bottomMargin = 20.dpInt(context)
            }
        )
    }
}


fun SeekBarAction(
    context: Context,
    min: Int,
    max: Int,
    title:String,
    step: Float = 1f,
    onProgressChanged: ((progress: Float) -> Unit)? = null,
    onOkClick: (() -> Unit)? = null
): ActionView {
    return ActionView(context).apply {
        wrap(SeekbarText(context).apply {
            this.progress
            setTitle(title)
            updatePadding(16.dpInt(context), 16.dpInt(context))
            progress = 50f
            this.onProgressChanged = { radius ->
                setValuesText(radius.toString())
                onProgressChanged?.invoke(radius)
            }
            setup(min, max, step)
        })
        this.onOkClick = {
            removeFromParent()
            onOkClick?.invoke()
        }
    }
}