package com.dooze.djibox.extensions

import java.util.*

fun <T> lazyFast(initializer: () -> T): Lazy<T> =
    lazy(LazyThreadSafetyMode.NONE, initializer)


fun String?.ifNullOfEmpty(default: () -> String): String {
    return if (this.isNullOrEmpty()) default.invoke() else this
}


fun <T, R> T.mapTo(action: (T.() -> R)): R {
    return action.invoke(this)
}

fun uuid() = UUID.randomUUID().toString()