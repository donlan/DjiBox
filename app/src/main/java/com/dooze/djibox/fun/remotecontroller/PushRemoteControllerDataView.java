package com.dooze.djibox.fun.remotecontroller;

import android.content.Context;

import androidx.annotation.NonNull;

import com.dooze.djibox.R;
import com.dooze.djibox.internal.controller.App;
import com.dooze.djibox.internal.utils.ModuleVerificationUtil;
import com.dooze.djibox.internal.view.BasePushDataView;

import dji.common.remotecontroller.HardwareState;
import dji.sdk.products.Aircraft;
import dji.sdk.remotecontroller.RemoteController;

/**
 * Class for getting remote controller information.
 */
public class PushRemoteControllerDataView extends BasePushDataView {

    private RemoteController remoteController;

    public PushRemoteControllerDataView(Context context) {
        super(context);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (ModuleVerificationUtil.isRemoteControllerAvailable()) {
            remoteController = ((Aircraft) App.getProductInstance()).getRemoteController();

            remoteController.setHardwareStateCallback(new HardwareState.HardwareStateCallback() {
                @Override
                public void onUpdate(@NonNull HardwareState rcHardwareState) {
                    stringBuffer.delete(0, stringBuffer.length());

                    stringBuffer.append("FlightModeSwitch: ").
                        append(rcHardwareState.getFlightModeSwitch().name()).append("\n");
                    stringBuffer.append("OnClickGoHomeBtn: ").
                        append(rcHardwareState.getGoHomeButton().isClicked()).append("\n");
                    stringBuffer.append("RightHorizontalChanged: ")
                                .append(rcHardwareState.getRightStick().getHorizontalPosition())
                                .append("\n");

                    showStringBufferResult();
                }
            });
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (ModuleVerificationUtil.isRemoteControllerAvailable()) {
            remoteController = ((Aircraft) App.getProductInstance()).getRemoteController();

            remoteController.setHardwareStateCallback(null);
        }
    }

    @Override
    public int getDescription() {
        return R.string.remote_controller_listview_push_info;
    }
}
