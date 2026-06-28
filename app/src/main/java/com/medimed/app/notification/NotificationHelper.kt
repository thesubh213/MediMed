package com.medimed.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.medimed.app.R
import com.medimed.app.domain.model.Medicine
import com.medimed.app.presentation.MainActivity
import com.medimed.app.presentation.ReminderActivity
import com.medimed.app.receiver.MedicineReminderReceiver

class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "medicine_reminders_channel"
        const val CHANNEL_NAME = "Medicine Reminders"
        const val CHANNEL_DESCRIPTION = "Notifications for scheduled medication reminders"
        
        const val EXTRA_MEDICINE_ID = "extra_medicine_id"
        const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                lightColor = Color.parseColor("#a53860") 
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    
    fun showReminderNotification(medicine: Medicine, scheduledTime: Long) {
        val notificationId = medicine.id.toInt()

        
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        
        val fullScreenIntent = Intent(context, ReminderActivity::class.java).apply {
            putExtra(EXTRA_MEDICINE_ID, medicine.id)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 400000,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        
        val takeIntent = Intent(context, MedicineReminderReceiver::class.java).apply {
            action = MedicineReminderReceiver.ACTION_TAKE_MEDICINE
            putExtra(EXTRA_MEDICINE_ID, medicine.id)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
        }
        val takePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 100000, 
            takeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        
        val skipIntent = Intent(context, MedicineReminderReceiver::class.java).apply {
            action = MedicineReminderReceiver.ACTION_SKIP_MEDICINE
            putExtra(EXTRA_MEDICINE_ID, medicine.id)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 200000,
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        
        val snoozeIntent = Intent(context, MedicineReminderReceiver::class.java).apply {
            action = MedicineReminderReceiver.ACTION_SNOOZE_MEDICINE
            putExtra(EXTRA_MEDICINE_ID, medicine.id)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 300000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val textInstructions = if (medicine.instructions.isNotBlank()) {
            " - Note: ${medicine.instructions}"
        } else {
            ""
        }

        val largeIcon = if (!medicine.imagePath.isNullOrBlank()) {
            try {
                val file = java.io.File(medicine.imagePath)
                if (file.exists()) {
                    android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }

        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setContentTitle("Medicine Reminder: ${medicine.name}")
            .setContentText("Please take ${medicine.dosage}$textInstructions")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM) 
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true) 
            .setAutoCancel(false)
            .setOngoing(true) 
            .setColor(Color.parseColor(medicine.colorHex)) 
            .setColorized(false) 
            .addAction(R.drawable.ic_check, "Take", takePendingIntent)
            .addAction(R.drawable.ic_clear, "Skip", skipPendingIntent)
            .addAction(R.drawable.ic_snooze, "Snooze (10m)", snoozePendingIntent)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(largeIcon)
                    .bigLargeIcon(null as android.graphics.Bitmap?)
                    .setSummaryText("Dosage: ${medicine.dosage}")
            )
        }

        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            
            e.printStackTrace()
        }
    }

    
    fun cancelNotification(medicineId: Long) {
        notificationManager.cancel(medicineId.toInt())
    }
}
