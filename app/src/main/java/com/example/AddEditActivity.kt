package com.example

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.databinding.ActivityAddEditBinding
import com.example.db.RecordEntity
import com.example.db.RecordViewModel
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
import java.util.Date
import java.util.Locale

class AddEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditBinding
    private lateinit var viewModel: RecordViewModel
    private var travelItem: RecordEntity? = null

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

    private val cameraPermissionContract = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            openCameraIntent()
        } else {
            Toast.makeText(this, "카메라 권한이 거부되어 촬영을 실행할 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[RecordViewModel::class.java]

        // Parse possible existing travel diary model (RecordEntity)
        travelItem = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("TRAVEL_ITEM", RecordEntity::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("TRAVEL_ITEM") as? RecordEntity
        }

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        if (travelItem != null) {
            // Edit Mode
            binding.tvTitle.text = "기록 수정"
            binding.tvSubtitle.text = "지나간 아름다운 흔적을 수정합니다"
            binding.btnSave.text = "추억 수정하기"
            binding.btnDeleteRecord.visibility = View.VISIBLE

            val item = travelItem!!
            binding.etPlace.setText(item.title)
            
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            binding.tvSelectedDate.text = sdf.format(Date(item.createdAt))
            binding.etMemo.setText(item.memo)

            val lat = item.latitude ?: 0.0
            val lng = item.longitude ?: 0.0
            if (lat != 0.0 || lng != 0.0) {
                binding.etLatitude.setText(lat.toString())
                binding.etLongitude.setText(lng.toString())
            }

            if (item.imageType == "DRAWABLE") {
                binding.layPhotoPlaceholder.visibility = View.GONE
                val cleanName = (item.imageRef ?: "").substringBeforeLast('.').trim().lowercase()
                var resolvedResId = resources.getIdentifier(cleanName, "drawable", packageName)
                if (resolvedResId == 0) {
                    resolvedResId = when {
                        cleanName.contains("hallasan") -> R.drawable.img_hallasan
                        cleanName.contains("gyeongbokgung") -> R.drawable.img_gyeongbokgung
                        cleanName.contains("haeundae") -> R.drawable.img_haeundae
                        else -> 0
                    }
                }
                if (resolvedResId != 0) {
                    binding.ivAddPhoto.setImageResource(resolvedResId)
                } else {
                    binding.ivAddPhoto.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            } else if (!item.imageUri.isNullOrEmpty()) {
                selectedPhotoUri = Uri.parse(item.imageUri)
                displayImage(selectedPhotoUri)
            }
        } else {
            // Add Mode
            binding.tvTitle.text = "여행 기록 남기기"
            binding.tvSubtitle.text = "어디서 어떤 추억을 쌓으셨나요?"
            binding.btnSave.text = "추억 저장하기"
            binding.btnDeleteRecord.visibility = View.GONE

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

        binding.btnDeleteRecord.setOnClickListener {
            showDeleteCurrentRecordDialog()
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
        binding.scrollViewAddEdit.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (oldBottom > 0 && bottom < oldBottom) {
                val keyboardHeight = oldBottom - bottom
                binding.keyboardSpacer.layoutParams.height = keyboardHeight
                binding.keyboardSpacer.visibility = View.VISIBLE

                if (binding.etMemo.hasFocus()) {
                    binding.scrollViewAddEdit.postDelayed({
                        binding.scrollViewAddEdit.smoothScrollTo(0, binding.inputLayoutMemo.top)
                    }, 100)
                }
            } else if (oldBottom > 0 && bottom > oldBottom) {
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionContract.launch(Manifest.permission.CAMERA)
            return
        }

        openCameraIntent()
    }

    private fun openCameraIntent() {
        try {
            tempCameraFile = File(externalCacheDir, "temp_photo_${System.currentTimeMillis()}.jpg")
            val authority = "$packageName.fileprovider"
            tempCameraFileUri = FileProvider.getUriForFile(this, authority, tempCameraFile!!)
            captureCameraContract.launch(tempCameraFileUri!!)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "카메라 실행 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processSelectedImage(uri: Uri) {
        binding.progressBarLoadImage.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false
        binding.layPhotoPlaceholder.visibility = View.GONE
        Toast.makeText(this, "사진 정보를 분석 중입니다.", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.Main).launch {
            val localSavedUri = withContext(Dispatchers.IO) {
                copyUriToInternalStorage(this@AddEditActivity, uri)
            }

            if (localSavedUri != null) {
                selectedPhotoUri = localSavedUri
                displayImage(localSavedUri)

                // Try to extract GPS EXIF info using ExifGpsExtractor
                val gpsInfo = com.example.util.ExifGpsExtractor.extractGpsFromImageUri(this@AddEditActivity, localSavedUri)

                binding.progressBarLoadImage.visibility = View.GONE
                binding.btnSave.isEnabled = true
                if (gpsInfo != null) {
                    binding.etLatitude.setText(String.format(Locale.US, "%.5f", gpsInfo.first))
                    binding.etLongitude.setText(String.format(Locale.US, "%.5f", gpsInfo.second))
                    Toast.makeText(this@AddEditActivity, "GPS 정보를 추출했습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    binding.etLatitude.setText("")
                    binding.etLongitude.setText("")
                    Toast.makeText(this@AddEditActivity, "GPS 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                }
            } else {
                binding.progressBarLoadImage.visibility = View.GONE
                binding.btnSave.isEnabled = true
                Toast.makeText(this@AddEditActivity, "이미지 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "제목을 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        } else {
            binding.inputLayoutPlace.error = null
        }

        // Check photo selection
        val imagePath = selectedPhotoUri?.toString() ?: ""
        val existingImagePath = travelItem?.imageUri ?: ""
        val finalImagePath = if (imagePath.isNotEmpty()) imagePath else existingImagePath
        val isDrawableFallback = travelItem?.imageType == "DRAWABLE"

        if (finalImagePath.isEmpty() && !isDrawableFallback) {
            Toast.makeText(this, "사진을 선택해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        val latitude = latString.toDoubleOrNull()
        val longitude = lngString.toDoubleOrNull()

        val parsedDate = try {
            dateFormatter.parse(visitDate) ?: Date()
        } catch (e: Exception) {
            Date()
        }
        val createdAt = parsedDate.time

        binding.progressBarLoadImage.visibility = View.VISIBLE

        if (travelItem != null) {
            // Update mode
            val updatedPhotoUri = if (selectedPhotoUri != null) imagePath else travelItem!!.imageUri
            val updatedImageType = if (selectedPhotoUri != null) "URI" else travelItem!!.imageType
            val updatedImageRef = if (selectedPhotoUri != null) imagePath else travelItem!!.imageRef

            val updatedRecord = RecordEntity(
                id = travelItem!!.id,
                title = place,
                memo = memo,
                imageUri = updatedPhotoUri,
                createdAt = createdAt,
                latitude = latitude,
                longitude = longitude,
                imageType = updatedImageType,
                imageRef = updatedImageRef
            )

            viewModel.updateRecord(updatedRecord) { rows ->
                binding.progressBarLoadImage.visibility = View.GONE
                if (rows > 0) {
                    Toast.makeText(this, "성공적으로 저장되었습니다.", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "저장 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // Add mode
            val newRecord = RecordEntity(
                title = place,
                memo = memo,
                imageUri = imagePath,
                createdAt = createdAt,
                latitude = latitude,
                longitude = longitude,
                imageType = "URI",
                imageRef = imagePath
            )

            viewModel.insertRecord(newRecord) { id ->
                binding.progressBarLoadImage.visibility = View.GONE
                if (id != -1L) {
                    Toast.makeText(this, "성공적으로 저장되었습니다.", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "저장 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showDeleteCurrentRecordDialog() {
        val item = travelItem ?: return
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("이 기록을 삭제할까요?")
            .setMessage("삭제한 기록은 복구할 수 없습니다.")
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("삭제") { dialog, _ ->
                viewModel.deleteRecord(item) { rows ->
                    if (rows > 0) {
                        Toast.makeText(this, "기록이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "삭제 처리에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun showMapSelectorDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_map_picker, null)
        val mapView = dialogView.findViewById<com.google.android.gms.maps.MapView>(R.id.dialogMapView)
        val tvTappedCoords = dialogView.findViewById<android.widget.TextView>(R.id.tvTappedCoords)

        val currentLatStr = binding.etLatitude.text.toString().trim()
        val currentLngStr = binding.etLongitude.text.toString().trim()
        val currentLat = currentLatStr.toDoubleOrNull() ?: 37.5665
        val currentLng = currentLngStr.toDoubleOrNull() ?: 126.9780

        var selectedLatLng = LatLng(currentLat, currentLng)

        mapView.onCreate(null)
        mapView.onResume()

        mapView.getMapAsync { googleMap ->
            googleMap.uiSettings.isZoomControlsEnabled = true
            
            var marker = googleMap.addMarker(
                MarkerOptions()
                    .position(selectedLatLng)
                    .title("선택된 위치")
            )
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 15.0f))
            
            tvTappedCoords.text = String.format(Locale.getDefault(), "선택된 좌표: %.5f, %.5f", selectedLatLng.latitude, selectedLatLng.longitude)

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
        try {
            tempCameraFile?.let {
                if (it.exists()) it.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
