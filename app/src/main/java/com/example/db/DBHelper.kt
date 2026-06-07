package com.example.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.model.TravelItem

class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "JansangTravel.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_NAME = "travel_diary"
        const val COLUMN_NO = "no"
        const val COLUMN_PLACE = "place"
        const val COLUMN_VISIT_DATE = "visit_date"
        const val COLUMN_MEMO = "memo"
        const val COLUMN_PHOTO_URI = "photo_uri"
        const val COLUMN_LATITUDE = "latitude"
        const val COLUMN_LONGITUDE = "longitude"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = ("CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_NO INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_PLACE TEXT NOT NULL, " +
                "$COLUMN_VISIT_DATE TEXT NOT NULL, " +
                "$COLUMN_MEMO TEXT, " +
                "$COLUMN_PHOTO_URI TEXT, " +
                "$COLUMN_LATITUDE REAL, " +
                "$COLUMN_LONGITUDE REAL)")
        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertTravel(
        place: String,
        visitDate: String,
        memo: String,
        photoUri: String?,
        latitude: Double,
        longitude: Double
    ): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PLACE, place)
            put(COLUMN_VISIT_DATE, visitDate)
            put(COLUMN_MEMO, memo)
            put(COLUMN_PHOTO_URI, photoUri)
            put(COLUMN_LATITUDE, latitude)
            put(COLUMN_LONGITUDE, longitude)
        }
        val id = db.insert(TABLE_NAME, null, values)
        db.close()
        return id
    }

    fun getAllTravels(sortBy: String = "DATE_DESC"): List<TravelItem> {
        val travelList = mutableListOf<TravelItem>()
        val db = this.readableDatabase

        val orderBy = when (sortBy) {
            "DATE_ASC" -> "$COLUMN_VISIT_DATE ASC, $COLUMN_NO DESC"
            "DATE_DESC" -> "$COLUMN_VISIT_DATE DESC, $COLUMN_NO DESC"
            "PLACE_ASC" -> "$COLUMN_PLACE ASC"
            else -> "$COLUMN_VISIT_DATE DESC, $COLUMN_NO DESC"
        }

        val selectQuery = "SELECT * FROM $TABLE_NAME ORDER BY $orderBy"
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val item = TravelItem(
                    no = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_NO)),
                    place = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PLACE)),
                    visitDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VISIT_DATE)),
                    memo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEMO)),
                    photoUri = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHOTO_URI)),
                    latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE)),
                    longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE))
                )
                travelList.add(item)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return travelList
    }

    fun getTravelById(no: Long): TravelItem? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            "$COLUMN_NO = ?",
            arrayOf(no.toString()),
            null, null, null
        )
        var item: TravelItem? = null
        if (cursor.moveToFirst()) {
            item = TravelItem(
                no = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_NO)),
                place = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PLACE)),
                visitDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VISIT_DATE)),
                memo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEMO)),
                photoUri = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHOTO_URI)),
                latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE)),
                longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE))
            )
        }
        cursor.close()
        db.close()
        return item
    }

    fun updateTravel(
        no: Long,
        place: String,
        visitDate: String,
        memo: String,
        photoUri: String?,
        latitude: Double,
        longitude: Double
    ): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PLACE, place)
            put(COLUMN_VISIT_DATE, visitDate)
            put(COLUMN_MEMO, memo)
            put(COLUMN_PHOTO_URI, photoUri)
            put(COLUMN_LATITUDE, latitude)
            put(COLUMN_LONGITUDE, longitude)
        }
        val rowsAffected = db.update(TABLE_NAME, values, "$COLUMN_NO = ?", arrayOf(no.toString()))
        db.close()
        return rowsAffected
    }

    fun deleteTravel(no: Long): Int {
        val db = this.writableDatabase
        val rowsDeleted = db.delete(TABLE_NAME, "$COLUMN_NO = ?", arrayOf(no.toString()))
        db.close()
        return rowsDeleted
    }

    fun deleteAllTravels() {
        val db = this.writableDatabase
        db.delete(TABLE_NAME, null, null)
        db.close()
    }
}
