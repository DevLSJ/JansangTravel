package com.example.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.databinding.FragmentTravelMapBinding
import com.example.db.DBHelper
import com.example.model.TravelItem
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TravelMapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentTravelMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: DBHelper
    private var googleMap: GoogleMap? = null
    private var travelItems: List<TravelItem> = emptyList()

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
        dbHelper = DBHelper(requireContext())

        // Initialize MapView
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

        loadMapData()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // Configure Map Settings
        map.uiSettings.isZoomControlsEnabled = true
        
        // Populate markers if data is already loaded, or it will be populated in loadMapData()
        displayMarkers()
    }

    fun loadMapData() {
        if (_binding == null) return
        binding.progressBarMap.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            val travels = withContext(Dispatchers.IO) {
                dbHelper.getAllTravels()
            }

            if (_binding == null) return@launch

            val totalCount = travels.size
            val listWithGps = travels.filter { it.latitude != 0.0 && it.longitude != 0.0 }
            val mappedCount = listWithGps.size

            // Update Statistics
            binding.tvTotalTrips.text = totalCount.toString()
            binding.tvMappedTrips.text = mappedCount.toString()

            travelItems = listWithGps

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

    private fun displayMarkers() {
        val map = googleMap ?: return
        if (travelItems.isEmpty()) {
            binding.progressBarMap.visibility = View.GONE
            return
        }

        map.clear()
        val builder = LatLngBounds.Builder()
        var hasPoints = false

        for (item in travelItems) {
            val position = LatLng(item.latitude, item.longitude)
            map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(item.place)
                    .snippet("${item.visitDate}: ${item.memo}")
            )
            builder.include(position)
            hasPoints = true
        }

        if (hasPoints) {
            try {
                val bounds = builder.build()
                // Use a standard padding
                val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 100)
                map.animateCamera(cameraUpdate)
            } catch (e: Exception) {
                // If maps layout isn't fully completed yet, center on first item after a delay or immediately
                val first = travelItems.first()
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(first.latitude, first.longitude), 10.0f))
            }
        } else {
            // Default to Seoul if no valid location exists
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
