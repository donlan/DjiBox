package com.dooze.djibox.widgets

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.updatePadding
import com.dooze.djibox.R
import pdb.app.base.extensions.dpInt
import pdb.app.base.extensions.getColorCompat
import pdb.app.base.extensions.setTextEmptyGone

/**
 * @author: liangguidong
 * @date: 2022/5/26 16:18
 * @lastModifyUser: liangguidong
 * @lastModifyDate: 2022/5/26 16:18
 * @description:
 */
class AppAlert(
    private val context: Context,
    title: String? = null,
    content: CharSequence? = null,
    cancelable: Boolean = true,
    private val onClickListener: View.OnClickListener? = null
) {

    private val tvContent: TextView
    private val tvTitle: TextView
    private val actionsLayout: LinearLayout
    private var dialog: AlertDialog

    private val clickListener = View.OnClickListener {
        onClickListener?.onClick(it)
        dismiss()
    }

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.view_app_alert, null, false)
        dialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(cancelable)
            .create()
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        actionsLayout = view.findViewById(R.id.actionsLayout)
        tvTitle = view.findViewById(R.id.tvTitle)
        tvContent = view.findViewById(R.id.tvContent)
        tvTitle.setTextEmptyGone(title)
        tvContent.setTextEmptyGone(content)
    }

    private fun newActions(action: Action): TextView {
        return TextView(context).apply {
            this.id = action.id
            textSize = 14f
            text = action.text
            isAllCaps = action.isAllCaps
            typeface = if (action.isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            setTextColor(action.color ?: context.getColorCompat(R.color.colorAccent))
            val hPad = 8.dpInt(context)
            val vPad = 10.dpInt(context)
            updatePadding(hPad, vPad, hPad, vPad)
        }
    }


    fun addActions(actions: List<Action>, isHorizontal: Boolean = true): AppAlert {
        actionsLayout.updatePadding(top = 8.dpInt(context), bottom = 8.dpInt(context))
        actionsLayout.orientation =
            if (isHorizontal) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
        actions.forEach {
            val view = newActions(it)
            view.setOnClickListener(clickListener)
            actionsLayout.addView(view, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                if (isHorizontal) {
                    marginStart = 8.dpInt(context)
                } else {
                    gravity = Gravity.END
                    topMargin = 12.dpInt()
                }
            })
        }
        return this
    }

    fun addActions(vararg actions: Action, isHorizontal: Boolean = true): AppAlert {
        addActions(actions.toList(), isHorizontal)
        return this
    }


    fun show() {
        dialog.show()
    }

    fun hide() {
        dialog.hide()
    }

    fun dismiss() {
        dialog.dismiss()
    }

    data class Action(
        val id: Int,
        val text: String,
        val color: Int? = null,
        val isBold: Boolean = true,
        val isAllCaps: Boolean = false
    )

    companion object
}

fun AppAlert.Companion.text(context: Context, @StringRes stringRes: Int): AppAlert.Action {
    return AppAlert.Action(R.id.commonStart, context.getString(stringRes), isBold = false)
}

fun AppAlert.Companion.cancelAction(context: Context): AppAlert.Action {
    return AppAlert.Action(0, context.getString(R.string.common_cancel), isBold = false)
}

fun AppAlert.Companion.startAction(context: Context): AppAlert.Action {
    return AppAlert.Action(R.id.commonStart, context.getString(R.string.common_start), isBold = false)
}