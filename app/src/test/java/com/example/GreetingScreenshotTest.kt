package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.db.DBHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseUnitTest {

    private lateinit var dbHelper: DBHelper

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        dbHelper = DBHelper(context)
        dbHelper.deleteAllTravels()
    }

    @Test
    fun testDatabaseCrudOperations() {
        // 1. Create (Insert)
        val id = dbHelper.insertTravel(
            place = "Jeju",
            visitDate = "2026-06-01",
            memo = "Ocean waves and green tea fields.",
            photoUri = "content://media/external/images/media/42",
            latitude = 33.4996,
            longitude = 126.5312
        )
        assertTrue(id != -1L)

        // 2. Read (Query)
        var travels = dbHelper.getAllTravels()
        assertEquals(1, travels.size)
        val firstItem = travels[0]
        assertEquals("Jeju", firstItem.place)
        assertEquals(33.4996, firstItem.latitude, 0.0001)

        // 3. Update
        val updatedRows = dbHelper.updateTravel(
            no = firstItem.no,
            place = "Jeju Island Star",
            visitDate = "2026-06-02",
            memo = "Sunset views over the ocean.",
            photoUri = firstItem.photoUri,
            latitude = 33.5,
            longitude = 126.6
        )
        assertEquals(1, updatedRows)

        travels = dbHelper.getAllTravels()
        assertEquals(1, travels.size)
        assertEquals("Jeju Island Star", travels[0].place)
        assertEquals("2026-06-02", travels[0].visitDate)

        // 4. Delete
        val deletedRows = dbHelper.deleteTravel(firstItem.no)
        assertEquals(1, deletedRows)

        travels = dbHelper.getAllTravels()
        assertTrue(travels.isEmpty())
    }
}
