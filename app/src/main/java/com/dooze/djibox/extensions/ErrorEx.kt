package com.dooze.djibox.extensions

import dji.common.error.DJIError

/**
 * @author: liangguidong
 * @date: 2023/2/21 20:59
 * @lastModifyUser: liangguidong
 * @lastModifyDate: 2023/2/21 20:59
 * @description:
 */

val DJIError.message: String
    get() = "$errorCode:$description"