package com.medimed.app.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.medimed.app.MediMedApp
import com.medimed.app.domain.model.LogStatus
import com.medimed.app.domain.model.MedicineLog
import com.medimed.app.domain.scheduler.MedicineAlarmScheduler
import com.medimed.app.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MedicineReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MedicineReceiver"

        // Actions
        const val ACTION_TAKE_MEDICINE = "com.medimed.app.ACTION_TAKE_MEDICINE"
        const val ACTION_SKIP_MEDICINE = "com.medimed.app.ACTION_SKIP_MEDICINE"
        const val ACTION_SNOOZE_MEDICINE = "com.medimed.app.ACTION_SNOOZE_MEDICINE"

        // Snooze duration in milliseconds (10 minutes)
        private const val SNOOZE_DURATION_MS = 10 * 60 * 1000L
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "onReceive triggered with action: $action")

        val app = context.applicationContext as MediMedApp
        val repository = app.repository
        val scheduler = app.scheduler
        val notificationHelper = app.notificationHelper

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                when (action) {
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_LOCKED_BOOT_COMPLETED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED -> {
                        Log.i(TAG, "System state changed ($action). Rescheduling all alarms.")
                        scheduler.rescheduleAllAlarms(repository)
                    }

                    MedicineAlarmScheduler.ACTION_TRIGGER_REMINDER -> {
                        val medicineId = intent.getLongExtra(MedicineAlarmScheduler.EXTRA_MEDICINE_ID, -1L)
                        val scheduledTime = intent.getLongExtra(MedicineAlarmScheduler.EXTRA_SCHEDULED_TIME, -1L)
                        
                        if (medicineId != -1L && scheduledTime != -1L) {
                            val medicine = repository.getMedicineById(medicineId)
                            if (medicine != null && medicine.isActive) {
                                // 1. Post notification
                                Log.i(TAG, "Triggering notification reminder for ${medicine.name}")
                                notificationHelper.showReminderNotification(medicine, scheduledTime)

                                // 2. Immediately schedule the next dose for this medicine
                                scheduler.scheduleAlarmForMedicine(medicine)
                            }
                        }
                    }

                    ACTION_TAKE_MEDICINE -> {
                        val medicineId = intent.getLongExtra(NotificationHelper.EXTRA_MEDICINE_ID, -1L)
                        val scheduledTime = intent.getLongExtra(NotificationHelper.EXTRA_SCHEDULED_TIME, -1L)

                        if (medicineId != -1L && scheduledTime != -1L) {
                            val medicine = repository.getMedicineById(medicineId)
                            if (medicine != null) {
                                // Record the positive compliance log
                                val log = MedicineLog(
                                    medicineId = medicineId,
                                    scheduledTime = scheduledTime,
                                    status = LogStatus.TAKEN,
                                    actionTime = System.currentTimeMillis()
                                )
                                repository.insertLog(log)

                                // Handle optional inventory/stock tracking
                                medicine.stockCount?.let { currentStock ->
                                    if (currentStock > 0) {
                                        val updatedMedicine = medicine.copy(
                                            stockCount = currentStock - 1
                                        )
                                        repository.updateMedicine(updatedMedicine)
                                    }
                                }

                                Log.i(TAG, "Recorded TAKE for ${medicine.name} scheduled at $scheduledTime")
                            }
                            notificationHelper.cancelNotification(medicineId)
                        }
                    }

                    ACTION_SKIP_MEDICINE -> {
                        val medicineId = intent.getLongExtra(NotificationHelper.EXTRA_MEDICINE_ID, -1L)
                        val scheduledTime = intent.getLongExtra(NotificationHelper.EXTRA_SCHEDULED_TIME, -1L)

                        if (medicineId != -1L && scheduledTime != -1L) {
                            val medicine = repository.getMedicineById(medicineId)
                            if (medicine != null) {
                                // Record the skip log
                                val log = MedicineLog(
                                    medicineId = medicineId,
                                    scheduledTime = scheduledTime,
                                    status = LogStatus.SKIPPED,
                                    actionTime = System.currentTimeMillis()
                                )
                                repository.insertLog(log)
                                Log.i(TAG, "Recorded SKIP for ${medicine.name} scheduled at $scheduledTime")
                            }
                            notificationHelper.cancelNotification(medicineId)
                        }
                    }

                    ACTION_SNOOZE_MEDICINE -> {
                        val medicineId = intent.getLongExtra(NotificationHelper.EXTRA_MEDICINE_ID, -1L)
                        val scheduledTime = intent.getLongExtra(NotificationHelper.EXTRA_SCHEDULED_TIME, -1L)

                        if (medicineId != -1L && scheduledTime != -1L) {
                            val medicine = repository.getMedicineById(medicineId)
                            if (medicine != null) {
                                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                
                                val snoozeTime = System.currentTimeMillis() + SNOOZE_DURATION_MS
                                
                                val snoozeIntent = Intent(context, MedicineReminderReceiver::class.java).apply {
                                    this.action = MedicineAlarmScheduler.ACTION_TRIGGER_REMINDER
                                    this.putExtra(MedicineAlarmScheduler.EXTRA_MEDICINE_ID, medicineId)
                                    this.putExtra(MedicineAlarmScheduler.EXTRA_SCHEDULED_TIME, scheduledTime)
                                }

                                val pendingIntent = PendingIntent.getBroadcast(
                                    context,
                                    medicineId.toInt(),
                                    snoozeIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )

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
                                Log.i(TAG, "Snoozed alarm for ${medicine.name} by 10 minutes.")
                            }
                            notificationHelper.cancelNotification(medicineId)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in broadcast execution background coroutine", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
