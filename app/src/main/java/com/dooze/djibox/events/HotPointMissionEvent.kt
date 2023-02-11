package com.dooze.djibox.events

import dji.sdk.mission.timeline.TimelineElement

/**
 * @author: liangguidong
 * @date: 2023/2/11 14:44
 * @lastModifyUser: liangguidong
 * @lastModifyDate: 2023/2/11 14:44
 * @description:
 */
data class HotPointMissionEvent(val elements: ArrayList<TimelineElement>)