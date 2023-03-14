package com.dooze.djibox.events

import com.dooze.djibox.extensions.uuid

/**
 * @author: liangguidong
 * @date: 2023/3/14 23:41
 * @lastModifyUser: liangguidong
 * @lastModifyDate: 2023/3/14 23:41
 * @description:
 */
data class TakePhotoEvent(val id: String = uuid())