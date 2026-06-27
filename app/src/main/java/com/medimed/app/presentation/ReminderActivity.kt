package com.medimed.app.presentation

import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medimed.app.MediMedApp
import com.medimed.app.domain.model.LogStatus
import com.medimed.app.domain.model.Medicine
import com.medimed.app.domain.model.MedicineLog
import com.medimed.app.domain.scheduler.MedicineAlarmScheduler
import com.medimed.app.notification.NotificationHelper
import com.medimed.app.presentation.component.MedicineImage
import com.medimed.app.presentation.theme.LocalDimens
import com.medimed.app.presentation.theme.MediMedTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ReminderActivity : ComponentActivity() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wake screen and show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        or android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        // Dismiss keyguard
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager.requestDismissKeyguard(this, null)
        }

        val medicineId = intent.getLongExtra(NotificationHelper.EXTRA_MEDICINE_ID, -1L)
        val scheduledTime = intent.getLongExtra(NotificationHelper.EXTRA_SCHEDULED_TIME, -1L)

        val app = application as MediMedApp
        val repository = app.repository
        val scheduler = app.scheduler
        val notificationHelper = app.notificationHelper

        // Start Ringtone & Vibration
        startAlarm()

        setContent {
            MediMedTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ReminderScreen(
                        medicineId = medicineId,
                        scheduledTime = scheduledTime,
                        repository = repository,
                        scheduler = scheduler,
                        notificationHelper = notificationHelper,
                        onActionDone = {
                            stopAlarm()
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun startAlarm() {
        try {
            // Ringtone
            val alert: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(applicationContext, alert)?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                play()
            }

            // Vibrator
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            val pattern = longArrayOf(0, 800, 800)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlarm() {
        try {
            ringtone?.stop()
            vibrator?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }
}

@Composable
fun ReminderScreen(
    medicineId: Long,
    scheduledTime: Long,
    repository: com.medimed.app.domain.repository.MedicineRepository,
    scheduler: MedicineAlarmScheduler,
    notificationHelper: NotificationHelper,
    onActionDone: () -> Unit
) {
    val dimens = LocalDimens.current
    var medicine by remember { mutableStateOf<Medicine?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Auto-dismiss after 5 minutes to prevent indefinite alarm ringing
    LaunchedEffect(Unit) {
        delay(5 * 60 * 1000L)
        onActionDone()
    }

    // Load medicine details
    LaunchedEffect(medicineId) {
        if (medicineId != -1L) {
            val result = repository.getMedicineById(medicineId)
            if (result != null) {
                medicine = result
            } else {
                loadFailed = true
            }
        } else {
            loadFailed = true
        }
    }

    when {
        loadFailed -> {
            // Medicine was deleted or ID is invalid — show graceful error state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(dimens.spacingXxxl),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(dimens.spacingXxxl),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚠️",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(dimens.spacingLg))
                        Text(
                            text = "Medication Not Found",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(dimens.spacingSm))
                        Text(
                            text = "This medication may have been deleted. The reminder has been dismissed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(dimens.spacingXl))
                        Button(
                            onClick = onActionDone,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(dimens.largeTouchTarget),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Dismiss", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        medicine == null -> {
            // Loading state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(dimens.spacingLg))
                    Text(
                        text = "Loading medication...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }

        else -> {
            val med = medicine!!
            val medColor = try {
                Color(android.graphics.Color.parseColor(med.colorHex))
            } catch (e: Exception) {
                MaterialTheme.colorScheme.primary
            }

            val isDark = false
            val baseBgColor = MaterialTheme.colorScheme.background
            val topGradientColor = medColor.copy(alpha = if (isDark) 0.16f else 0.10f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(topGradientColor, baseBgColor)
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(dimens.spacingXxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top warning visual
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = dimens.spacingXxl + dimens.spacingXs)
                    ) {
                        Text(
                            text = "MEDICINE REMINDER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = medColor,
                            letterSpacing = 2.5.sp
                        )
                        Spacer(modifier = Modifier.height(dimens.spacingSm))
                        Text(
                            text = "It's time for your medication",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Medicine visual Card Container
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = dimens.spacingXxl),
                        elevation = CardDefaults.cardElevation(defaultElevation = dimens.elevationHigh),
                        border = androidx.compose.foundation.BorderStroke(dimens.borderMedium, medColor.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(dimens.spacingXxl),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Medicine image
                            Card(
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(dimens.borderMedium, medColor.copy(alpha = 0.5f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = dimens.elevationMedium)
                            ) {
                                MedicineImage(
                                    imagePath = med.imagePath,
                                    modifier = Modifier
                                        .size(260.dp)
                                        .padding(dimens.spacingXs)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(dimens.spacingXl))
                            
                            Text(
                                text = med.name,
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Pill-shaped dosage badge
                            Box(
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.small)
                                    .background(medColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 18.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = med.dosage,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 18.sp,
                                    color = medColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                            
                            // Instructions Box
                            if (med.instructions.isNotBlank()) {
                                Spacer(modifier = Modifier.height(dimens.spacingLg))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = dimens.spacingSm)
                                ) {
                                    Text(
                                        text = "💡 Instructions:\n${med.instructions}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(dimens.spacingMd)
                                    )
                                }
                            }
                        }
                    }

                    // Action Buttons
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = dimens.spacingMd),
                        verticalArrangement = Arrangement.spacedBy(dimens.spacingLg)
                    ) {
                        // TAKE Button
                        Button(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val log = MedicineLog(
                                            medicineId = med.id,
                                            scheduledTime = scheduledTime,
                                            status = LogStatus.TAKEN,
                                            actionTime = System.currentTimeMillis()
                                        )
                                        repository.insertLog(log)
                                        // Update inventory with floor guard
                                        med.stockCount?.let { currentStock ->
                                            if (currentStock > 0) {
                                                repository.updateMedicine(med.copy(stockCount = currentStock - 1))
                                            }
                                        }
                                        notificationHelper.cancelNotification(med.id)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    scope.launch(Dispatchers.Main) { onActionDone() }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = medColor),
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(dimens.xlTouchTarget)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(dimens.iconLg))
                            Spacer(modifier = Modifier.width(dimens.spacingSm))
                            Text("TAKE MEDICINE", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.White, letterSpacing = 0.5.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(dimens.spacingLg)
                        ) {
                            // SKIP Button
                            OutlinedButton(
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            val log = MedicineLog(
                                                medicineId = med.id,
                                                scheduledTime = scheduledTime,
                                                status = LogStatus.SKIPPED,
                                                actionTime = System.currentTimeMillis()
                                            )
                                            repository.insertLog(log)
                                            notificationHelper.cancelNotification(med.id)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        scope.launch(Dispatchers.Main) { onActionDone() }
                                    }
                                },
                                border = androidx.compose.foundation.BorderStroke(dimens.borderThick, MaterialTheme.colorScheme.outline),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(dimens.largeTouchTarget)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(dimens.spacingSm - 2.dp))
                                Text("SKIP", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            }

                            // SNOOZE Button
                            Button(
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            // Schedule snooze alarm
                                            val snoozeTime = System.currentTimeMillis() + 10 * 60 * 1000L
                                            scheduler.scheduleSnoozeAlarm(med.id, scheduledTime, snoozeTime)
                                            notificationHelper.cancelNotification(med.id)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        scope.launch(Dispatchers.Main) { onActionDone() }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(dimens.largeTouchTarget)
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = null)
                                Spacer(modifier = Modifier.width(dimens.spacingSm - 2.dp))
                                Text("SNOOZE", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}
