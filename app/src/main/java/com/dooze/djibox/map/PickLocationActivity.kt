package com.dooze.djibox.map

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dooze.djibox.R

/**
 * @author: liangguidong
 * @date: 2022/8/24 14:09
 * @lastModifyUser: liangguidong
 * @lastModifyDate: 2022/8/24 14:09
 * @description:
 */
class PickLocationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_container)
        val fragment =
            PickLocationSheet.newInstance(PickLocationSheet.ATTACH_OTHER_ACTIVITY) { point, radius ->
                setResult(RESULT_OK, Intent().apply {
                    putExtra("Location", point)
                    putExtra("Radius", radius)
                })
                finish()
            }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()

    }

}