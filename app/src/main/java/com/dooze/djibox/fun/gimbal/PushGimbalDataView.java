package com.dooze.djibox.fun.gimbal;

import android.content.Context;

import androidx.annotation.NonNull;

import com.dooze.djibox.R;
import com.dooze.djibox.internal.controller.App;
import com.dooze.djibox.internal.utils.ModuleVerificationUtil;
import com.dooze.djibox.internal.view.BasePushDataView;

import dji.common.gimbal.GimbalState;

/**
 * Class for getting gimbal information.
 */
public class PushGimbalDataView extends BasePushDataView {

    public PushGimbalDataView(Context context) {
        super(context);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (ModuleVerificationUtil.isGimbalModuleAvailable()) {
            App.getProductInstance().getGimbal().setStateCallback(new GimbalState.Callback() {
                @Override
                public void onUpdate(@NonNull GimbalState gimbalState) {
                    stringBuffer.delete(0, stringBuffer.length());

                    stringBuffer.append("PitchInDegrees: ").
                            append(gimbalState.getAttitudeInDegrees().getPitch()).append("\n");
                    stringBuffer.append("RollInDegrees: ").
                            append(gimbalState.getAttitudeInDegrees().getRoll()).append("\n");
                    stringBuffer.append("YawInDegrees: ").
                            append(gimbalState.getAttitudeInDegrees().getYaw()).append("\n");

                    showStringBufferResult();
                }
            });
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (ModuleVerificationUtil.isGimbalModuleAvailable()) {
            App.getProductInstance().getGimbal().setStateCallback(null);
        }
    }

    @Override
    public int getDescription() {
        return R.string.gimbal_listview_push_info;
    }
}
