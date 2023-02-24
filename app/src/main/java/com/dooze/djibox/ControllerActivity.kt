package com.dooze.djibox

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.view.*
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.maps.AMap
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.MyLocationStyle
import com.amap.api.maps.offlinemap.OfflineMapActivity
import com.dooze.djibox.databinding.ActivityControllerBinding
import com.dooze.djibox.events.HotPointMissionConfigEvent
import com.dooze.djibox.events.HotPointMissionEvent
import com.dooze.djibox.extensions.makeVibrate
import com.dooze.djibox.extensions.message
import com.dooze.djibox.extensions.showSnack
import com.dooze.djibox.internal.controller.App
import com.dooze.djibox.internal.controller.MainActivity
import com.dooze.djibox.internal.utils.ToastUtils
import com.dooze.djibox.internal.view.MainContent
import com.dooze.djibox.map.point
import com.dooze.djibox.map.zoomTo
import com.dooze.djibox.widgets.GimbalAdjustView
import com.google.android.material.snackbar.Snackbar
import com.squareup.otto.Subscribe
import dji.common.error.DJIError
import dji.common.error.DJISDKError
import dji.common.mission.hotpoint.HotpointMissionEvent
import dji.common.useraccount.UserAccountState
import dji.common.util.CommonCallbacks.CompletionCallbackWith
import dji.log.DJILog
import dji.sdk.base.BaseComponent
import dji.sdk.base.BaseProduct
import dji.sdk.base.BaseProduct.ComponentKey
import dji.sdk.mission.MissionControl
import dji.sdk.mission.MissionControl.Listener
import dji.sdk.mission.hotpoint.HotpointMissionOperator
import dji.sdk.mission.hotpoint.HotpointMissionOperatorListener
import dji.sdk.sdkmanager.DJISDKInitEvent
import dji.sdk.sdkmanager.DJISDKManager
import dji.sdk.sdkmanager.DJISDKManager.SDKManagerCallback
import dji.sdk.useraccount.UserAccountManager
import dji.ux.widget.controls.CameraCaptureWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pdb.app.base.extensions.roundedCorner

/**
 * @author: liangguidong
 * @date: 2022/8/6 12:05
 * @lastModifyUser: liangguidong
 * @lastModifyDate: 2022/8/6 12:05
 * @description:
 */
class ControllerActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityControllerBinding

    private var locationClient: AMapLocationClient? = null

    private val flightStateHelper = FlightStateHelper()
    private val hotPointHelper = HotPointHelper()
    private val wayPointHelper = WayPointHelper()
    private val groundMissionHelper = GroundMissionHelper()
    private val rtkHelper = RTKHelper()

    private val missionListener = Listener { timelineElement, timelineEvent, djiError ->
        showSnack(
            "Timeline Update $timelineEvent (${djiError?.description}:${djiError?.errorCode})"
        )
    }

    private var hotpointMissionOperator: HotpointMissionOperator? = null

    private val missionControl by lazy {
        MissionControl.getInstance()
    }

    private var gimbalAdjustView: GimbalAdjustView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_controller)
        App.getEventBus().register(this)
        binding = ActivityControllerBinding.bind(findViewById(R.id.rootDrawer))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                this, arrayOf<String>(
                    Manifest.permission.CAMERA,
                    Manifest.permission.VIBRATE, // Gimbal rotation
                    // Gimbal rotation
                    Manifest.permission.INTERNET, // API requests
                    // API requests
                    Manifest.permission.ACCESS_WIFI_STATE, // WIFI connected products
                    // WIFI connected products
                    Manifest.permission.ACCESS_COARSE_LOCATION, // Maps
                    // Maps
                    Manifest.permission.ACCESS_NETWORK_STATE, // WIFI connected products
                    // WIFI connected products
                    Manifest.permission.ACCESS_FINE_LOCATION, // Maps
                    // Maps
                    Manifest.permission.CHANGE_WIFI_STATE, // Changing between WIFI and USB connection
                    // Changing between WIFI and USB connection
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, // Log files
                    // Log files
                    Manifest.permission.BLUETOOTH, // Bluetooth connected products
                    // Bluetooth connected products
                    Manifest.permission.BLUETOOTH_ADMIN, // Bluetooth connected products
                    // Bluetooth connected products
                    Manifest.permission.READ_EXTERNAL_STORAGE, // Log files
                    // Log files
                    Manifest.permission.READ_PHONE_STATE, // Device UUID accessed upon registration
                    // Device UUID accessed upon registration
                    Manifest.permission.RECORD_AUDIO // Speaker accessory
                    // Speaker accessory
                ), 1
            )
        } else {
            init(savedInstanceState)
        }
        binding.rootLayout.doOnPreDraw {
            binding.fragmentContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                width = resources.displayMetrics.heightPixels
            }
        }
        binding.tvFunHotPoint.setOnClickListener(this)
        binding.tvFunWayPoint.setOnClickListener(this)
        binding.tvFunMediaManager.setOnClickListener(this)
        binding.ibFpvMapLayer.setOnClickListener(this)
        binding.tvFunOfflineMap.setOnClickListener(this)
        binding.bottomActionLayout.roundedCorner(4)
        binding.ivLayer.setOnClickListener(this)
        binding.ivMyLocation.setOnClickListener(this)
        binding.tvFunGimbalAdjust.setOnClickListener(this)
        binding.tvFunGroundMission.setOnClickListener(this)
        binding.rtkSwitcher.setOnClickListener(this)
        binding.tvFunUserAccount.setOnClickListener(this)
        binding.ivExit.setOnLongClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            true
        }
        WindowInsetsControllerCompat(
            window,
            window.decorView
        ).hide(WindowInsetsCompat.Type.statusBars())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    private fun initSDK() {
        val mDJIComponentListener = BaseComponent.ComponentListener { isConnected ->
            Log.d(MainContent.TAG, "onComponentConnectivityChanged: $isConnected")
        }
        lifecycleScope.launch(Dispatchers.Default) {
            DJISDKManager.getInstance()
                .registerApp(applicationContext, object : SDKManagerCallback {
                    override fun onRegister(djiError: DJIError) {
                        if (djiError === DJISDKError.REGISTRATION_SUCCESS) {
                            DJILog.e(
                                "App registration",
                                DJISDKError.REGISTRATION_SUCCESS.description
                            )
                            DJISDKManager.getInstance().startConnectionToProduct()
                            ToastUtils.setResultToToast(getString(R.string.sdk_registration_success_message))
                        } else {
                            ToastUtils.setResultToToast(
                                getString(R.string.sdk_registration_message)
                                    .toString() + djiError.description
                            )
                        }
                        Log.v(MainContent.TAG, djiError.description)
                    }

                    override fun onProductDisconnect() {
                        Log.d(MainContent.TAG, "onProductDisconnect")
                    }

                    override fun onProductConnect(baseProduct: BaseProduct?) {
                        Log.d(
                            MainContent.TAG,
                            String.format("onProductConnect newProduct:%s", baseProduct)
                        )
                        launch {
                            kotlin.runCatching {
                                rtkHelper.start()
                            }.exceptionOrNull()?.let {
                                showSnack(it.message ?: return@let)
                            }
                        }
                    }

                    override fun onProductChanged(baseProduct: BaseProduct?) {
                    }

                    override fun onComponentChange(
                        componentKey: ComponentKey?,
                        oldComponent: BaseComponent?,
                        newComponent: BaseComponent?
                    ) {
                        newComponent?.setComponentListener(mDJIComponentListener)
                        Log.d(
                            MainContent.TAG, String.format(
                                "onComponentChange key:%s, oldComponent:%s, newComponent:%s",
                                componentKey,
                                oldComponent,
                                newComponent
                            )
                        )
                    }

                    override fun onInitProcess(djisdkInitEvent: DJISDKInitEvent, i: Int) {}
                    override fun onDatabaseDownloadProgress(current: Long, total: Long) {
//                    val process = (100 * current / total).toInt()
//                    if (process == lastProcess) {
//                        return
//                    }
//                    lastProcess = process
//                    showProgress(process)
//                    if (process % 25 == 0) {
//                        ToastUtils.setResultToToast("DB load process : $process")
//                    } else if (process == 0) {
//                        ToastUtils.setResultToToast("DB load begin")
//                    }
                    }
                })
        }

        rtkHelper.rtkState.observe(this) { rtkState ->
            rtkState ?: return@observe
            flightStateHelper.rtkState = rtkState
        }
    }

    val contentRoot: RelativeLayout
        get() = binding.rootLayout

    private fun init(savedInstanceState: Bundle?) {
        initSDK()
        binding.ivExit.setOnClickListener {
            finish()
        }
        binding.tvMoreFun.setOnClickListener {
            binding.root.openDrawer(Gravity.LEFT)
        }

        val context = this
        val mapView = binding.mapView
        AMapLocationClient.updatePrivacyShow(context, true, true)
        AMapLocationClient.updatePrivacyAgree(context, true)
        mapView.onCreate(savedInstanceState)
        mapView.map.uiSettings.apply {
            isZoomControlsEnabled = false
        }
        if (locationClient != null) return
        val locationClient = AMapLocationClient(context)
        this.locationClient = locationClient
        locationClient.setLocationOption(AMapLocationClientOption().apply {
            locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
        })
        mapView.map.myLocationStyle = MyLocationStyle().apply {
            myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_my_location_circle))
            this.radiusFillColor(Color.TRANSPARENT)
            strokeWidth(0f)
            myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
        }
        mapView.map.isMyLocationEnabled = true

        var firstLocation = true
        locationClient.setLocationListener {
            mapView.map.myLocation.set(it)
            if (firstLocation) {
                mapView.zoomTo(it.point)
            }
            firstLocation = false
        }
        locationClient.startLocation()


        val pickMarkers = listOf(hotPointHelper, wayPointHelper, groundMissionHelper)

        mapView.map.setOnMapLongClickListener { point ->
            if (pickMarkers.firstOrNull { it.onPickPoint(point) } == null) {
                //mapView.zoomTo(mapView.map.myLocation?.point ?: return@setOnMapLongClickListener)
            }
            makeVibrate()
        }

        mapView.map.setOnMarkerClickListener { marker ->
            pickMarkers.first { it.onMarkerClick(marker) }
            true
        }


        binding.rootDrawer.addDrawerListener(object : DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            }

            override fun onDrawerOpened(drawerView: View) {
                updateLoginState()
            }

            override fun onDrawerClosed(drawerView: View) {
            }

            override fun onDrawerStateChanged(newState: Int) {
            }

        })

        flightStateHelper.init(binding.mapView, binding.tvDistance)
        hotPointHelper.init(binding.mapView, this)
        wayPointHelper.init(binding.mapView, this)
        groundMissionHelper.init(binding.mapView, this)
    }

    private fun updateLoginState() {
        when (UserAccountManager.getInstance().userAccountState) {
            UserAccountState.AUTHORIZED -> {
                binding.tvFunUserAccount.text = getString(R.string.uesr_logined)
            }

            UserAccountState.NOT_AUTHORIZED, UserAccountState.TOKEN_OUT_OF_DATE -> {
                binding.tvFunUserAccount.text = getString(R.string.user_login_invalid)
            }

            else -> {
                binding.tvFunUserAccount.text = getString(R.string.fun_login)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }


    @Subscribe
    fun onHotPointTimelineSetup(event: HotPointMissionEvent) {
        Log.i("DjiBox", "missionControl.scheduledCount ${missionControl.scheduledCount()}")
        if (missionControl.scheduledCount() > 0) {
            missionControl.unscheduleEverything()
            missionControl.removeAllListeners()
        }
        val error = missionControl.scheduleElements(event.elements)
        missionControl.addListener(missionListener)
        if (error != null && error.errorCode != 0) {
            binding.root.showSnack(
                getString(
                    R.string.hot_pooint_execution_error,
                    "Fallback2:${error.description}(${error.errorCode})"
                )
            )
        } else {
            supportFragmentManager.fragments.find { it is HotPointConfigFragment }?.let {
                supportFragmentManager.beginTransaction()
                    .remove(it)
                    .commit()
            }
        }
    }


    @Subscribe
    fun onHotpotMissionConfigEvent(event: HotPointMissionConfigEvent) {
        //startHotpointMission(event)
        hotPointHelper.startCapture(event) {
            binding.CameraCapturePanel.children.find { it is CameraCaptureWidget }
                ?.performClick()
        }
    }

    private fun startHotpointMission(event: HotPointMissionConfigEvent) {
        val mission = event.mission
        val operator = hotpointMissionOperator
            ?: DJISDKManager.getInstance().missionControl.hotpointMissionOperator.also { missionOperator ->
                hotpointMissionOperator = missionOperator
                missionOperator.addListener(object : HotpointMissionOperatorListener {
                    override fun onExecutionUpdate(p0: HotpointMissionEvent) {
                    }

                    override fun onExecutionStart() {
                        hotPointHelper.startCapture(event) {
                            binding.CameraCapturePanel.children.find { it is CameraCaptureWidget }
                                ?.performClick()
                        }
                    }

                    override fun onExecutionFinish(p0: DJIError?) {
                        showSnack("HotPoint onExecutionFinish ${p0?.message}")
                    }

                })
            }
        operator.startMission(mission) {
            if (it != null) {
                binding.root.showSnack(
                    getString(
                        R.string.hot_pooint_start_mission_error,
                        "${it.description}(${it.errorCode})"
                    )
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        App.getEventBus().unregister(this)
        binding.mapView.onDestroy()
        locationClient?.stopLocation()
        locationClient?.onDestroy()
        locationClient = null
    }

    private fun closeDrawer() {
        binding.rootDrawer.closeDrawer(Gravity.LEFT)
    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.tvFunHotPoint -> {
                closeDrawer()
                //pickLocation.launch(0)
                hotPointHelper.markStartHotPoint(this)
            }

            R.id.tvFunWayPoint -> {
                closeDrawer()
//                startActivity(
//                    Intent(
//                        this@ControllerActivity,
//                        WapPointActivity::class.java
//                    )
//                )
                wayPointHelper.markStartWapPoint()
            }

            R.id.tvFunMediaManager -> {
                closeDrawer()
                showFragment(MediaManagerFragment())
            }

            R.id.tvFunGroundMission -> {
                closeDrawer()
                groundMissionHelper.markStartWapPoint()
            }

            R.id.tvFunGimbalAdjust -> {
                closeDrawer()
                showGimbalAdjust()
            }

            R.id.ibFpvMapLayer -> {
                changeMapViewMode(!binding.mapView.isVisible)
            }

            R.id.ivMyLocation -> {
                locationClient?.lastKnownLocation?.let {
                    binding.mapView.zoomTo(it.point, 17f)
                }
            }

            R.id.ivLayer -> {
                PopupMenu(this, p0, Gravity.TOP).apply {
                    this.menuInflater.inflate(R.menu.map_layer_menu, menu)
                    setOnMenuItemClickListener { menu ->
                        val type = when (menu.itemId) {
                            R.id.mayLayerNavi -> {
                                AMap.MAP_TYPE_NAVI
                            }

                            R.id.mayLayerSatellite -> {
                                AMap.MAP_TYPE_SATELLITE
                            }

                            else -> {
                                AMap.MAP_TYPE_NORMAL
                            }
                        }
                        binding.mapView.map.mapType = type
                        WindowInsetsControllerCompat(window, window.decorView).hide(
                            WindowInsetsCompat.Type.statusBars()
                        )
                        true
                    }
                }.show()
            }

            R.id.tvFunOfflineMap -> {
                binding.rootDrawer.closeDrawer(Gravity.LEFT)
                startActivity(Intent(this, OfflineMapActivity::class.java))
            }

            R.id.rtkSwitcher -> {
                binding.rtkSwitcher.toggleChecked()
                if (binding.rtkSwitcher.isChecked) {
                    lifecycleScope.launch {
                        kotlin.runCatching {
                            rtkHelper.start()
                        }.exceptionOrNull()?.let {
                            showSnack(it.message ?: return@let)
                        }
                    }
                    if (!rtkHelper.attachCallback()) {
                        showSnack(getString(R.string.fun_rtk_not_avaliable))
                    }
                } else {
                    rtkHelper.detachCallback()
                    flightStateHelper.rtkState = null
                }
            }

            R.id.tvFunUserAccount -> {
                when (UserAccountManager.getInstance().userAccountState) {
                    UserAccountState.AUTHORIZED -> {
                        Snackbar.make(
                            window.decorView,
                            getString(R.string.fun_login_out_confirm),
                            Snackbar.LENGTH_LONG
                        ).setAction(getString(R.string.confirm)) {
                            UserAccountManager.getInstance().logoutOfDJIUserAccount {
                                if (it != null) {
                                    showSnack(
                                        getString(
                                            R.string.fun_login_out_error,
                                            it.description
                                        )
                                    )
                                }
                                updateLoginState()
                            }
                        }.show()
                    }

                    else -> {
                        UserAccountManager.getInstance().logIntoDJIUserAccount(
                            this,
                            object : CompletionCallbackWith<UserAccountState> {
                                override fun onSuccess(p0: UserAccountState?) {
                                    updateLoginState()
                                }

                                override fun onFailure(p0: DJIError?) {
                                    showSnack(
                                        getString(
                                            R.string.fun_login_in_error,
                                            p0?.description ?: ""
                                        )
                                    )
                                }
                            })
                    }
                }
            }
        }
    }

    fun changeMapViewMode(showMap: Boolean) {
        binding.mapView.isVisible = showMap
        binding.mapRightActionLayout.isVisible = binding.mapView.isVisible
        binding.CameraCapturePanel.isVisible = !binding.mapView.isVisible
        binding.camera.isVisible = !binding.mapView.isVisible
    }

    private fun showGimbalAdjust() {
        val gimbalAdjustView = gimbalAdjustView ?: GimbalAdjustView(this).also {
            gimbalAdjustView = it
        }
        binding.fragmentContainer.addView(
            gimbalAdjustView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        gimbalAdjustView.attach()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            init(null)
        } else {
            showSnack(getString(R.string.app_require_all_permission_granted))
        }
    }


    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .show(fragment)
            .commit()
    }

}