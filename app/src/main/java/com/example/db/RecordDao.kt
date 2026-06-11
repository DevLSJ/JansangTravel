package com.example.db

import androidx.room.*

@Dao
interface RecordDao {
    @Query("SELECT * FROM records ORDER BY createdAt DESC")
    fun getAllRecords(): List<RecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRecord(record: RecordEntity): Long

    @Update
    fun updateRecord(record: RecordEntity): Int

    @Delete
    fun deleteRecord(record: RecordEntity): Int

    @Query("DELETE FROM records")
    fun deleteAllRecords()

    @Query("SELECT * FROM records WHERE id = :id")
    fun getRecordById(id: Long): RecordEntity?

    @Query("SELECT * FROM records WHERE latitude IS NOT NULL AND longitude IS NOT NULL ORDER BY createdAt DESC")
    fun getRecordsWithGps(): List<RecordEntity>
}
