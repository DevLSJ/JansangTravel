package com.example.db

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecordViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RecordRepository
    
    private val _records = MutableStateFlow<List<RecordEntity>>(emptyList())
    val records: StateFlow<List<RecordEntity>> = _records

    private val _recordsWithGps = MutableStateFlow<List<RecordEntity>>(emptyList())
    val recordsWithGps: StateFlow<List<RecordEntity>> = _recordsWithGps

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var currentSortOrder = "DATE_DESC" // "DATE_DESC", "DATE_ASC", "TITLE_ASC"

    init {
        val database = AppDatabase.getDatabase(application)
        repository = RecordRepository(database.recordDao())
        
        // Populate default data if database is empty on start
        viewModelScope.launch {
            checkAndInsertDefaultData()
            loadRecords()
        }
    }

    private suspend fun checkAndInsertDefaultData() {
        val prefs = getApplication<Application>().getSharedPreferences("TravelAppPrefs", android.content.Context.MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("isFirstRunRoomV2", true)
        
        if (isFirstRun) {
            val existing = repository.getAllRecords()
            if (existing.isEmpty()) {
                // Insert default records
                repository.insertRecord(
                    RecordEntity(
                        title = "한라산",
                        memo = "제주도의 중심에 우뚝 솟은 대한민국에서 가장 높은 산으로, 사계절 아름다운 자연 경관을 자랑합니다.",
                        imageUri = "",
                        createdAt = 1717200000000L, // 2026-06-01
                        latitude = 33.3617,
                        longitude = 126.5292,
                        imageType = "DRAWABLE",
                        imageRef = "hallasan"
                    )
                )
                repository.insertRecord(
                    RecordEntity(
                        title = "경복궁",
                        memo = "조선 왕조의 법궁으로, 서울의 중심에서 한국 전통 건축의 아름다움과 역사의 기품을 간직하고 있습니다.",
                        imageUri = "",
                        createdAt = 1717286400000L, // 2026-06-02
                        latitude = 37.5796,
                        longitude = 126.9770,
                        imageType = "DRAWABLE",
                        imageRef = "gyeongbokgung"
                    )
                )
                repository.insertRecord(
                    RecordEntity(
                        title = "해운대",
                        memo = "부산을 대표하는 해수욕장으로, 넓은 백사장과 현대적인 고층 빌딩이 어우러진 해양 휴양지입니다.",
                        imageUri = "",
                        createdAt = 1717372800000L, // 2026-06-03
                        latitude = 35.1587,
                        longitude = 129.1604,
                        imageType = "DRAWABLE",
                        imageRef = "haeundae"
                    )
                )
            }
            prefs.edit().putBoolean("isFirstRunRoomV2", false).apply()
        }
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
        // Resort current list
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

    fun deleteAllRecords(onComplete: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.deleteAllRecords()
            _records.value = emptyList()
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
}
