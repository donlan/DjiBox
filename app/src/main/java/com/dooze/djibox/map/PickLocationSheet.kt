package com.dooze.djibox.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.MyLocationStyle
import com.dooze.djibox.R
import com.dooze.djibox.databinding.SheetMapPickLocationBinding
import com.dooze.djibox.extensions.makeVibrate
import com.dooze.djibox.point
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pdb.app.base.extensions.dp
import pdb.app.base.extensions.roundedCorner
import pdb.app.base.extensions.topRoundedCorner

/**
 * @author 梁桂栋
 * @date 2022/8/23  21:54.
 * e-mail 760625325@qq.com
 * GitHub: https://github.com/donlan
 * description: com.dooze.djibox.map
 * @version 1.0
 */

class PickLocationSheet : BottomSheetDialogFragment() {

    private var binding: SheetMapPickLocationBinding? = null

    private var locationClient: AMapLocationClient? = null

    private var listener: ((point: LatLng) -> Unit)? = null

    private var attachDialog = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return SheetMapPickLocationBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        attachDialog = arguments?.getBoolean("AttachDialog") == true
        val binding = binding!!
        binding.ivClose.roundedCorner(-1)
        binding.root.topRoundedCorner(16.dp(view.context))
        binding.ivClose.setOnClickListener {
            close()
        }
        binding.ivMyLocation.setOnClickListener {
            locationClient?.lastKnownLocation?.let {
                zoomTo(it.point, 17f)
            }
        }
        binding.mapView.onCreate(savedInstanceState)

        binding.mapView.map.setOnMapLongClickListener {
            binding.mapView.map.addMarker(MarkerOptions().apply {
                position(it)
                val drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_loc, null)!!
                drawable.setTint(ContextCompat.getColor(requireContext(), R.color.alert))
                this.icon(BitmapDescriptorFactory.fromBitmap(drawable.toBitmap()))
            })
            makeVibrate()
            lifecycleScope.launch {
                delay(300)
                listener?.invoke(it)
                close()
            }
        }
        binding.mapView.map.myLocationStyle = MyLocationStyle().apply {
            this.showMyLocation(true)
            myLocationType(MyLocationStyle.LOCATION_TYPE_SHOW)
        }

        AMapLocationClient.updatePrivacyShow(requireContext(), true, true)
        AMapLocationClient.updatePrivacyAgree(requireContext(), true)

        binding.mapView.map.isMyLocationEnabled = true
        val locationClient = AMapLocationClient(requireContext())


        this.locationClient = locationClient
        locationClient.setLocationOption(AMapLocationClientOption().apply {
            locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
        })
        var firstLocation = true
        locationClient.setLocationListener {
            binding.mapView.map.myLocation.set(it)
            if (firstLocation) {
                zoomTo(it.point)
            }
            firstLocation = false
        }
        locationClient.startLocation()


        locationClient.lastKnownLocation?.let {
            zoomTo(it.point, 17f)
        }
    }

    fun close() {
        locationClient?.onDestroy()
        runCatching {
            binding?.mapView?.onDestroy()
        }
        locationClient = null
        listener = null
        if (attachDialog) {
            dismissAllowingStateLoss()
            return
        }
        parentFragmentManager.beginTransaction()
            .hide(this)
            .commit()
    }

    private fun zoomTo(point: LatLng, zoom: Float = 17f) {
        binding?.mapView?.map?.moveCamera(
            CameraUpdateFactory.newLatLngZoom(point, zoom)
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding?.mapView?.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        binding?.mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding?.mapView?.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
//        locationClient?.onDestroy()
//        runCatching {
//            binding?.mapView?.onDestroy()
//        }
//        listener = null
    }

    override fun onDestroy() {
        super.onDestroy()
    }


    companion object {

        fun newInstance(
            attachDialog: Boolean = false,
            listener: (point: LatLng) -> Unit
        ): PickLocationSheet {
            val sheet = PickLocationSheet()
            sheet.listener = listener
            sheet.arguments = Bundle().apply {
                putBoolean("AttachDialog", attachDialog)
            }
            return sheet
        }

        fun show(fm: FragmentManager, listener: (point: LatLng) -> Unit) {
            val sheet = newInstance(true, listener)
            sheet.show(fm, PickLocationSheet::class.java.simpleName)
        }
    }


}