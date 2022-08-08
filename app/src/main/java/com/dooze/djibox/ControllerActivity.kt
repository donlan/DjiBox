package com.dooze.djibox

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.view.Gravity
import androidx.appcompat.widget.PopupMenu
import com.dooze.djibox.databinding.ActivityControllerBinding
import com.dooze.djibox.extensions.showSnack

/**
 * @author: liangguidong
 * @date: 2022/8/6 12:05
 * @lastModifyUser: liangguidong
 * @lastModifyDate: 2022/8/6 12:05
 * @description:
 */
class ControllerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityControllerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_controller)
        binding = ActivityControllerBinding.bind(findViewById(R.id.root_layout))
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
    }

    private fun init() {
        binding.ivExit.setOnClickListener {
            finish()
        }
        binding.tvMoreFun.setOnClickListener {
            PopupMenu(this, it, Gravity.TOP).apply {
                this.menuInflater.inflate(R.menu.controller_more_funs, menu)
                setOnMenuItemClickListener { menu ->
                    when (menu.itemId) {
                        R.id.menuWayPoint -> {
                            startActivity(Intent(this@ControllerActivity, WapPointActivity::class.java))
                        }
                    }
                    true
                }
            }.show()
        }
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

}