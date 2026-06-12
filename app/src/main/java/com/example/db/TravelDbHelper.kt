package com.example.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TravelDbHelper(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_SQL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        migrateFromPreviousSchema(db)
    }

    fun insertTravel(record: RecordEntity): Long {
        return writableDatabase.insert(TABLE_TRAVELS, null, record.toContentValues())
    }

    fun getAllTravels(): List<RecordEntity> {
        val records = mutableListOf<RecordEntity>()
        readableDatabase.query(
            TABLE_TRAVELS,
            ALL_COLUMNS,
            null,
            null,
            null,
            null,
            "$COLUMN_ID DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                records += cursor.toRecordEntity()
            }
        }
        return records
    }

    fun getTravelById(id: Long): RecordEntity? {
        readableDatabase.query(
            TABLE_TRAVELS,
            ALL_COLUMNS,
            "$COLUMN_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toRecordEntity() else null
        }
    }

    fun updateTravel(record: RecordEntity): Int {
        return writableDatabase.update(
            TABLE_TRAVELS,
            record.toContentValues(),
            "$COLUMN_ID = ?",
            arrayOf(record.id.toString())
        )
    }

    fun deleteTravel(id: Long): Int {
        return writableDatabase.delete(
            TABLE_TRAVELS,
            "$COLUMN_ID = ?",
            arrayOf(id.toString())
        )
    }

    fun deleteTravels(ids: List<Long>): Int {
        if (ids.isEmpty()) return 0

        val db = writableDatabase
        var deletedRows = 0
        db.beginTransaction()
        try {
            ids.forEach { id ->
                deletedRows += db.delete(
                    TABLE_TRAVELS,
                    "$COLUMN_ID = ?",
                    arrayOf(id.toString())
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return deletedRows
    }

    fun deleteAllTravels(): Int {
        return writableDatabase.delete(TABLE_TRAVELS, null, null)
    }

    fun getTravelsWithGps(): List<RecordEntity> {
        val records = mutableListOf<RecordEntity>()
        readableDatabase.query(
            TABLE_TRAVELS,
            ALL_COLUMNS,
            "$COLUMN_LATITUDE IS NOT NULL AND $COLUMN_LONGITUDE IS NOT NULL",
            null,
            null,
            null,
            "$COLUMN_ID DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                records += cursor.toRecordEntity()
            }
        }
        return records
    }

    private fun migrateFromPreviousSchema(db: SQLiteDatabase) {
        db.beginTransaction()
        try {
            db.execSQL("ALTER TABLE $TABLE_TRAVELS RENAME TO ${TABLE_TRAVELS}_old")
            db.execSQL(CREATE_TABLE_SQL)

            db.rawQuery("SELECT * FROM ${TABLE_TRAVELS}_old", null).use { cursor ->
                while (cursor.moveToNext()) {
                    val values = ContentValues().apply {
                        put(COLUMN_ID, cursor.getLongOrNull("id") ?: 0L)
                        put(COLUMN_PLACE, cursor.getStringOrNull("title").orEmpty())
                        put(COLUMN_VISIT_DATE, formatDate(cursor.getLongOrNull("createdAt") ?: System.currentTimeMillis()))
                        put(COLUMN_MEMO, cursor.getStringOrNull("memo").orEmpty())
                        put(COLUMN_PHOTO_URI, cursor.getStringOrNull("imageUri").orEmpty())
                        put(COLUMN_LATITUDE, cursor.getDoubleOrNull("latitude"))
                        put(COLUMN_LONGITUDE, cursor.getDoubleOrNull("longitude"))
                        put(COLUMN_IMAGE_TYPE, cursor.getStringOrNull("imageType") ?: "URI")
                        put(COLUMN_IMAGE_REF, cursor.getStringOrNull("imageRef"))
                    }
                    db.insert(TABLE_TRAVELS, null, values)
                }
            }

            db.execSQL("DROP TABLE IF EXISTS ${TABLE_TRAVELS}_old")
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            db.execSQL("DROP TABLE IF EXISTS ${TABLE_TRAVELS}_old")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_TRAVELS")
            db.execSQL(CREATE_TABLE_SQL)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun RecordEntity.toContentValues(): ContentValues {
        return ContentValues().apply {
            if (id > 0) put(COLUMN_ID, id)
            put(COLUMN_PLACE, title)
            put(COLUMN_VISIT_DATE, formatDate(createdAt))
            put(COLUMN_MEMO, memo)
            put(COLUMN_PHOTO_URI, imageUri)
            put(COLUMN_LATITUDE, latitude)
            put(COLUMN_LONGITUDE, longitude)
            put(COLUMN_IMAGE_TYPE, imageType)
            put(COLUMN_IMAGE_REF, imageRef)
        }
    }

    private fun Cursor.toRecordEntity(): RecordEntity {
        val visitDate = getStringOrNull(COLUMN_VISIT_DATE).orEmpty()
        return RecordEntity(
            id = getLongOrNull(COLUMN_ID) ?: 0L,
            title = getStringOrNull(COLUMN_PLACE).orEmpty(),
            memo = getStringOrNull(COLUMN_MEMO).orEmpty(),
            imageUri = getStringOrNull(COLUMN_PHOTO_URI).orEmpty(),
            createdAt = parseDate(visitDate),
            latitude = getDoubleOrNull(COLUMN_LATITUDE),
            longitude = getDoubleOrNull(COLUMN_LONGITUDE),
            imageType = getStringOrNull(COLUMN_IMAGE_TYPE) ?: "URI",
            imageRef = getStringOrNull(COLUMN_IMAGE_REF)
        )
    }

    private fun Cursor.getStringOrNull(columnName: String): String? {
        val index = getColumnIndex(columnName)
        return if (index >= 0 && !isNull(index)) getString(index) else null
    }

    private fun Cursor.getLongOrNull(columnName: String): Long? {
        val index = getColumnIndex(columnName)
        return if (index >= 0 && !isNull(index)) getLong(index) else null
    }

    private fun Cursor.getDoubleOrNull(columnName: String): Double? {
        val index = getColumnIndex(columnName)
        return if (index >= 0 && !isNull(index)) getDouble(index) else null
    }

    companion object {
        private const val DATABASE_NAME = "record_database"
        private const val DATABASE_VERSION = 2

        private const val TABLE_TRAVELS = "records"
        private const val COLUMN_ID = "id"
        private const val COLUMN_PLACE = "place"
        private const val COLUMN_VISIT_DATE = "visit_date"
        private const val COLUMN_MEMO = "memo"
        private const val COLUMN_PHOTO_URI = "photo_uri"
        private const val COLUMN_LATITUDE = "latitude"
        private const val COLUMN_LONGITUDE = "longitude"
        private const val COLUMN_IMAGE_TYPE = "image_type"
        private const val COLUMN_IMAGE_REF = "image_ref"

        private val ALL_COLUMNS = arrayOf(
            COLUMN_ID,
            COLUMN_PLACE,
            COLUMN_VISIT_DATE,
            COLUMN_MEMO,
            COLUMN_PHOTO_URI,
            COLUMN_LATITUDE,
            COLUMN_LONGITUDE,
            COLUMN_IMAGE_TYPE,
            COLUMN_IMAGE_REF
        )

        private const val CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS $TABLE_TRAVELS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PLACE TEXT NOT NULL,
                $COLUMN_VISIT_DATE TEXT NOT NULL,
                $COLUMN_MEMO TEXT,
                $COLUMN_PHOTO_URI TEXT,
                $COLUMN_LATITUDE REAL,
                $COLUMN_LONGITUDE REAL,
                $COLUMN_IMAGE_TYPE TEXT DEFAULT 'URI',
                $COLUMN_IMAGE_REF TEXT
            )
        """

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        private fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))

        private fun parseDate(dateText: String): Long {
            return try {
                dateFormat.parse(dateText)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        }
    }
}
