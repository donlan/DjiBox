package com.dooze.djibox.internal.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;

import com.dooze.djibox.R;
import com.dooze.djibox.fun.accessory.AccessoryAggregationView;
import com.dooze.djibox.fun.accessory.AudioFileListManagerView;
import com.dooze.djibox.fun.airlink.RebootWiFiAirlinkView;
import com.dooze.djibox.fun.airlink.SetGetOcuSyncLinkView;
import com.dooze.djibox.fun.airlink.SetGetWiFiLinkSSIDView;
import com.dooze.djibox.fun.appactivation.AppActivationView;
import com.dooze.djibox.fun.battery.PushBatteryDataView;
import com.dooze.djibox.fun.battery.SetGetDischargeDayView;
import com.dooze.djibox.fun.camera.CameraCalibration;
import com.dooze.djibox.fun.camera.FetchMediaView;
import com.dooze.djibox.fun.camera.LiveStreamView;
import com.dooze.djibox.fun.camera.MediaPlaybackView;
import com.dooze.djibox.fun.camera.MultipleLensCameraView;
import com.dooze.djibox.fun.camera.PlaybackCommandsView;
import com.dooze.djibox.fun.camera.PlaybackDownloadView;
import com.dooze.djibox.fun.camera.PlaybackPushInfoView;
import com.dooze.djibox.fun.camera.PushCameraDataView;
import com.dooze.djibox.fun.camera.RecordVideoView;
import com.dooze.djibox.fun.camera.SetGetISOView;
import com.dooze.djibox.fun.camera.ShootSinglePhotoView;
import com.dooze.djibox.fun.camera.ShootSuperResolutionPhotoView;
import com.dooze.djibox.fun.camera.VideoFeederView;
import com.dooze.djibox.fun.camera.XT2CameraView;
import com.dooze.djibox.fun.datalocker.AccessLockerView;
import com.dooze.djibox.fun.firmwareupgrade.FirmwareUpgradeView;
import com.dooze.djibox.fun.flightcontroller.CompassCalibrationView;
import com.dooze.djibox.fun.flightcontroller.FlightAssistantPushDataView;
import com.dooze.djibox.fun.flightcontroller.FlightHubView;
import com.dooze.djibox.fun.flightcontroller.FlightLimitationView;
import com.dooze.djibox.fun.flightcontroller.NetworkRTKView;
import com.dooze.djibox.fun.flightcontroller.OrientationModeView;
import com.dooze.djibox.fun.flightcontroller.VirtualStickView;
import com.dooze.djibox.fun.gimbal.GimbalCapabilityView;
import com.dooze.djibox.fun.gimbal.MoveGimbalWithSpeedView;
import com.dooze.djibox.fun.gimbal.PushGimbalDataView;
import com.dooze.djibox.fun.key.KeyedInterfaceView;
import com.dooze.djibox.fun.keymanager.KeyManagerView;
import com.dooze.djibox.fun.lidar.LidarView;
import com.dooze.djibox.fun.lookat.LookAtMissionView;
import com.dooze.djibox.fun.missionoperator.FollowMeMissionOperatorView;
import com.dooze.djibox.fun.missionoperator.WaypointMissionOperatorView;
import com.dooze.djibox.fun.missionoperator.WaypointV2MissionOperatorView;
import com.dooze.djibox.fun.mobileremotecontroller.MobileRemoteControllerView;
import com.dooze.djibox.fun.radar.RadarView;
import com.dooze.djibox.fun.remotecontroller.PushRemoteControllerDataView;
import com.dooze.djibox.fun.timeline.TimelineMissionControlView;
import com.dooze.djibox.fun.useraccount.LDMView;
import com.dooze.djibox.internal.controller.DJISampleApplication;
import com.dooze.djibox.internal.controller.ExpandableListAdapter;
import com.dooze.djibox.internal.controller.MainActivity;
import com.dooze.djibox.internal.model.GroupHeader;
import com.dooze.djibox.internal.model.GroupItem;
import com.dooze.djibox.internal.model.ListItem;
import com.squareup.otto.Subscribe;

