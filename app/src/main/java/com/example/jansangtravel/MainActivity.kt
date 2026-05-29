package com.example.jansangtravel

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.CancellationTokenSource

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fused: FusedLocationProviderClient
    private var myMarker: Marker? = null

    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                enableMyLocationAndMove()
            } else {
                moveToDefaultLocation()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fused = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true

        if (hasFineLocationPermission()) {
            enableMyLocationAndMove()
        } else {
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocationAndMove() {
        if (!hasFineLocationPermission()) return

        map.isMyLocationEnabled = true

        val cts = CancellationTokenSource()

        fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    val here = LatLng(loc.latitude, loc.longitude)
                    showMarkerAndMove(here, "현재 위치")
                } else {
                    fallbackToLastLocation()
                }
            }
            .addOnFailureListener {
                fallbackToLastLocation()
            }
    }

    @SuppressLint("MissingPermission")
    private fun fallbackToLastLocation() {
        fused.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                val here = LatLng(loc.latitude, loc.longitude)
                showMarkerAndMove(here, "현재 위치(최근 기록)")
            } else {
                moveToDefaultLocation()
            }
        }.addOnFailureListener {
            moveToDefaultLocation()
        }
    }

    private fun showMarkerAndMove(latLng: LatLng, title: String) {
        myMarker?.remove()
        myMarker = map.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(title)
        )

        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
    }

    private fun moveToDefaultLocation() {
        val seoulCityHall = LatLng(37.5665, 126.9780)

        myMarker?.remove()
        myMarker = map.addMarker(
            MarkerOptions()
                .position(seoulCityHall)
                .title("기본 위치(권한/위치 미확인)")
        )

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(seoulCityHall, 12f))
    }
}
