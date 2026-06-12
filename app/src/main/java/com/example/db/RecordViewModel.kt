package com.example.db

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RecordViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RecordRepository
    private val preferences = application.getSharedPreferences("jansang_travel_prefs", Context.MODE_PRIVATE)

    private val _records = MutableStateFlow<List<RecordEntity>>(emptyList())
    val records: StateFlow<List<RecordEntity>> = _records

    private val _recordsWithGps = MutableStateFlow<List<RecordEntity>>(emptyList())
    val recordsWithGps: StateFlow<List<RecordEntity>> = _recordsWithGps

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var currentSortOrder = "DATE_DESC"

    init {
        repository = RecordRepository(TravelDbHelper(application))

        viewModelScope.launch {
            ensureDefaultTravelSpots()
            loadRecords()
        }
    }

    private suspend fun ensureDefaultTravelSpots() {
        if (preferences.getBoolean(KEY_DEFAULT_SPOTS_SEEDED, false)) {
            return
        }

        val existing = repository.getAllRecords()
        val defaults = listOf(
            DefaultTravelSpot(
                title = "해운대",
                location = "부산광역시 해운대구",
                description = "부산을 대표하는 해변 관광지로, 바다와 도시 스카이라인을 함께 즐길 수 있는 명소입니다.",
                imageRef = "img_haeundae",
                legacyImageRefs = setOf("haeundae", "img_haeundae"),
                createdAt = 1717372800000L,
                latitude = 35.1587,
                longitude = 129.1604
            ),
            DefaultTravelSpot(
                title = "한라산",
                location = "제주특별자치도 제주시/서귀포시",
                description = "제주도의 중심에 위치한 대한민국 최고봉으로, 사계절 자연경관이 아름다운 대표 명산입니다.",
                imageRef = "img_hallasan",
                legacyImageRefs = setOf("hallasan", "img_hallasan"),
                createdAt = 1717286400000L,
                latitude = 33.3617,
                longitude = 126.5292
            ),
            DefaultTravelSpot(
                title = "경복궁",
                location = "서울특별시 종로구 사직로 161",
                description = "조선 왕조의 대표 궁궐로, 전통 건축과 역사 문화를 체험할 수 있는 서울의 대표 관광지입니다.",
                imageRef = "img_gyeongbokgung",
                legacyImageRefs = setOf("gyeongbokgung", "img_gyeongbokgung"),
                createdAt = 1717200000000L,
                latitude = 37.5796,
                longitude = 126.9770
            )
        )

        defaults.forEach { spot ->
            val matchingRecord = existing.firstOrNull { record ->
                val recordImageRef = record.imageRef
                    ?.substringBeforeLast('.')
                    ?.trim()
                    ?.lowercase()
                record.title == spot.title || recordImageRef?.let { it in spot.legacyImageRefs } == true
            }

            val normalizedRecord = RecordEntity(
                id = matchingRecord?.id ?: 0,
                title = spot.title,
                memo = "${spot.location}\n${spot.description}",
                imageUri = "",
                createdAt = spot.createdAt,
                latitude = spot.latitude,
                longitude = spot.longitude,
                imageType = "DRAWABLE",
                imageRef = spot.imageRef
            )

            if (matchingRecord == null) {
                repository.insertRecord(normalizedRecord)
            } else if (matchingRecord != normalizedRecord) {
                repository.updateRecord(normalizedRecord)
            }
        }

        preferences.edit().putBoolean(KEY_DEFAULT_SPOTS_SEEDED, true).apply()
    }

    fun loadRecords() {
        viewModelScope.launch {
            _isLoading.value = true
            val rawList = repository.getAllRecords()
            _records.value = sortList(rawList, currentSortOrder)
            _recordsWithGps.value = repository.getRecordsWithGps()
            _isLoading.value = false
        }
    }

    fun setSortOrder(sortOrder: String) {
        currentSortOrder = sortOrder
        _records.value = sortList(_records.value, currentSortOrder)
    }

    private fun sortList(list: List<RecordEntity>, sortBy: String): List<RecordEntity> {
        return when (sortBy) {
            "DATE_ASC" -> list.sortedWith(compareBy<RecordEntity> { it.createdAt }.thenBy { it.id })
            "DATE_DESC" -> list.sortedWith(compareByDescending<RecordEntity> { it.createdAt }.thenByDescending { it.id })
            "TITLE_ASC" -> list.sortedBy { it.title }
            else -> list.sortedByDescending { it.createdAt }
        }
    }

    fun insertRecord(record: RecordEntity, onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val id = repository.insertRecord(record)
            loadRecords()
            _isLoading.value = false
            onComplete(id)
        }
    }

    fun updateRecord(record: RecordEntity, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val rows = repository.updateRecord(record)
            loadRecords()
            _isLoading.value = false
            onComplete(rows)
        }
    }

    fun deleteRecord(record: RecordEntity, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val rows = repository.deleteRecord(record)
            loadRecords()
            _isLoading.value = false
            onComplete(rows)
        }
    }

    fun deleteRecords(ids: List<Long>, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val rows = repository.deleteRecords(ids)
            loadRecords()
            _isLoading.value = false
            onComplete(rows)
        }
    }

    fun deleteAllRecords(onComplete: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.deleteAllRecords()
            _records.value = emptyList()
            _recordsWithGps.value = emptyList()
            _isLoading.value = false
            onComplete()
        }
    }

    fun getRecordById(id: Long, onResult: (RecordEntity?) -> Unit) {
        viewModelScope.launch {
            val record = repository.getRecordById(id)
            onResult(record)
        }
    }

    private data class DefaultTravelSpot(
        val title: String,
        val location: String,
        val description: String,
        val imageRef: String,
        val legacyImageRefs: Set<String>,
        val createdAt: Long,
        val latitude: Double,
        val longitude: Double
    )

    companion object {
        private const val KEY_DEFAULT_SPOTS_SEEDED = "default_spots_seeded"
    }
}
