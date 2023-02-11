package com.dooze.djibox.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.dooze.djibox.R
import com.dooze.djibox.databinding.ViewGimbalAdjustBinding
import com.dooze.djibox.internal.controller.App
import com.dooze.djibox.internal.controller.MainActivity.RequestEndFullScreenEvent
import com.dooze.djibox.internal.controller.MainActivity.RequestStartFullScreenEvent
import com.dooze.djibox.internal.utils.CallbackHandlers.CallbackToastHandler
import com.dooze.djibox.internal.utils.Helper
import com.dooze.djibox.internal.utils.ToastUtils
import dji.common.error.DJIError
import dji.common.gimbal.CapabilityKey
import dji.common.gimbal.GimbalMode
import dji.common.gimbal.Rotation
import dji.common.gimbal.RotationMode
import dji.common.util.DJIParamCapability
import dji.common.util.DJIParamMinMaxCapability
import dji.keysdk.AirLinkKey
import dji.keysdk.callback.SetCallback
import dji.sdk.camera.VideoFeeder.*
import dji.sdk.gimbal.Gimbal
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import pdb.app.base.extensions.removeFromParent
import pdb.app.base.extensions.roundedCorner

/**
 * @author 梁桂栋
 * @date 2022/10/30  16:23.
 * e-mail 760625325@qq.com
 * GitHub: https://github.com/donlan
 * description: com.dooze.djibox.widgets
 * @version 1.0
 */
class GimbalAdjustView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attributeSet, defStyle) {

    private var sourceListener: PhysicalSourceListener? = null
    private var setBandwidthCallback: SetCallback? = null
    private var lbBandwidthKey: AirLinkKey? = null
    private var mainCameraBandwidthKey: AirLinkKey? = null


    private val binding: ViewGimbalAdjustBinding

    private var gimbal: Gimbal? = null
    private var currentGimbalId = 0

    init {
        roundedCorner(6)
        LayoutInflater.from(context).inflate(R.layout.view_gimbal_adjust, this, true)
        setBackgroundResource(R.drawable.bg_sheet)
        binding = ViewGimbalAdjustBinding.bind(this)
        initCallbacks()
        binding.ivBack.setOnClickListener {
            detach()
        }
        binding.gimbalYaw.onProgressChanged = {
            commitGimbalValue(CapabilityKey.ADJUST_YAW, it / 10f)
        }
        binding.gimbalPitch.onProgressChanged = {
            commitGimbalValue(CapabilityKey.ADJUST_PITCH, it / 10f)
        }
        binding.gimbalRoll.onProgressChanged = {
            commitGimbalValue(CapabilityKey.ADJUST_ROLL, it / 10f)
        }
    }

    private fun commitGimbalValue(key:CapabilityKey, value:Float) {
        val gimbal = gimbal ?: return
        val builder = Rotation.Builder().mode(RotationMode.ABSOLUTE_ANGLE).time(2.0)
        when(key) {
            CapabilityKey.ADJUST_YAW -> builder.yaw(value)
            CapabilityKey.ADJUST_PITCH -> builder.pitch(value)
            CapabilityKey.ADJUST_ROLL -> builder.roll(value)
            else -> return
        }
        gimbal.rotate(builder.build(), CallbackToastHandler())
    }

    private fun getGimbalInstance(): Gimbal? {
        if (gimbal == null) {
            initGimbal()
        }
        return gimbal
    }

    fun attach() {
        initGimbal()
    }

    fun detach() {
        removeFromParent()
    }

    private fun initGimbal() {
        if (DJISDKManager.getInstance() != null) {
            val product = DJISDKManager.getInstance().product
            if (product != null) {
                gimbal = if (product is Aircraft) {
                    product.gimbals[currentGimbalId]
                } else {
                    product.gimbal
                }
                setupUI(gimbal!!)
            }
        }
    }

    private fun initAllKeys() {
        lbBandwidthKey =
            AirLinkKey.createLightbridgeLinkKey(AirLinkKey.BANDWIDTH_ALLOCATION_FOR_LB_VIDEO_INPUT_PORT)
        mainCameraBandwidthKey =
            AirLinkKey.createLightbridgeLinkKey(AirLinkKey.BANDWIDTH_ALLOCATION_FOR_LEFT_CAMERA)
    }

