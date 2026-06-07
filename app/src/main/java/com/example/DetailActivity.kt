package com.example

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.databinding.ActivityDetailBinding
import com.example.db.DBHelper
import com.example.model.TravelItem
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private lateinit var dbHelper: DBHelper
    private var travelNo: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DBHelper(this)

        val item = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("TRAVEL_ITEM", TravelItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("TRAVEL_ITEM") as? TravelItem
        }
        if (item != null) {
            travelNo = item.no
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
            // Fetch latest item to edit
            val latestItem = dbHelper.getTravelById(travelNo)
            if (latestItem != null) {
                val intent = Intent(this, AddEditActivity::class.java).apply {
                    putExtra("TRAVEL_ITEM", latestItem)
                }
                startActivity(intent)
            }
        }
    }

    private fun loadData() {
        val item = dbHelper.getTravelById(travelNo)
        if (item == null) {
            finish()
            return
        }

        binding.tvDetailPlace.text = item.place
        binding.tvDetailDate.text = item.visitDate
        binding.tvDetailMemo.text = item.memo

        // Load photo asynchronously
        loadPhotoAsync(item.photoUri)

        // Setup mini map based on coordinates
        if (item.latitude != 0.0 || item.longitude != 0.0) {
            binding.layDetailMapBox.visibility = View.VISIBLE
            binding.tvDetailCoords.text = String.format("위도(Lat): %.6f, 경도(Lng): %.6f", item.latitude, item.longitude)
            setupMiniMap(item)
        } else {
            binding.layDetailMapBox.visibility = View.GONE
        }
    }

    private fun loadPhotoAsync(photoUriStr: String?) {
        binding.progressBarDetailImage.visibility = View.VISIBLE
        binding.ivDetailPhoto.setImageBitmap(null)

        if (photoUriStr.isNullOrEmpty()) {
            binding.ivDetailPhoto.setImageResource(android.R.drawable.ic_menu_gallery)
            binding.progressBarDetailImage.visibility = View.GONE
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val uri = Uri.parse(photoUriStr)
                    val inputStream = contentResolver.openInputStream(uri)
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 2 // balance memory & quality
                    }
                    val bmp = BitmapFactory.decodeStream(inputStream, null, options)
                    inputStream?.close()
                    bmp
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                    null
                }
            }

            binding.progressBarDetailImage.visibility = View.GONE
            if (bitmap != null) {
                binding.ivDetailPhoto.setImageBitmap(bitmap)
            } else {
                binding.ivDetailPhoto.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }
    }

    private fun setupMiniMap(item: TravelItem) {
        binding.mapDetailView.getMapAsync { map ->
            binding.progressBarDetailMap.visibility = View.GONE
            val position = LatLng(item.latitude, item.longitude)
            map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(item.place)
                    .snippet(item.memo)
            )
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 14.0f))
        }
    }

    override fun onStart() {
        super.onStart()
        binding.mapDetailView.onStart()
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
