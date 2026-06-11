package com.example

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.databinding.ActivityDetailBinding
import com.example.db.RecordEntity
import com.example.db.RecordViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private lateinit var viewModel: RecordViewModel
    private var travelNo: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[RecordViewModel::class.java]

        val item = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("TRAVEL_ITEM", RecordEntity::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("TRAVEL_ITEM") as? RecordEntity
        }
        if (item != null) {
            travelNo = item.id
        } else {
            finish()
            return
        }

        binding.mapDetailView.onCreate(savedInstanceState)
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnEdit.setOnClickListener {
            viewModel.getRecordById(travelNo) { latestItem ->
                if (latestItem != null) {
                    val intent = Intent(this, AddEditActivity::class.java).apply {
                        putExtra("TRAVEL_ITEM", latestItem)
                    }
                    startActivity(intent)
                }
            }
        }
    }

    private fun loadData() {
        viewModel.getRecordById(travelNo) { item ->
            if (item == null) {
                finish()
                return@getRecordById
            }

            binding.tvDetailPlace.text = item.title
            
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            binding.tvDetailDate.text = sdf.format(Date(item.createdAt))
            binding.tvDetailMemo.text = item.memo

            // Load photo asynchronously using the RecordEntity
            loadPhotoAsync(item)

            // Setup mini map based on coordinates
            val lat = item.latitude ?: 0.0
            val lng = item.longitude ?: 0.0
            if (lat != 0.0 || lng != 0.0) {
                binding.layDetailMapBox.visibility = View.VISIBLE
                binding.tvDetailCoords.text = String.format("위도(Lat): %.6f, 경도(Lng): %.6f", lat, lng)
                setupMiniMap(item)
            } else {
                binding.tvDetailCoords.text = "GPS 정보 없음"
                binding.layDetailMapBox.visibility = View.GONE
            }
        }
    }

    private fun loadPhotoAsync(item: RecordEntity) {
        com.example.util.RecordImageLoader.load(
            context = this,
            imageView = binding.ivDetailPhoto,
            progressBar = binding.progressBarDetailImage,
            record = item,
            scope = CoroutineScope(Dispatchers.Main)
        )
    }

    private fun setupMiniMap(item: RecordEntity) {
        binding.mapDetailView.getMapAsync { map ->
            binding.progressBarDetailMap.visibility = View.GONE
            val lat = item.latitude ?: 0.0
            val lng = item.longitude ?: 0.0
            val position = LatLng(lat, lng)
            map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(item.title)
                    .snippet(item.memo)
            )
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 14.0f))
        }
    }

    override fun onStart() {
        super.onStart()
        binding.mapDetailView.onStart()
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        loadData()
    }

    override fun onPause() {
        binding.mapDetailView.onPause()
        super.onPause()
    }

    override fun onStop() {
        binding.mapDetailView.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        binding.mapDetailView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapDetailView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapDetailView.onSaveInstanceState(outState)
    }
}
