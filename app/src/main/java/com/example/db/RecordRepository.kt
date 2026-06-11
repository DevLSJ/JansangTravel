package com.example.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecordRepository(private val recordDao: RecordDao) {

    suspend fun getAllRecords(): List<RecordEntity> = withContext(Dispatchers.IO) {
        recordDao.getAllRecords()
    }

    suspend fun insertRecord(record: RecordEntity): Long = withContext(Dispatchers.IO) {
        recordDao.insertRecord(record)
    }

    suspend fun updateRecord(record: RecordEntity): Int = withContext(Dispatchers.IO) {
        recordDao.updateRecord(record)
    }

    suspend fun deleteRecord(record: RecordEntity): Int = withContext(Dispatchers.IO) {
        recordDao.deleteRecord(record)
    }

    suspend fun deleteAllRecords() = withContext(Dispatchers.IO) {
        recordDao.deleteAllRecords()
    }

    suspend fun getRecordById(id: Long): RecordEntity? = withContext(Dispatchers.IO) {
        recordDao.getRecordById(id)
    }

    suspend fun getRecordsWithGps(): List<RecordEntity> = withContext(Dispatchers.IO) {
        recordDao.getRecordsWithGps()
    }
}
