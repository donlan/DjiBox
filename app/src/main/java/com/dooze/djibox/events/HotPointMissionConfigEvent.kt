package com.dooze.djibox.events

import com.dooze.djibox.extensions.uuid
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
    val takePhotoByApi: Boolean,
    val takeOffFirst: Boolean,
    val id: String = uuid()
)

data class GroundHotPointMissionConfigEvent(
    val missions: List<HotPointMissionConfigEvent>,
    val takePhotoCount: Int = 10,
    val takePhotoByApi: Boolean,
    val takeOffFirst: Boolean,
    val id: String = uuid()
)

data class StopHotPointEvent(val id: String = uuid())