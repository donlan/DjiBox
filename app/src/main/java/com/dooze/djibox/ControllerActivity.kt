package com.dooze.djibox

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import com.amap.api.maps.model.LatLng
import com.dooze.djibox.databinding.ActivityControllerBinding
import com.dooze.djibox.extensions.showSnack
import com.dooze.djibox.map.PickLocationActivity
import dji.sdk.sdkmanager.DJISDKManager

/**
 * @author: liangguidong
 * @date: 2022/8/6 12:05
 * @lastModifyUser: liangguidong
 * @lastModifyDate: 2022/8/6 12:05
 * @description:
 */
class ControllerActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityControllerBinding

    private val hotpointMissionOperator by lazy {
        DJISDKManager.getInstance().missionControl.hotpointMissionOperator
    }

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
            init()
        }
        binding.rootLayout.doOnPreDraw {
            binding.fragmentContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                width = resources.displayMetrics.heightPixels
            }
        }
        binding.tvFunHotPoint.setOnClickListener(this)
        binding.tvFunWayPoint.setOnClickListener(this)
    }

    private fun init() {
        binding.ivExit.setOnClickListener {
            finish()
        }
        binding.tvMoreFun.setOnClickListener {
            binding.root.openDrawer(Gravity.LEFT)
//            PopupMenu(this, it, Gravity.TOP).apply {
//                this.menuInflater.inflate(R.menu.controller_more_funs, menu)
//                setOnMenuItemClickListener { menu ->
//                    when (menu.itemId) {
//                        R.id.menuWayPoint -> {
//                            startActivity(
//                                Intent(
//                                    this@ControllerActivity,
//                                    WapPointActivity::class.java
//                                )
//                            )
//                        }
//                        R.id.menuHotPoint -> {
//                            pickLocation.launch(0)
//                        }
//                    }
//                    true
//                }
//            }.show()
        }
    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.tvFunHotPoint -> {
                binding.rootDrawer.closeDrawer(Gravity.LEFT)
                pickLocation.launch(0)
            }
            R.id.tvFunWayPoint -> {
                binding.rootDrawer.closeDrawer(Gravity.LEFT)
                startActivity(
                    Intent(
                        this@ControllerActivity,
                        WapPointActivity::class.java
                    )
                )
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
            init()
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