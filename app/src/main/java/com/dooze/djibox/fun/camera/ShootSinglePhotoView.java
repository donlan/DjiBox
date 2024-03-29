package com.dooze.djibox.fun.camera;

import android.content.Context;
import android.util.Log;

import com.dooze.djibox.R;
import com.dooze.djibox.internal.controller.App;
import com.dooze.djibox.internal.utils.ModuleVerificationUtil;
import com.dooze.djibox.internal.utils.ToastUtils;
import com.dooze.djibox.internal.view.BaseThreeBtnView;

import androidx.annotation.NonNull;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.sdk.camera.Camera;
import dji.sdk.media.MediaFile;

/**
 * Class for shooting single photo.
 */
public class ShootSinglePhotoView extends BaseThreeBtnView {

    private Context context;
    private Camera camera;

    public ShootSinglePhotoView(Context context) {
        super(context);
        this.context = context;
    }

    /**
     * Every commands relative to the shooting photos are only allowed executed in shootphoto work
     * mode.
     */
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.v("Attached To Window", "onAttachedToWindow");

        if (ModuleVerificationUtil.isCameraModuleAvailable()) {
            camera = App.getAircraftInstance().getCamera();
            if (ModuleVerificationUtil.isMatrice300RTK() || ModuleVerificationUtil.isMavicAir2()) {
                camera.setFlatMode(SettingsDefinitions.FlatCameraMode.PHOTO_SINGLE, djiError -> ToastUtils.setResultToToast("setFlatMode to PHOTO_SINGLE"));
            } else {
                camera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, djiError -> ToastUtils.setResultToToast("setMode to shoot_PHOTO"));
            }
            camera.setMediaFileCallback(new MediaFile.Callback() {
                @Override
                public void onNewFile(@NonNull MediaFile mediaFile) {
                    ToastUtils.setResultToToast("New photo generated");
                }
            });
        }
    }

    private boolean isModuleAvailable() {
        return (null != App.getProductInstance()) && (null != App.getProductInstance()
                .getCamera());
    }

    @Override
    protected int getLeftBtnTextResourceId() {
        return DISABLE;
    }

    @Override
    protected int getDescriptionResourceId() {
        return getDescription();
    }

    @Override
    protected void handleLeftBtnClick() {

    }

    @Override
    protected void handleMiddleBtnClick() {
        //Shoot Photo Button
        if (isModuleAvailable()) {
            post(new Runnable() {
                @Override
                public void run() {
                    middleBtn.setEnabled(false);
                }
            });

            App.getProductInstance()
                    .getCamera()
                    .startShootPhoto(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (null == djiError) {
                                ToastUtils.setResultToToast(getContext().getString(R.string.success));
                            } else {
                                ToastUtils.setResultToToast(djiError.getDescription());
                            }
                            post(new Runnable() {
                                @Override
                                public void run() {
                                    middleBtn.setEnabled(true);
                                }
                            });
                        }
                    });
        }
    }

    @Override
    protected void handleRightBtnClick() {
    }

    @Override
    protected int getRightBtnTextResourceId() {
        return DISABLE;
    }

    @Override
    protected int getMiddleBtnTextResourceId() {
        return R.string.shoot_single_photo;
    }

    @Override
    public int getDescription() {
        return R.string.camera_listview_shoot_single_photo;
    }
}
