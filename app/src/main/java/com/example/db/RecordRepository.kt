package com.example.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecordRepository(private val dbHelper: TravelDbHelper) {

    suspend fun getAllRecords(): List<RecordEntity> = withContext(Dispatchers.IO) {
        dbHelper.getAllTravels()
    }

    suspend fun insertRecord(record: RecordEntity): Long = withContext(Dispatchers.IO) {
        dbHelper.insertTravel(record)
    }

    suspend fun updateRecord(record: RecordEntity): Int = withContext(Dispatchers.IO) {
        dbHelper.updateTravel(record)
    }

    suspend fun deleteRecord(record: RecordEntity): Int = withContext(Dispatchers.IO) {
        dbHelper.deleteTravel(record.id)
    }

    suspend fun deleteRecords(ids: List<Long>): Int = withContext(Dispatchers.IO) {
        dbHelper.deleteTravels(ids)
    }

    suspend fun deleteAllRecords(): Int = withContext(Dispatchers.IO) {
        dbHelper.deleteAllTravels()
    }

    suspend fun getRecordById(id: Long): RecordEntity? = withContext(Dispatchers.IO) {
        dbHelper.getTravelById(id)
    }

    suspend fun getRecordsWithGps(): List<RecordEntity> = withContext(Dispatchers.IO) {
        dbHelper.getTravelsWithGps()
    }
}
