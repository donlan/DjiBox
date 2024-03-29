package com.dooze.djibox.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import android.widget.SeekBar
import com.dooze.djibox.R
import com.dooze.djibox.databinding.ViewSeekBarTextBinding
import pdb.app.base.extensions.safeUse

/**
 * @author 梁桂栋
 * @date 2022/8/31  22:55.
 * e-mail 760625325@qq.com
 * GitHub: https://github.com/donlan
 * description: com.dooze.djibox.widgets
 * @version 1.0
 */

class SeekbarText @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null,
    defStyle: Int = com.google.android.material.R.attr.seekBarStyle
) : RelativeLayout(context, attr, defStyle) {

    private val binding: ViewSeekBarTextBinding

    private var min = 0
    private var max = 0
    private var step = 1.0f

    var onProgressChanged: ((progress: Float) -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_seek_bar_text, this, true)
        binding = ViewSeekBarTextBinding.bind(this)
        context.obtainStyledAttributes(attr, R.styleable.SeekbarText).safeUse { ta ->
            binding.tvTitle.text = ta.getString(R.styleable.SeekbarText_sbtTitle)
            setup(
                ta.getInt(R.styleable.SeekbarText_sbtMin, min),
                ta.getInt(R.styleable.SeekbarText_sbtMax, max),
                ta.getFloat(R.styleable.SeekbarText_sbtStep, step)
            )

            val p = ta.getInt(R.styleable.SeekbarText_sbtProgress, 0)
            binding.speedSeekBar.progress =
                ((max - min) * (p / binding.speedSeekBar.max.toFloat()) + min).toInt()
        }

        binding.speedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar, p1: Int, p2: Boolean) {
                val progress = (max - min) * (p1 / p0.max.toFloat())
                if (onProgressChanged == null) {
                    setValuesText((progress + min).toString())
                    return
                }
                onProgressChanged?.invoke(progress + min)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

        })

        setValuesText(progress.toString())

    }

    fun setup(min: Int, max: Int, step: Float = 1f) {
        this.min = min
        this.max = max
        this.step = step
        binding.speedSeekBar.max = ((max - min) / step).toInt()
        onProgressChanged?.invoke(progress)
    }


    var progress: Float
        get() = (max - min) * (binding.speedSeekBar.progress / binding.speedSeekBar.max.toFloat()) + min
        set(value) {
            binding.speedSeekBar.progress = (value - min).toInt()
        }

    fun setTitle(content:String) {
        binding.tvTitle.text = content
    }

    fun setValuesText(value: String) {
        binding.tvValue.text = value
    }
}