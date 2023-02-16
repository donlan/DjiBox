package com.dooze.djibox.extensions

import android.app.Activity
import android.util.Log
import android.view.View
import androidx.annotation.StringRes
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar

/**
 * @author: liangguidong
 * @date: 2022/8/6 12:15
 * @lastModifyUser: liangguidong
 * @lastModifyDate: 2022/8/6 12:15
 * @description:
 */


fun View.showSnack(@StringRes resId: Int, duration: Int = BaseTransientBottomBar.LENGTH_SHORT) {
    Snackbar.make(this, resId, duration).show()
}

fun View.showSnack(text: CharSequence, duration: Int = BaseTransientBottomBar.LENGTH_SHORT) {
    Log.i("DjiBox", text.toString())
    Snackbar.make(this, text, duration).show()
}

fun Activity.showSnack(text: CharSequence, duration: Int = BaseTransientBottomBar.LENGTH_SHORT) {
    Log.i("DjiBox", text.toString())
    window.decorView.let { view ->
        Snackbar.make(view, text, duration).show()
    }
}
