package com.dooze.djibox.extensions

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * @author: liangguidong
 * @date: 2022/8/6 22:26
 * @lastModifyUser: liangguidong
 * @lastModifyDate: 2022/8/6 22:26
 * @description:
 */

private var vibrator: Vibrator? = null
fun init(context: Context) {
    if (vibrator == null) {
        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
}

fun makeVibrate() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(VibrationEffect.createOneShot(20, 1))
    } else {
        vibrator?.vibrate(20L)
    }
}