package com.dooze.djibox.events

import dji.common.mission.hotpoint.HotpointMission
import dji.sdk.mission.timeline.TimelineElement

/**
 * @author: liangguidong
 * @date: 2023/2/11 14:44
 * @lastModifyUser: liangguidong
 * @lastModifyDate: 2023/2/11 14:44
 * @description:
 */
data class HotPointMissionConfigEvent(
    val mission: HotpointMission,
    val takePhotoCount: Int = 10,
    val takePhotoByApi:Boolean
)