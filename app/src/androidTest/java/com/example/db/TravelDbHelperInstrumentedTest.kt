package com.example.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TravelDbHelperInstrumentedTest {

    @Test
    fun sqliteCrudMethodsWorkEndToEnd() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbHelper = TravelDbHelper(context)
        dbHelper.deleteAllTravels()

        val insertedId = dbHelper.insertTravel(
            RecordEntity(
                title = "테스트 여행지",
                memo = "테스트 위치\n테스트 메모",
                imageUri = "file:///tmp/test.jpg",
                createdAt = 1_717_200_000_000L,
                latitude = 37.5665,
                longitude = 126.9780,
                imageType = "URI",
                imageRef = "file:///tmp/test.jpg"
            )
        )

        assertTrue(insertedId > 0)
        assertEquals(1, dbHelper.getAllTravels().size)

        val inserted = dbHelper.getTravelById(insertedId)
        assertNotNull(inserted)
        assertEquals("테스트 여행지", inserted?.title)

        val updatedRows = dbHelper.updateTravel(
            inserted!!.copy(
                title = "수정된 여행지",
                memo = "수정된 위치\n수정된 메모",
                latitude = 35.1587,
                longitude = 129.1604
            )
        )
        assertEquals(1, updatedRows)
        assertEquals("수정된 여행지", dbHelper.getTravelById(insertedId)?.title)
        assertEquals(1, dbHelper.getTravelsWithGps().size)

        assertEquals(1, dbHelper.deleteTravel(insertedId))
        assertEquals(0, dbHelper.getAllTravels().size)

        dbHelper.insertTravel(inserted.copy(id = 0, title = "전체 삭제 테스트"))
        assertEquals(1, dbHelper.deleteAllTravels())
        assertEquals(0, dbHelper.getAllTravels().size)
    }
}
