package com.medimed.app.domain.scheduler

import com.medimed.app.domain.model.FrequencyType
import com.medimed.app.domain.model.Medicine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Calendar

class MedicineAlarmSchedulerTest {

    
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
            startDate = today8AM.timeInMillis - 2 * 60 * 60 * 1000 
        )

        
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
            startDate = today8AM.timeInMillis - 24 * 60 * 60 * 1000 
        )

        
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

        
        val medicine = Medicine(
            id = 1L,
            name = "Ibuprofen",
            dosage = "1 tablet",
            frequencyType = FrequencyType.INTERVAL,
            frequencyData = "8",
            timesOfDay = listOf("08:00"),
            startDate = today8AM.timeInMillis
        )

        
        val now9AM = Calendar.getInstance().apply {
            timeInMillis = today8AM.timeInMillis
            set(Calendar.HOUR_OF_DAY, 9)
        }.timeInMillis

        val nextDose = scheduler.calculateNextDoseTime(medicine, now9AM)
        assertNotNull(nextDose)

        val expected = Calendar.getInstance().apply {
            timeInMillis = today8AM.timeInMillis
            set(Calendar.HOUR_OF_DAY, 16) 
        }.timeInMillis

        assertEquals(expected, nextDose)
    }
}



class DummyContext : android.content.ContextWrapper(null) {
    override fun getSystemService(name: String): Any {
        
        return Any()
    }
    override fun getApplicationContext(): android.content.Context {
        return this
    }
}
