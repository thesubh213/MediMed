package com.medimed.app.domain.scheduler

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.medimed.app.domain.model.FrequencyType
import com.medimed.app.domain.model.Medicine
import com.medimed.app.domain.repository.MedicineRepository
import com.medimed.app.receiver.MedicineReminderReceiver
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Locale

class MedicineAlarmScheduler(private val context: Context) {

    private val alarmManager by lazy { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }

    companion object {
        private const val TAG = "MedicineScheduler"
        const val ACTION_TRIGGER_REMINDER = "com.medimed.app.ACTION_TRIGGER_REMINDER"
        const val EXTRA_MEDICINE_ID = "extra_medicine_id"
        const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
    }

    /**
     * Calculates the next upcoming scheduled dose time after the given timestamp.
     * Returns null if no future doses can be scheduled (e.g. past end date).
     */
    fun calculateNextDoseTime(medicine: Medicine, fromTimeMillis: Long): Long? {
        if (!medicine.isActive) return null

        val now = Calendar.getInstance()
        val startCal = Calendar.getInstance().apply { timeInMillis = medicine.startDate }
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // The effective starting time is the maximum of startDate and now
        val effectiveStart = maxOf(medicine.startDate, fromTimeMillis)

        // If the end date has already passed, we cannot schedule
        if (medicine.endDate != null && effectiveStart >= medicine.endDate) {
            return null
        }

        return when (medicine.frequencyType) {
            FrequencyType.DAILY -> {
                getNextDailyDose(medicine, effectiveStart)
            }
            FrequencyType.WEEKDAYS -> {
                getNextWeekdayDose(medicine, effectiveStart)
            }
            FrequencyType.INTERVAL -> {
                getNextIntervalDose(medicine, effectiveStart)
            }
        }
    }

