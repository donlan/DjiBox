package com.dooze.djibox.fun.flightcontroller;

import android.content.Context;

import com.dooze.djibox.R;
import com.dooze.djibox.internal.controller.App;
import com.dooze.djibox.internal.utils.DialogUtils;
import com.dooze.djibox.internal.utils.ModuleVerificationUtil;
import com.dooze.djibox.internal.utils.ToastUtils;
import com.dooze.djibox.internal.view.BaseThreeBtnView;

import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;

/**
 * Class for flight limitation.
 */
public class FlightLimitationView extends BaseThreeBtnView {
    private boolean mRadiusLimitaionToggleFlag = true;

    public FlightLimitationView(Context context) {
        super(context);
    }

    @Override
    protected int getMiddleBtnTextResourceId() {
        return R.string.flight_limitation_set_height_limitation;
    }

    @Override
    protected int getLeftBtnTextResourceId() {
        return R.string.flight_limitation_toggle_enable_radius_limitation;
    }

    @Override
    protected int getRightBtnTextResourceId() {
        return R.string.flight_limitation_set_radius_limitation;
    }

    @Override
    protected int getDescriptionResourceId() {
        return R.string.flight_limitation_description;
    }

    @Override
    protected void handleMiddleBtnClick() {
        if (ModuleVerificationUtil.isFlightLimitationAvailable()) {
            App.getAircraftInstance()
                    .getFlightController()
                    .setMaxFlightHeight(100, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            ToastUtils.setResultToToast("Result: " + (djiError == null
                                    ? "Success"
                                    : djiError.getDescription()));
                        }
                    });
        }
    }

    @Override
    protected void handleLeftBtnClick() {
        if (ModuleVerificationUtil.isFlightLimitationAvailable()) {
            App.getAircraftInstance()
                    .getFlightController()
                    .setMaxFlightRadiusLimitationEnabled(mRadiusLimitaionToggleFlag,
                            new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    DialogUtils.showDialogBasedOnError(getContext(),
                                            djiError);
                                    mRadiusLimitaionToggleFlag =
                                            mRadiusLimitaionToggleFlag ^ true;
                                }
                            });
        }
    }

    @Override
    protected void handleRightBtnClick() {
        if (ModuleVerificationUtil.isFlightLimitationAvailable()) {
            App.getAircraftInstance()
                    .getFlightController()
                    .setMaxFlightRadius(40, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            ToastUtils.setResultToToast("Result: " + (djiError == null
                                    ? "Success"
                                    : djiError.getDescription()));
                        }
                    });
        }
    }

    @Override
    public int getDescription() {
        return R.string.flight_controller_listview_flight_limitation;
    }
}
