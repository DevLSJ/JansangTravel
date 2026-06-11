package com.example.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.databinding.FragmentTravelMapBinding
import com.example.db.RecordEntity
import com.example.db.RecordViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TravelMapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentTravelMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: RecordViewModel
    private var googleMap: GoogleMap? = null
    private var travelItems: List<RecordEntity> = emptyList()
    private var isMapInitError = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTravelMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Scope ViewModel to requireActivity() so that we share memory seamlessly
        viewModel = ViewModelProvider(requireActivity())[RecordViewModel::class.java]

        checkApiKeyAndInitMap(savedInstanceState)

        observeViewModel()
    }

    private fun checkApiKeyAndInitMap(savedInstanceState: Bundle?) {
        try {
            val appInfo = requireContext().packageManager.getApplicationInfo(
                requireContext().packageName,
                android.content.pm.PackageManager.GET_META_DATA
            )
            val apiKey = appInfo.metaData?.getString("com.google.android.geo.API_KEY")
            
            if (apiKey.isNullOrEmpty() || apiKey == "placeholder_key" || apiKey.contains("placeholder")) {
                showApiKeyWarning()
                return
            }
            
            binding.mapView.onCreate(savedInstanceState)
            binding.mapView.getMapAsync(this)
        } catch (e: Exception) {
            e.printStackTrace()
            showApiKeyWarning()
        }
    }

    private fun showApiKeyWarning() {
        isMapInitError = true
        binding.mapView.visibility = View.GONE
        binding.progressBarMap.visibility = View.GONE
        binding.cardRecentOverlay.visibility = View.GONE
        binding.tvMapEmptyState.visibility = View.GONE
        binding.tvApiKeyWarning.visibility = View.VISIBLE
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // Configure Map Settings
        map.uiSettings.isZoomControlsEnabled = true
        
        map.setOnInfoWindowClickListener { marker ->
            val recordId = marker.tag as? Long ?: return@setOnInfoWindowClickListener
            viewModel.getRecordById(recordId) { record ->
                if (record != null && isAdded) {
                    val intent = android.content.Intent(requireContext(), com.example.DetailActivity::class.java).apply {
                        putExtra("TRAVEL_ITEM", record)
                    }
                    startActivity(intent)
                }
            }
        }
        
        // Display markers immediately if we already have records
        displayMarkers()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.records.collectLatest { list ->
                if (_binding == null) return@collectLatest

                val totalCount = list.size
                val listWithGps = list.filter { (it.latitude ?: 0.0) != 0.0 && (it.longitude ?: 0.0) != 0.0 }
                val mappedCount = listWithGps.size

                // Update Statistics
                binding.tvTotalTrips.text = totalCount.toString()
                binding.tvMappedTrips.text = mappedCount.toString()

                travelItems = listWithGps

                val recentItem = listWithGps.firstOrNull()
                if (recentItem != null) {
                    binding.cardRecentOverlay.visibility = View.VISIBLE
                    binding.tvRecentPlaceName.text = recentItem.title
                    
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    binding.tvRecentPlaceDate.text = sdf.format(Date(recentItem.createdAt))

                    val lat = recentItem.latitude ?: 0.0
                    val lng = recentItem.longitude ?: 0.0
                    binding.btnFocusRecent.setOnClickListener {
                        focusOnLatLng(LatLng(lat, lng))
                    }
                    binding.btnShowOnMap.setOnClickListener {
                        focusOnLatLng(LatLng(lat, lng))
                    }
                } else {
                    binding.cardRecentOverlay.visibility = View.GONE
                }

                if (totalCount == 0) {
                    binding.tvMapEmptyState.visibility = View.VISIBLE
                    binding.progressBarMap.visibility = View.GONE
                    binding.mapView.visibility = View.GONE
                } else {
                    binding.tvMapEmptyState.visibility = View.GONE
                    binding.mapView.visibility = View.VISIBLE
                    displayMarkers()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { loading ->
                if (_binding == null) return@collectLatest
                if (loading) {
                    binding.progressBarMap.visibility = View.VISIBLE
                } else {
                    binding.progressBarMap.visibility = View.GONE
                }
            }
        }
    }

    fun loadMapData() {
        viewModel.loadRecords()
    }

    private fun focusOnLatLng(latLng: LatLng) {
        val map = googleMap ?: return
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14.0f))
    }

    private fun displayMarkers() {
        val map = googleMap ?: return
        if (travelItems.isEmpty()) {
            binding.progressBarMap.visibility = View.GONE
            return
        }

        map.clear()
        val builder = LatLngBounds.Builder()
        var hasPoints = false
        var recentMarker: com.google.android.gms.maps.model.Marker? = null
        val recentItem = travelItems.firstOrNull()

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (item in travelItems) {
            val lat = item.latitude ?: continue
            val lng = item.longitude ?: continue
            val position = LatLng(lat, lng)
            val dateStr = sdf.format(Date(item.createdAt))
            
            val marker = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(item.title)
                    .snippet("$dateStr: ${item.memo}")
            )
            marker?.tag = item.id
            if (item == recentItem) {
                recentMarker = marker
            }
            builder.include(position)
            hasPoints = true
        }

        if (hasPoints && recentItem != null) {
            val recentLatLng = LatLng(recentItem.latitude ?: 0.0, recentItem.longitude ?: 0.0)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(recentLatLng, 13.0f))
            recentMarker?.showInfoWindow()
        } else if (hasPoints) {
            try {
                val bounds = builder.build()
                val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 100)
                map.animateCamera(cameraUpdate)
            } catch (e: Exception) {
                val first = travelItems.first()
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(first.latitude ?: 37.5665, first.longitude ?: 126.9780), 10.0f))
            }
        } else {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(37.5665, 126.9780), 7.0f))
        }
        
        binding.progressBarMap.visibility = View.GONE
    }

    override fun onStart() {
        super.onStart()
        if (_binding != null) {
            binding.mapView.onStart()
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            binding.mapView.onResume()
            loadMapData()
        }
    }

    override fun onPause() {
        if (_binding != null) {
            binding.mapView.onPause()
        }
        super.onPause()
    }

    override fun onStop() {
        if (_binding != null) {
            binding.mapView.onStop()
        }
        super.onStop()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        if (_binding != null) {
            binding.mapView.onDestroy()
        }
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (_binding != null) {
            binding.mapView.onLowMemory()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (_binding != null) {
            binding.mapView.onSaveInstanceState(outState)
        }
    }
}