    private fun getNextDailyDose(medicine: Medicine, effectiveStart: Long): Long? {
        val candidates = mutableListOf<Long>()
        
        // Check times for today and the next 7 days to cover timezone boundaries
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = effectiveStart
        
        for (dayOffset in 0..7) {
            val baseDay = Calendar.getInstance().apply {
                timeInMillis = calendar.timeInMillis
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            
            for (timeStr in medicine.timesOfDay) {
                val timeParts = timeStr.split(":")
                if (timeParts.size != 2) continue
                val hour = timeParts[0].toIntOrNull() ?: continue
                val minute = timeParts[1].toIntOrNull() ?: continue
                
                val candidate = Calendar.getInstance().apply {
                    timeInMillis = baseDay.timeInMillis
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                if (candidate > effectiveStart && candidate >= medicine.startDate) {
                    if (medicine.endDate == null || candidate <= medicine.endDate) {
                        candidates.add(candidate)
                    }
                }
            }
        }
        
        return candidates.minOrNull()
    }

    private fun getNextWeekdayDose(medicine: Medicine, effectiveStart: Long): Long? {
        val activeDays = medicine.frequencyData.split(",")
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() }
        
        if (activeDays.isEmpty()) return null
        
        val dayOfWeekMap = mapOf(
            "sunday" to Calendar.SUNDAY,
            "monday" to Calendar.MONDAY,
            "tuesday" to Calendar.TUESDAY,
            "wednesday" to Calendar.WEDNESDAY,
            "thursday" to Calendar.THURSDAY,
            "friday" to Calendar.FRIDAY,
            "saturday" to Calendar.SATURDAY
        )

        val targetDays = activeDays.mapNotNull { dayOfWeekMap[it] }
        if (targetDays.isEmpty()) return null

        val candidates = mutableListOf<Long>()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = effectiveStart

        for (dayOffset in 0..14) { // Check up to 14 days ahead
            val baseDay = Calendar.getInstance().apply {
                timeInMillis = calendar.timeInMillis
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            
            if (targetDays.contains(baseDay.get(Calendar.DAY_OF_WEEK))) {
                for (timeStr in medicine.timesOfDay) {
                    val timeParts = timeStr.split(":")
                    if (timeParts.size != 2) continue
                    val hour = timeParts[0].toIntOrNull() ?: continue
                    val minute = timeParts[1].toIntOrNull() ?: continue

                    val candidate = Calendar.getInstance().apply {
                        timeInMillis = baseDay.timeInMillis
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    if (candidate > effectiveStart && candidate >= medicine.startDate) {
                        if (medicine.endDate == null || candidate <= medicine.endDate) {
                            candidates.add(candidate)
                        }
                    }
                }
            }
        }

        return candidates.minOrNull()
    }

    private fun getNextIntervalDose(medicine: Medicine, effectiveStart: Long): Long? {
        val intervalHours = medicine.frequencyData.toIntOrNull() ?: return null
        if (intervalHours <= 0) return null
        val intervalMillis = intervalHours.toLong() * 60 * 60 * 1000

        // The sequence starts at the first scheduled time on the startDate.
        val baseTimeStr = medicine.timesOfDay.firstOrNull() ?: "08:00"
        val timeParts = baseTimeStr.split(":")
        val startHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 8
        val startMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

        val sequenceStart = Calendar.getInstance().apply {
            timeInMillis = medicine.startDate
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, startMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // If now is before sequence start, the very first dose is the sequence start
        if (effectiveStart < sequenceStart) {
            return if (medicine.endDate == null || sequenceStart <= medicine.endDate) {
                sequenceStart
            } else {
                null
            }
        }

        // Otherwise, find the next interval step
        val diff = effectiveStart - sequenceStart
        val steps = (diff / intervalMillis) + 1
        val nextTime = sequenceStart + (steps * intervalMillis)

        return if (medicine.endDate == null || nextTime <= medicine.endDate) {
            nextTime
        } else {
            null
        }
    }

    /**
     * Schedules the next upcoming reminder alarm for the medicine.
     */
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleAlarmForMedicine(medicine: Medicine) {
        if (!medicine.isActive) {
            cancelAlarmForMedicine(medicine)
            return
        }

        // Check if we can schedule exact alarms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarms - permission missing. Alarm will be inaccurate.")
            }
        }

        val nextTime = calculateNextDoseTime(medicine, System.currentTimeMillis())
        if (nextTime == null) {
            Log.d(TAG, "No next scheduled dose found for ${medicine.name} (ID: ${medicine.id})")
            cancelAlarmForMedicine(medicine)
            return
        }

        val intent = Intent(context, MedicineReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(EXTRA_MEDICINE_ID, medicine.id)
            putExtra(EXTRA_SCHEDULED_TIME, nextTime)
        }

        // We use a unique request code per medicine to support multiple alarms (one active alarm per medicine)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicine.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Schedule to wake up even if the device is in idle doze mode
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    nextTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled alarm for ${medicine.name} at epoch $nextTime")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule exact alarm for ${medicine.name}", e)
        }
    }

    /**
     * Schedules a snooze alarm to trigger in the future.
     */
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleSnoozeAlarm(medicineId: Long, scheduledTime: Long, snoozeTime: Long) {
        val intent = Intent(context, MedicineReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(EXTRA_MEDICINE_ID, medicineId)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicineId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    snoozeTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    snoozeTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled snooze alarm for medicine ID: $medicineId at epoch $snoozeTime")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule exact snooze alarm for medicine ID: $medicineId", e)
        }
    }

    /**
     * Cancels any active alarms scheduled for the medicine.
     */
    fun cancelAlarmForMedicine(medicine: Medicine) {
        val intent = Intent(context, MedicineReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicine.id.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled alarm for medicine ID: ${medicine.id}")
        }
    }

    /**
     * Queries the database and schedules alarms for all active medicines.
     */
    suspend fun rescheduleAllAlarms(repository: MedicineRepository) {
        val activeMedicines = repository.getActiveMedicines().first()
        Log.d(TAG, "Rescheduling alarms for ${activeMedicines.size} active medicines.")
        for (medicine in activeMedicines) {
            scheduleAlarmForMedicine(medicine)
        }
    }
}
