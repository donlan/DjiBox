package com.dooze.djibox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.dooze.djibox.databinding.FragmentMediaManagerBinding

/**
 * @author 梁桂栋
 * @date 2022/9/4  14:04.
 * e-mail 760625325@qq.com
 * GitHub: https://github.com/donlan
 * description: com.dooze.djibox
 * @version 1.0
 */
class MediaManagerFragment : Fragment(R.layout.fragment_media_manager), View.OnClickListener {


    private var binding:FragmentMediaManagerBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return FragmentMediaManagerBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding!!) {
            ivClose.setOnClickListener(this@MediaManagerFragment)
        }
    }

    override fun onClick(p0: View?) {
        when(p0?.id) {
            R.id.ivClose -> {
                parentFragmentManager.beginTransaction()
                    .remove(this)
                    .commitAllowingStateLoss()
            }
        }
    }

}