/**
 * This view is in charge of showing all the demos in a list.
 */

public class DemoListView extends FrameLayout {

    private ExpandableListAdapter listAdapter;
    private ExpandableListView expandableListView;

    public DemoListView(Context context) {
        this(context, null, 0);
    }

    public DemoListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DemoListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.demo_list_view, this);

        // Build model for ListView
        ListItem.ListBuilder builder = new ListItem.ListBuilder();
        builder.addGroup(R.string.component_listview_sdk_4_16,
                false,
                new GroupItem(R.string.look_at_mission, LookAtMissionView.class));
        builder.addGroup(R.string.component_listview_sdk_4_15,
                false,
                new GroupItem(R.string.component_listview_lidar_view, LidarView.class));
        builder.addGroup(R.string.component_listview_sdk_4_14,
                false,
                new GroupItem(R.string.component_listview_radar, RadarView.class),
                new GroupItem(R.string.component_listview_ldm, LDMView.class));
        builder.addGroup(R.string.component_listview_sdk_4_12,
                false,
                new GroupItem(R.string.component_listview_multiple_lens_camera, MultipleLensCameraView.class),
                new GroupItem(R.string.component_listview_health_information, HealthInformationView.class),
                new GroupItem(R.string.component_listview_waypointv2, WaypointV2MissionOperatorView.class),
                new GroupItem(R.string.component_listview_utmiss, StartUTMISSActivityView.class));
        builder.addGroup(R.string.component_listview_sdk_4_11,
                false,
                new GroupItem(R.string.component_listview_firmware_upgrade, FirmwareUpgradeView.class));
        builder.addGroup(R.string.component_listview_sdk_4_9,
                false,
                new GroupItem(R.string.component_listview_live_stream, LiveStreamView.class));
        builder.addGroup(R.string.component_listview_sdk_4_8,
                false,
                new GroupItem(R.string.component_listview_access_locker, AccessLockerView.class),
                new GroupItem(R.string.component_listview_accessory_aggregation,
                        AccessoryAggregationView.class),
                new GroupItem(R.string.component_listview_audio_file_list_manager,
                        AudioFileListManagerView.class));
        builder.addGroup(R.string.component_listview_sdk_4_6,
                false,
                new GroupItem(R.string.component_listview_payload,
                        StartPayloadAcitivityView.class),
                new GroupItem(R.string.component_listview_redirect_to_djigo,
                        StartRedirectGoAcitivityView.class));
        builder.addGroup(R.string.component_listview_sdk_4_5,
                false,
                new GroupItem(R.string.component_listview_flight_hub,
                        FlightHubView.class));

        builder.addGroup(R.string.component_listview_sdk_4_1,
                false,
                new GroupItem(R.string.component_listview_app_activation,
                        AppActivationView.class));


        builder.addGroup(R.string.component_listview_sdk_4_0,
                false,
                new GroupItem(R.string.component_listview_waypoint_mission_operator,
                        WaypointMissionOperatorView.class),
                new GroupItem(R.string.component_listview_follwome_mission_operator,
                        FollowMeMissionOperatorView.class),
                new GroupItem(R.string.component_listview_keyed_interface, KeyedInterfaceView.class),
                new GroupItem(R.string.component_listview_timeline_mission_control,
                        TimelineMissionControlView.class));

        builder.addGroup(R.string.component_listview_key_manager, false,
                new GroupItem(R.string.key_manager_listview_key_Interface, KeyManagerView.class));

        builder.addGroup(R.string.component_listview_camera,
                false,
                new GroupItem(R.string.camera_listview_push_info, PushCameraDataView.class),
                new GroupItem(R.string.camera_listview_iso, SetGetISOView.class),
                new GroupItem(R.string.camera_listview_shoot_single_photo, ShootSinglePhotoView.class),
                new GroupItem(R.string.camera_listview_shoot_super_resolutiob_photo, ShootSuperResolutionPhotoView.class),
                new GroupItem(R.string.camera_listview_record_video, RecordVideoView.class),
                new GroupItem(R.string.camera_listview_playback_push_info, PlaybackPushInfoView.class),
                new GroupItem(R.string.camera_listview_playback_command, PlaybackCommandsView.class),
                new GroupItem(R.string.camera_listview_playback_download, PlaybackDownloadView.class),
                new GroupItem(R.string.camera_listview_download_media, FetchMediaView.class),
                new GroupItem(R.string.camera_listview_media_playback, MediaPlaybackView.class),
                new GroupItem(R.string.component_listview_video_feeder, VideoFeederView.class),
                new GroupItem(R.string.component_xt2_camera_view, XT2CameraView.class),
                new GroupItem(R.string.camera_calibration, CameraCalibration.class));

        builder.addGroup(R.string.component_listview_gimbal,
                false,
                new GroupItem(R.string.gimbal_listview_push_info, PushGimbalDataView.class),
                new GroupItem(R.string.gimbal_listview_rotate_gimbal, MoveGimbalWithSpeedView.class),
                new GroupItem(R.string.gimbal_listview_gimbal_capability, GimbalCapabilityView.class));

        builder.addGroup(R.string.component_listview_battery,
                false,
                new GroupItem(R.string.battery_listview_push_info, PushBatteryDataView.class),
                new GroupItem(R.string.battery_listview_set_get_discharge_day, SetGetDischargeDayView.class));

        builder.addGroup(R.string.component_listview_airlink,
                false,
                new GroupItem(R.string.airlink_listview_wifi_set_get_ssid, SetGetWiFiLinkSSIDView.class),
                new GroupItem(R.string.airlink_listview_wifi_reboot_wifi, RebootWiFiAirlinkView.class),
                new GroupItem(R.string.airlink_listview_lb_set_get_channel, SetGetWiFiLinkSSIDView.class),
                new GroupItem(R.string.airlink_listview_ocusync_set_get_channel, SetGetOcuSyncLinkView.class));

        builder.addGroup(R.string.component_listview_flight_controller,
                false,
                new GroupItem(R.string.flight_controller_listview_compass_calibration,
                        CompassCalibrationView.class),
                new GroupItem(R.string.flight_controller_listview_flight_limitation,
                        FlightLimitationView.class),
                new GroupItem(R.string.flight_controller_listview_orientation_mode, OrientationModeView.class),
                new GroupItem(R.string.flight_controller_listview_virtual_stick, VirtualStickView.class),
                new GroupItem(R.string.flight_controller_listview_intelligent_flight_assistant,
                        FlightAssistantPushDataView.class),
                new GroupItem(R.string.flight_controller_listview_networkRTK, NetworkRTKView.class));

        builder.addGroup(R.string.component_listview_remote_controller,
                false,
                new GroupItem(R.string.remote_controller_listview_push_info,
                        PushRemoteControllerDataView.class),
                new GroupItem(R.string.component_listview_mobile_remote_controller,
                        MobileRemoteControllerView.class));

        // Set-up ExpandableListView
        expandableListView = (ExpandableListView) view.findViewById(R.id.expandable_list);
        listAdapter = new ExpandableListAdapter(context, builder.build());
        expandableListView.setAdapter(listAdapter);
        DJISampleApplication.getEventBus().register(this);
        expandAllGroupIfNeeded();
    }

    @Subscribe
    public void onSearchQueryEvent(MainActivity.SearchQueryEvent event) {
        listAdapter.filterData(event.getQuery());
        expandAllGroup();
    }

    /**
     * Expands all the group that has children
     */
    private void expandAllGroup() {
        int count = listAdapter.getGroupCount();
        for (int i = 0; i < count; i++) {
            expandableListView.expandGroup(i);
        }
    }

    /**
     * Expands all the group that has children
     */
    private void expandAllGroupIfNeeded() {
        int count = listAdapter.getGroupCount();
        for (int i = 0; i < count; i++) {
            if (listAdapter.getGroup(i) instanceof GroupHeader
                    && ((GroupHeader) listAdapter.getGroup(i)).shouldCollapseByDefault()) {
                expandableListView.collapseGroup(i);
            } else {
                expandableListView.expandGroup(i);
            }
        }
    }
}
