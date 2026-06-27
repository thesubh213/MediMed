package com.medimed.app.domain.scheduler

import com.medimed.app.domain.model.FrequencyType
import com.medimed.app.domain.model.Medicine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Calendar

class MedicineAlarmSchedulerTest {

    // Simple stub context or mock is not needed for calculateNextDoseTime as it's a pure function!
    private val scheduler = MedicineAlarmScheduler(DummyContext())

    @Test
    fun testCalculateNextDose_Daily_Future() {
        val today8AM = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val medicine = Medicine(
            id = 1L,
            name = "Aspirin",
            dosage = "1 pill",
            frequencyType = FrequencyType.DAILY,
            frequencyData = "",
            timesOfDay = listOf("08:00", "20:00"),
            startDate = today8AM.timeInMillis - 2 * 60 * 60 * 1000 // started 2 hours ago
        )

        // If it's currently 07:00 AM today, next dose should be 08:00 AM today
        val now7AM = Calendar.getInstance().apply {
            timeInMillis = today8AM.timeInMillis
            set(Calendar.HOUR_OF_DAY, 7)
        }.timeInMillis

        val nextDose = scheduler.calculateNextDoseTime(medicine, now7AM)
        assertNotNull(nextDose)
        
        val expected = Calendar.getInstance().apply {
            timeInMillis = today8AM.timeInMillis
            set(Calendar.HOUR_OF_DAY, 8)
        }.timeInMillis
        
        assertEquals(expected, nextDose)
    }

    @Test
    fun testCalculateNextDose_Daily_NextDay() {
        val today8AM = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val medicine = Medicine(
            id = 1L,
            name = "Aspirin",
            dosage = "1 pill",
            frequencyType = FrequencyType.DAILY,
            frequencyData = "",
            timesOfDay = listOf("08:00"),
            startDate = today8AM.timeInMillis - 24 * 60 * 60 * 1000 // started yesterday
        )

        // If it's currently 09:00 AM today, next dose should be 08:00 AM tomorrow
        val now9AM = Calendar.getInstance().apply {
            timeInMillis = today8AM.timeInMillis
            set(Calendar.HOUR_OF_DAY, 9)
        }.timeInMillis

        val nextDose = scheduler.calculateNextDoseTime(medicine, now9AM)
        assertNotNull(nextDose)

        val expected = Calendar.getInstance().apply {
            timeInMillis = today8AM.timeInMillis
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 8)
        }.timeInMillis

        assertEquals(expected, nextDose)
    }

    @Test
    fun testCalculateNextDose_Interval() {
        val today8AM = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Interval: every 8 hours starting at 08:00 AM
        val medicine = Medicine(
            id = 1L,
            name = "Ibuprofen",
            dosage = "1 tablet",
            frequencyType = FrequencyType.INTERVAL,
            frequencyData = "8",
            timesOfDay = listOf("08:00"),
            startDate = today8AM.timeInMillis
        )

        // If it's currently 09:00 AM today, next dose should be 04:00 PM today (8 hours after 08:00 AM)
        val now9AM = Calendar.getInstance().apply {
            timeInMillis = today8AM.timeInMillis
            set(Calendar.HOUR_OF_DAY, 9)
        }.timeInMillis

        val nextDose = scheduler.calculateNextDoseTime(medicine, now9AM)
        assertNotNull(nextDose)

        val expected = Calendar.getInstance().apply {
            timeInMillis = today8AM.timeInMillis
            set(Calendar.HOUR_OF_DAY, 16) // 16:00 is 04:00 PM
        }.timeInMillis

        assertEquals(expected, nextDose)
    }
}

// A dummy context to instantiate the scheduler class for testing calculateNextDoseTime
// since it accesses System ALARM_SERVICE but calculateNextDoseTime itself does not use context.
class DummyContext : android.content.ContextWrapper(null) {
    override fun getSystemService(name: String): Any {
        // Return dummy mock/null objects so constructor doesn't crash
        return Any()
    }
    override fun getApplicationContext(): android.content.Context {
        return this
    }
}
