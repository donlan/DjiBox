package com.dooze.djibox.map

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.*
import com.dooze.djibox.R
import com.dooze.djibox.databinding.SheetMapPickLocationBinding
import com.dooze.djibox.extensions.makeVibrate
import com.dooze.djibox.point
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pdb.app.base.extensions.*

/**
 * @author 梁桂栋
 * @date 2022/8/23  21:54.
 * e-mail 760625325@qq.com
 * GitHub: https://github.com/donlan
 * description: com.dooze.djibox.map
 * @version 1.0
 */

class PickLocationSheet : BottomSheetDialogFragment(), View.OnClickListener {

    private var binding: SheetMapPickLocationBinding? = null

    private var locationClient: AMapLocationClient? = null

    private var listener: ((point: LatLng?, radius: Double?) -> Unit)? = null

    private var attachType: Int = ATTACH_SHEET

    private var pickedMarker: Marker? = null
    private var radiusCircle: Circle? = null

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
        attachType = arguments?.getInt("AttachType") ?: ATTACH_SHEET
        val binding = binding!!
        binding.ivRadius.isEnable = false
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
        binding.ivRadius.setOnClickListener(this)
        binding.mapView.onCreate(savedInstanceState)


        binding.radiusSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.tvRadiusHint.text = getString(
                    R.string.map_pick_point_radius_progress,
                    (progress + 10).toString()
                )
                val marker = pickedMarker ?: return
                val radius = (seekBar.progress + 10).toDouble()
                val circle = radiusCircle ?: binding.mapView.map.addCircle(CircleOptions().apply {
                    center(marker.position)
                    radius(radius)
                    fillColor(ContextCompat.getColor(requireContext(), R.color.map_radius_fill))
                    strokeWidth(1.dp(requireContext()))
                    strokeColor(ContextCompat.getColor(requireContext(), R.color.map_radius_stoker))
                }).also {
                    radiusCircle = it
                }
                circle.center = marker.position
                circle.radius = radius
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }

        })

        binding.mapView.map.setOnMapLongClickListener {
            pickedMarker = binding.mapView.map.addMarker(MarkerOptions().apply {
                position(it)
                val drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_loc, null)!!
                drawable.setTint(ContextCompat.getColor(requireContext(), R.color.alert))
                this.icon(BitmapDescriptorFactory.fromBitmap(drawable.toBitmap()))
            })
            binding.ivRadius.isEnable = true
            binding.layoutRadiusSeek.isVisible = true
            binding.tvRadiusHint.text = getString(
                R.string.map_pick_point_radius_progress,
                (binding.radiusSeekBar.progress + 10).toString()
            )
            makeVibrate()
        }
        binding.mapView.map.myLocationStyle = MyLocationStyle().apply {
            this.showMyLocation(true)
            radiusFillColor(Color.TRANSPARENT)
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

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.ivRadius -> {
                binding?.layoutRadiusSeek?.toggleVisible()
            }
        }
    }


    private fun close() {
        if (attachType == ATTACH_OTHER_ACTIVITY) {
            listener?.invoke(pickedMarker?.position, radiusCircle?.radius)
            return
        }
        locationClient?.onDestroy()
        runCatching {
            binding?.mapView?.onDestroy()
        }
        locationClient = null
        listener = null
        if (attachType == ATTACH_CUR_ACTIVITY) {
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

    }

    override fun onDestroy() {
        super.onDestroy()
        locationClient?.onDestroy()
        listener = null
    }


    companion object {

        const val ATTACH_SHEET = 0
        const val ATTACH_CUR_ACTIVITY = 1
        const val ATTACH_OTHER_ACTIVITY = 2

        fun newInstance(
            attachType: Int = ATTACH_SHEET,
            listener: (point: LatLng?, radius: Double?) -> Unit
        ): PickLocationSheet {
            val sheet = PickLocationSheet()
            sheet.listener = listener
            sheet.arguments = Bundle().apply {
                putInt("AttachType", attachType)
            }
            return sheet
        }

        fun show(fm: FragmentManager, listener: (point: LatLng?, radius: Double?) -> Unit) {
            val sheet = newInstance(ATTACH_SHEET, listener)
            sheet.show(fm, PickLocationSheet::class.java.simpleName)
        }
    }


}