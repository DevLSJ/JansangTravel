package com.example

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.example.databinding.ActivityAddEditBinding
import com.example.db.DBHelper
import com.example.model.TravelItem
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditBinding
    private lateinit var dbHelper: DBHelper
    private var travelItem: TravelItem? = null

    private var selectedPhotoUri: Uri? = null
    private var tempCameraFileUri: Uri? = null
    private var tempCameraFile: File? = null

    // Date variables
    private val calendar = Calendar.getInstance()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Result contracts for image actions
    private val pickImageContract = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            processSelectedImage(uri)
        }
    }

    private val captureCameraContract = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempCameraFileUri?.let { uri ->
                processSelectedImage(uri)
            }
        } else {
            Toast.makeText(this, "카메라 촬영이 취소되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DBHelper(this)

        // Parse possible existing travel diary model
        travelItem = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("TRAVEL_ITEM", TravelItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("TRAVEL_ITEM") as? TravelItem
        }

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        if (travelItem != null) {
            // Edit Mode
            binding.tvTitle.text = "여행 기록 수정"
            binding.tvSubtitle.text = "지나간 아름다운 흔적을 수정합니다"
            binding.btnSave.text = "추억 수정하기"

            val item = travelItem!!
            binding.etPlace.setText(item.place)
            binding.tvSelectedDate.text = item.visitDate
            binding.etMemo.setText(item.memo)

            if (item.latitude != 0.0 || item.longitude != 0.0) {
                binding.etLatitude.setText(item.latitude.toString())
                binding.etLongitude.setText(item.longitude.toString())
            }

            if (!item.photoUri.isNullOrEmpty()) {
                selectedPhotoUri = Uri.parse(item.photoUri)
                displayImage(selectedPhotoUri)
            }
        } else {
            // Add Mode
            binding.tvTitle.text = "여행 기록 남기기"
            binding.tvSubtitle.text = "어디서 어떤 추억을 쌓으셨나요?"
            binding.btnSave.text = "추억 저장하기"

            // Default to today
            binding.tvSelectedDate.text = dateFormatter.format(calendar.time)

            // Suggest Seoul coordinates by default if empty
            binding.etLatitude.setText("37.5665")
            binding.etLongitude.setText("126.9780")
        }
    }

    private fun setupListeners() {
        // Date Picker Click
        binding.cardDatePicker.setOnClickListener {
            showDatePickerDialog()
        }

        // Card Photo Box Click
        binding.cardPhotoBox.setOnClickListener {
            showPhotoSourceDialog()
        }

        // Camera Click
        binding.btnCamera.setOnClickListener {
            launchCamera()
        }

        // Gallery Click
        binding.btnGallery.setOnClickListener {
            pickImageContract.launch("image/*")
        }

        // Select coordinates from Map
        binding.btnSelectFromMap.setOnClickListener {
            showMapSelectorDialog()
        }

        // Save Click
        binding.btnSave.setOnClickListener {
            saveDiary()
        }

        // Cancel/Back Click
        binding.btnCancel.setOnClickListener {
            finish()
        }

        // Click on the title label focuses the input field
        binding.tvMemoLabel.setOnClickListener {
            binding.etMemo.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.showSoftInput(binding.etMemo, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }

        // Automatic scrolling when focus or click occurs on the description edit text
        // This ensures the soft keyboard does not block the active editing experience
        binding.scrollViewAddEdit.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (oldBottom > 0 && bottom < oldBottom) {
                // Keyboard is opened, dynamically size and show spacer
                val keyboardHeight = oldBottom - bottom
                binding.keyboardSpacer.layoutParams.height = keyboardHeight
                binding.keyboardSpacer.visibility = View.VISIBLE

                if (binding.etMemo.hasFocus()) {
                    binding.scrollViewAddEdit.postDelayed({
                        binding.scrollViewAddEdit.smoothScrollTo(0, binding.inputLayoutMemo.top)
                    }, 100)
                }
            } else if (oldBottom > 0 && bottom > oldBottom) {
                // Keyboard is closed, retract spacer
                binding.keyboardSpacer.visibility = View.GONE
            }
        }

        binding.etMemo.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.scrollViewAddEdit.postDelayed({
                    binding.scrollViewAddEdit.smoothScrollTo(0, binding.inputLayoutMemo.top)
                }, 150)
            }
        }
        binding.etMemo.setOnClickListener {
            binding.scrollViewAddEdit.postDelayed({
                binding.scrollViewAddEdit.smoothScrollTo(0, binding.inputLayoutMemo.top)
            }, 150)
        }

        binding.etPlace.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.scrollViewAddEdit.postDelayed({
                    binding.scrollViewAddEdit.smoothScrollTo(0, binding.inputLayoutPlace.top)
                }, 150)
            }
        }

        binding.etLatitude.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.scrollViewAddEdit.postDelayed({
                    binding.scrollViewAddEdit.smoothScrollTo(0, binding.cardGpsBox.top)
                }, 150)
            }
        }

        binding.etLongitude.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.scrollViewAddEdit.postDelayed({
                    binding.scrollViewAddEdit.smoothScrollTo(0, binding.cardGpsBox.top)
                }, 150)
            }
        }
    }

    private fun showPhotoSourceDialog() {
        val options = arrayOf("카메라로 촬영", "갤러리에서 선택")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("대표 사진 선택")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> launchCamera()
                    1 -> pickImageContract.launch("image/*")
                }
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showDatePickerDialog() {
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                binding.tvSelectedDate.text = dateFormatter.format(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun launchCamera() {
        try {
            tempCameraFile = File(externalCacheDir, "temp_photo_${System.currentTimeMillis()}.jpg")
            val authority = "$packageName.fileprovider"
            tempCameraFileUri = FileProvider.getUriForFile(this, authority, tempCameraFile!!)
            captureCameraContract.launch(tempCameraFileUri!!)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "카메라 실행 중 요류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processSelectedImage(uri: Uri) {
        binding.progressBarLoadImage.visibility = View.VISIBLE
        binding.layPhotoPlaceholder.visibility = View.GONE

        // Run heavy copy and EXIF parsing in Coroutines background IO Thread
        CoroutineScope(Dispatchers.Main).launch {
            val localSavedUri = withContext(Dispatchers.IO) {
                copyUriToInternalStorage(this@AddEditActivity, uri)
            }

            if (localSavedUri != null) {
                selectedPhotoUri = localSavedUri
                displayImage(localSavedUri)

                // Try to extract GPS EXIF info
                val gpsInfo = withContext(Dispatchers.IO) {
                    try {
                        contentResolver.openInputStream(uri)?.use { inputStream ->
                            val exifInterface = ExifInterface(inputStream)
                            val latLong = exifInterface.latLong
                            if (latLong != null && latLong.size >= 2) {
                                Pair(latLong[0], latLong[1])
                            } else {
                                null
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }

                binding.progressBarLoadImage.visibility = View.GONE
                if (gpsInfo != null) {
                    binding.etLatitude.setText(gpsInfo.first.toString())
                    binding.etLongitude.setText(gpsInfo.second.toString())
                    Toast.makeText(this@AddEditActivity, "사진의 EXIF 위치 데이터를 성공적으로 추출했습니다!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@AddEditActivity, "사진에 GPS 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                }
            } else {
                binding.progressBarLoadImage.visibility = View.GONE
                Toast.makeText(this@AddEditActivity, "이미지 처리에 실패하였습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun copyUriToInternalStorage(context: Context, sourceUri: Uri): Uri? {
        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            val destFile = File(context.filesDir, "travel_photo_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(destFile)

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(destFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun displayImage(uri: Uri?) {
        if (uri == null) return
        binding.layPhotoPlaceholder.visibility = View.GONE

        // Use standard coroutines to load bitmap asynchronously
        CoroutineScope(Dispatchers.Main).launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 2 // Optimize memory using downsampling
                    }
                    val bmp = BitmapFactory.decodeStream(inputStream, null, options)
                    inputStream?.close()
                    bmp
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            if (bitmap != null) {
                binding.ivAddPhoto.setImageBitmap(bitmap)
            } else {
                binding.ivAddPhoto.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }
    }

    private fun saveDiary() {
        val place = binding.etPlace.text.toString().trim()
        val visitDate = binding.tvSelectedDate.text.toString()
        val memo = binding.etMemo.text.toString().trim()
        val latString = binding.etLatitude.text.toString().trim()
        val lngString = binding.etLongitude.text.toString().trim()

        if (place.isEmpty()) {
            binding.inputLayoutPlace.error = "여행지 이름을 입력해주세요"
            return
        } else {
            binding.inputLayoutPlace.error = null
        }

        val latitude = latString.toDoubleOrNull() ?: 0.0
        val longitude = lngString.toDoubleOrNull() ?: 0.0
        val photoPath = selectedPhotoUri?.toString() ?: ""

        val success: Boolean
        if (travelItem != null) {
            // Update
            val rows = dbHelper.updateTravel(
                no = travelItem!!.no,
                place = place,
                visitDate = visitDate,
                memo = memo,
                photoUri = photoPath,
                latitude = latitude,
                longitude = longitude
            )
            success = rows > 0
        } else {
            // Create
            val id = dbHelper.insertTravel(
                place = place,
                visitDate = visitDate,
                memo = memo,
                photoUri = photoPath,
                latitude = latitude,
                longitude = longitude
            )
            success = id != -1L
        }

        if (success) {
            Toast.makeText(this, "성공적으로 저장되었습니다.", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "저장 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMapSelectorDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_map_picker, null)
        val mapView = dialogView.findViewById<com.google.android.gms.maps.MapView>(R.id.dialogMapView)
        val tvTappedCoords = dialogView.findViewById<android.widget.TextView>(R.id.tvTappedCoords)

        // Parse current latitude and longitude. Default to Seoul if empty
        val currentLatStr = binding.etLatitude.text.toString().trim()
        val currentLngStr = binding.etLongitude.text.toString().trim()
        val currentLat = currentLatStr.toDoubleOrNull() ?: 37.5665
        val currentLng = currentLngStr.toDoubleOrNull() ?: 126.9780

        var selectedLatLng = LatLng(currentLat, currentLng)

        // Initialize MapView with null Bundle representing simple instanced State
        mapView.onCreate(null)
        mapView.onResume()

        mapView.getMapAsync { googleMap ->
            googleMap.uiSettings.isZoomControlsEnabled = true
            
            // Add initial marker & move camera
            var marker = googleMap.addMarker(
                MarkerOptions()
                    .position(selectedLatLng)
                    .title("선택된 위치")
            )
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 15.0f))
            
            // Format coordinates
            tvTappedCoords.text = String.format(Locale.getDefault(), "선택된 좌표: %.5f, %.5f", selectedLatLng.latitude, selectedLatLng.longitude)

            // Map click listener
            googleMap.setOnMapClickListener { latLng ->
                selectedLatLng = latLng
                marker?.remove()
                marker = googleMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title("선택된 위치")
                )
                tvTappedCoords.text = String.format(Locale.getDefault(), "선택된 좌표: %.5f, %.5f", latLng.latitude, latLng.longitude)
            }
        }

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("선택 완료") { dialog, _ ->
                binding.etLatitude.setText(String.format(Locale.getDefault(), "%.5f", selectedLatLng.latitude))
                binding.etLongitude.setText(String.format(Locale.getDefault(), "%.5f", selectedLatLng.longitude))
                mapView.onPause()
                mapView.onDestroy()
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ ->
                mapView.onPause()
                mapView.onDestroy()
                dialog.dismiss()
            }

        val alertDialog = builder.create()
        alertDialog.setOnDismissListener {
            // Ensure mapView lifecycle is terminated properly
            try {
                mapView.onPause()
                mapView.onDestroy()
            } catch (e: Exception) {
                // Ignore
            }
        }
        alertDialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up temp camera files if any
        try {
            tempCameraFile?.let {
                if (it.exists()) it.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
