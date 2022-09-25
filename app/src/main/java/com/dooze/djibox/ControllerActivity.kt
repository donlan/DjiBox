package com.dooze.djibox

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.view.*
import androidx.fragment.app.Fragment
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.maps.AMap
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MyLocationStyle
import com.amap.api.maps.offlinemap.OfflineMapActivity
import com.dooze.djibox.databinding.ActivityControllerBinding
import com.dooze.djibox.extensions.makeVibrate
import com.dooze.djibox.extensions.showSnack
import com.dooze.djibox.map.IPickPointMarker
import com.dooze.djibox.map.PickLocationActivity
import com.dooze.djibox.map.point
import com.dooze.djibox.map.zoomTo
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_controller)
        binding = ActivityControllerBinding.bind(findViewById(R.id.rootDrawer))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                this, arrayOf<String>(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.READ_PHONE_STATE
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
        binding.mapView.onCreate(savedInstanceState)
        binding.tvFunHotPoint.setOnClickListener(this)
        binding.tvFunWayPoint.setOnClickListener(this)
        binding.tvFunMediaManager.setOnClickListener(this)
        binding.ibFpvMapLayer.setOnClickListener(this)
        binding.tvFunOfflineMap.setOnClickListener(this)
        binding.bottomActionLayout.roundedCorner(4)
        binding.ivLayer.setOnClickListener(this)
        binding.ivMyLocation.setOnClickListener(this)
        WindowInsetsControllerCompat(window, window.decorView).hide(WindowInsetsCompat.Type.statusBars())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    private fun init(savedInstanceState: Bundle?) {
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


        val pickMarkers = listOf<IPickPointMarker>(hotPointHelper, wayPointHelper)

        mapView.map.setOnMapLongClickListener { point ->
            mapView.zoomTo(mapView.map.myLocation?.point ?: return@setOnMapLongClickListener)
            pickMarkers.first { it.onPickPoint(point) }
            makeVibrate()
        }

        mapView.map.setOnMarkerClickListener { marker ->
            pickMarkers.first { it.onMarkerClick(marker) }
            true
        }

        flightStateHelper.init(binding.mapView)
        hotPointHelper.init(binding.mapView, this)
        wayPointHelper.init(binding.mapView, this)
    }

    override fun onResume() {
        super.onResume()
       binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
       binding.mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
        locationClient?.stopLocation()
        locationClient?.onDestroy()
        locationClient = null
    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.tvFunHotPoint -> {
                binding.rootDrawer.closeDrawer(Gravity.LEFT)
                //pickLocation.launch(0)
                hotPointHelper.markStartHotPoint(this)
            }
            R.id.tvFunWayPoint -> {
                binding.rootDrawer.closeDrawer(Gravity.LEFT)
//                startActivity(
//                    Intent(
//                        this@ControllerActivity,
//                        WapPointActivity::class.java
//                    )
//                )
                wayPointHelper.markStartWapPoint()
            }
            R.id.tvFunMediaManager -> {
                binding.rootDrawer.closeDrawer(Gravity.LEFT)
                showFragment(MediaManagerFragment())
            }
            R.id.ibFpvMapLayer -> {
                binding.mapView.isVisible = !binding.mapView.isVisible
                binding.mapRightActionLayout.isVisible = binding.mapView.isVisible
                binding.CameraCapturePanel.isVisible = !binding.mapView.isVisible
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
                        WindowInsetsControllerCompat(window, window.decorView).hide(WindowInsetsCompat.Type.statusBars())
                        true
                    }
                }.show()
            }
            R.id.tvFunOfflineMap -> {
                binding.rootDrawer.closeDrawer(Gravity.LEFT)
                startActivity(Intent(this, OfflineMapActivity::class.java))
            }
        }
    }


    private val pickLocation =
        registerForActivityResult(object : ActivityResultContract<Int, Pair<LatLng, Double?>?>() {
            override fun createIntent(context: Context, input: Int): Intent {
                return Intent(context, PickLocationActivity::class.java)
            }

            override fun parseResult(resultCode: Int, intent: Intent?): Pair<LatLng, Double?>? {
                val point: LatLng = intent?.extras?.getParcelable("Location") ?: return null
                val radius = intent.extras?.getDouble("Radius")
                return Pair(point, radius)
            }

        }) {
            val (point, radius) = it ?: return@registerForActivityResult
            if (radius == null) {
                showSnack(getString(R.string.hot_point_need_set_radius_alert))
                return@registerForActivityResult
            }
            showFragment(HotPointConfigFragment.newInstance(point, radius))
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