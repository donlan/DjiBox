package com.dooze.djibox

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.dooze.djibox.internal.controller.DJISampleApplication
import com.dooze.djibox.internal.utils.ModuleVerificationUtil
import dji.common.error.DJIError
import dji.common.flightcontroller.RTKState
import dji.common.flightcontroller.rtk.CoordinateSystem
import dji.common.flightcontroller.rtk.ReferenceStationSource
import dji.sdk.flightcontroller.RTK
import dji.sdk.network.RTKNetworkServiceProvider
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * @author: liangguidong
 * @date: 2022/11/6 16:09
 * @lastModifyUser: liangguidong
 * @lastModifyDate: 2022/11/6 16:09
 * @description:
 */
class RTKHelper {

    private var rtk: RTK? = null
    private var isNetworkRTKSet = false
    private var isCoordinatorSet = false
    private var isEnable = false
    private var isStarted = false

    private val _rtkState = MutableLiveData<RTKState?>(null)
    val rtkState: LiveData<RTKState?> = _rtkState

    private val rtkStateCallback = RTKState.Callback {
        _rtkState.value = it
    }

    fun init() {
        if (rtk != null) return
        if (ModuleVerificationUtil.isRTKAvailable()) {
            rtk = DJISampleApplication.getAircraftInstance().flightController.rtk
        }
    }

    fun attachCallback(): Boolean {
        if (ModuleVerificationUtil.isRTKAvailable() && rtk != null) {
            rtk!!.setStateCallback(rtkStateCallback)
            return true
        }
        return false
    }

    fun detachCallback() {
        if (ModuleVerificationUtil.isRTKAvailable() && rtk != null) {
            rtk!!.setStateCallback(null)
        }
        _rtkState.value = null
    }


    suspend fun start() {
        init()
        if (!isNetworkRTKSet) {
            setReferenceStationSource()
        }
        if (!isCoordinatorSet) {
            setRTKCoordinatorSystem()
        }
        if (!isEnable) {
            enableRTK()
        }

        if (!isStarted) {
            startRTKService()
        }
    }


    private suspend fun setReferenceStationSource() =
        suspendCancellableCoroutine<DJIError?> { ucont ->
            rtk!!.setReferenceStationSource(ReferenceStationSource.NETWORK_RTK) {
                if (it == null) {
                    isNetworkRTKSet = true
                    ucont.resumeWith(Result.success(null))
                } else {
                    ucont.resumeWith(Result.failure(it.toException()))
                }
            }
        }

    private suspend fun setRTKCoordinatorSystem() =
        suspendCancellableCoroutine<DJIError?> { ucont ->
            RTKNetworkServiceProvider.getInstance()
                .setNetworkServiceCoordinateSystem(CoordinateSystem.WGS84) {
                    if (it == null) {
                        isCoordinatorSet = true
                        ucont.resumeWith(Result.success(null))
                    } else {
                        ucont.resumeWith(Result.failure(it.toException()))
                    }
                }
        }

    private suspend fun enableRTK() = suspendCancellableCoroutine<DJIError?> { ucont ->
        rtk!!.setRtkEnabled(true) {
            if (it == null) {
                isEnable = true
                ucont.resumeWith(Result.success(null))
            } else {
                ucont.resumeWith(Result.failure(it.toException()))
            }
        }
    }


    private suspend fun startRTKService() = suspendCancellableCoroutine<DJIError?> { ucont ->
        RTKNetworkServiceProvider.getInstance().startNetworkService {
            if (it == null) {
                ucont.resumeWith(Result.success(null))
            } else {
                ucont.resumeWith(Result.failure(it.toException()))
            }
        }
    }

    private fun DJIError.toException() = RuntimeException("${this.errorCode}:${this.description}")

}