    private fun initCallbacks() {
        setBandwidthCallback = object : SetCallback {
            override fun onSuccess() {
                ToastUtils.setResultToToast("Set key value successfully")
                binding.fpvVideoFeed.changeSourceResetKeyFrame()
                binding.primaryVideoFeed.changeSourceResetKeyFrame()
            }

            override fun onFailure(error: DJIError) {
                ToastUtils.setResultToToast("Failed to set: " + error.description)
            }
        }
    }

    private fun setupUI(gimbal: Gimbal) {
        setupGimbalValue(gimbal, binding.gimbalPitch)
        setupGimbalValue(gimbal, binding.gimbalYaw)
        setupGimbalValue(gimbal, binding.gimbalRoll)
    }

    private fun setupGimbalValue(gimbal: Gimbal, seekbarText: SeekbarText) {
        val key = when (seekbarText.id) {
            R.id.gimbalPitch -> CapabilityKey.ADJUST_PITCH
            R.id.gimbalYaw -> CapabilityKey.ADJUST_YAW
            else -> CapabilityKey.ADJUST_ROLL
        }
        val value = (gimbal.capabilities[key] as DJIParamMinMaxCapability?)!!
        val min = (value.min.toFloat() * 10).toInt()
        val max = (value.max.toFloat() * 10).toInt()
        seekbarText.setup(min, max, 1f)
    }

    private fun isFeatureSupported(key: CapabilityKey): Boolean {
        val gimbal = getGimbalInstance() ?: return false
        var capability: DJIParamCapability? = null
        if (gimbal.capabilities != null) {
            capability = gimbal.capabilities[key]
        }
        return capability?.isSupported ?: false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        App.getEventBus().post(RequestStartFullScreenEvent())
        if (getGimbalInstance() != null) {
            getGimbalInstance()!!.setMode(GimbalMode.YAW_FOLLOW, CallbackToastHandler())
        } else {
            ToastUtils.setResultToToast("Product disconnected")
        }
        setUpListeners()
    }

    override fun onDetachedFromWindow() {
        App.getEventBus().post(RequestEndFullScreenEvent())
        currentGimbalId = 0
        tearDownListeners()
        super.onDetachedFromWindow()
    }

    private fun setUpListeners() {
        sourceListener = PhysicalSourceListener { videoFeed, newPhysicalSource ->
            if (videoFeed === getInstance().primaryVideoFeed) {
//                val newText = "Primary Source: $newPhysicalSource"
//                ToastUtils.setResultToText(primaryVideoFeedTitle, newText)
            }
            if (videoFeed === getInstance().secondaryVideoFeed) {
//                ToastUtils.setResultToText(
//                    fpvVideoFeedTitle,
//                    "Secondary Source: $newPhysicalSource"
//                )
            }
        }
        setVideoFeederListeners(true)
    }

    private fun tearDownListeners() {
        setVideoFeederListeners(false)
    }

    private fun setVideoFeederListeners(isOpen: Boolean) {
        if (getInstance() == null) return
        val product = DJISDKManager.getInstance().product
        if (product != null) {
            val primaryVideoDataListener: VideoDataListener =
                binding.primaryVideoFeed.registerLiveVideo(
                    getInstance().primaryVideoFeed, true
                )
            val secondaryVideoDataListener: VideoDataListener =
                binding.fpvVideoFeed.registerLiveVideo(
                    getInstance().secondaryVideoFeed, false
                )
            if (isOpen) {
//                val newText = "Primary Source: " + getInstance().primaryVideoFeed.videoSource.name
//                ToastUtils.setResultToText(primaryVideoFeedTitle, newText)
//                if (Helper.isMultiStreamPlatform()) {
//                    val newTextFpv =
//                        "Secondary Source: " + getInstance().secondaryVideoFeed.videoSource.name
//                    ToastUtils.setResultToText(fpvVideoFeedTitle, newTextFpv)
//                }
                getInstance().addPhysicalSourceListener(sourceListener)
            } else {
                getInstance().removePhysicalSourceListener(sourceListener)
                getInstance().primaryVideoFeed.removeVideoDataListener(primaryVideoDataListener)
                if (Helper.isMultiStreamPlatform()) {
                    getInstance().secondaryVideoFeed.removeVideoDataListener(
                        secondaryVideoDataListener
                    )
                }
            }
        }
    }


}