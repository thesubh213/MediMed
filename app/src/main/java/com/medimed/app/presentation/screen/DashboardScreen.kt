package com.medimed.app.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medimed.app.domain.model.DoseSchedule
import com.medimed.app.domain.model.LogStatus
import com.medimed.app.domain.model.Medicine
import com.medimed.app.presentation.component.MedicineImage
import com.medimed.app.presentation.theme.LocalDimens
import com.medimed.app.presentation.theme.StatusTaken
import com.medimed.app.presentation.theme.StatusSkipped
import com.medimed.app.presentation.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onAddMedicineClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = LocalDimens.current
    val selectedDate by viewModel.selectedDate.collectAsState()
    val doses by viewModel.selectedDateDoses.collectAsState()

    val context = LocalContext.current
    var medicineToDelete by remember { mutableStateOf<Medicine?>(null) }
    var hasNotificationPermission by remember { mutableStateOf(true) }
    var hasExactAlarmPermission by remember { mutableStateOf(true) }
    var hasDndPermission by remember { mutableStateOf(true) }
    var hasOverlayPermission by remember { mutableStateOf(true) }

    val checkPermissions = {
        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        hasExactAlarmPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        hasDndPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.isNotificationPolicyAccessGranted
        } else {
            true
        }

        hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddMedicineClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(dimens.spacingLg)
                    .size(dimens.xlTouchTarget),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add new medicine",
                    modifier = Modifier.size(dimens.iconXl)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.spacingXl, vertical = dimens.spacingLg)
            ) {
                Text(
                    text = "MediMed",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Your safe and private medicine manager",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            // Permissions setup banner
            val showPermissionWarning = !hasNotificationPermission || !hasExactAlarmPermission || !hasDndPermission || !hasOverlayPermission
            AnimatedVisibility(
                visible = showPermissionWarning,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)),
                exit = fadeOut(tween(200))
            ) {
                PermissionWarningBanner(
                    hasNotificationPermission = hasNotificationPermission,
                    hasExactAlarmPermission = hasExactAlarmPermission,
                    hasDndPermission = hasDndPermission,
                    hasOverlayPermission = hasOverlayPermission,
                    context = context,
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }

            // Horizontal Date Bar
            DateSelectorBar(
                selectedDate = selectedDate,
                onDateSelected = { viewModel.selectDate(it) }
            )

            Spacer(modifier = Modifier.height(dimens.spacingLg))

            // Progress Summary Card
            DailyProgressCard(doses = doses)

            Spacer(modifier = Modifier.height(dimens.spacingMd))

            // Doses List
            if (doses.isEmpty()) {
                EmptyDoseState(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = dimens.spacingLg, vertical = dimens.spacingSm),
                    verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
                ) {
                    items(doses, key = { "${it.medicine.id}_${it.scheduledTime}" }) { dose ->
                        DoseCard(
                            dose = dose,
                            onTake = { viewModel.takeDose(dose.medicine.id, dose.scheduledTime) },
                            onSkip = { viewModel.skipDose(dose.medicine.id, dose.scheduledTime) },
                            onUndo = { viewModel.deleteDoseLog(dose.medicine.id, dose.scheduledTime) },
                            onDelete = { medicineToDelete = dose.medicine }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    medicineToDelete?.let { medicine ->
        AlertDialog(
            onDismissRequest = { medicineToDelete = null },
            title = { Text("Delete Medication?") },
            text = { Text("Are you sure you want to delete ${medicine.name}? This will permanently remove the medication, all its scheduled doses, history, and notifications. This action cannot be undone.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteMedicine(medicine)
                        medicineToDelete = null
                    }
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { medicineToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ─── Permission Warning Banner ───────────────────────────────────────────────

@Composable
private fun PermissionWarningBanner(
    hasNotificationPermission: Boolean,
    hasExactAlarmPermission: Boolean,
    hasDndPermission: Boolean,
    hasOverlayPermission: Boolean,
    context: Context,
    onRequestNotificationPermission: () -> Unit
) {
    val dimens = LocalDimens.current

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.spacingXl, vertical = dimens.spacingSm),
        border = BorderStroke(dimens.borderMedium, MaterialTheme.colorScheme.error)
    ) {
        Column(modifier = Modifier.padding(dimens.spacingLg)) {
            Text(
                text = "⚠️ Setup Required for Reminders",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(dimens.spacingXs))
            Text(
                text = "To ensure reminders ring reliably at the exact time (even in Do Not Disturb mode or on the Lock Screen), please grant the following permissions:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(dimens.spacingMd))

            if (!hasNotificationPermission) {
                PermissionRow(
                    label = "• Notification display permission",
                    buttonText = "Grant",
                    onClick = onRequestNotificationPermission
                )
            }

            if (!hasExactAlarmPermission) {
                PermissionRow(
                    label = "• Exact Alarm scheduling",
                    buttonText = "Enable",
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                            context.startActivity(intent)
                        }
                    }
                )
            }

            if (!hasDndPermission) {
                PermissionRow(
                    label = "• Do Not Disturb (DND) bypass",
                    buttonText = "Allow",
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                            context.startActivity(intent)
                        }
                    }
                )
            }

            if (!hasOverlayPermission) {
                PermissionRow(
                    label = "• Lock Screen overlay (Draw over apps)",
                    buttonText = "Allow",
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(
    label: String,
    buttonText: String,
    onClick: () -> Unit
) {
    val dimens = LocalDimens.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimens.spacingXs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f)
        )
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            contentPadding = PaddingValues(horizontal = dimens.spacingMd, vertical = dimens.spacingXs),
            modifier = Modifier.height(36.dp),
            shape = MaterialTheme.shapes.extraSmall
        ) {
            Text(buttonText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// ─── Empty State ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyDoseState(modifier: Modifier = Modifier) {
    val dimens = LocalDimens.current

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .padding(dimens.spacingXxl)
                .fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimens.spacingXxxl)
            ) {
                Text(
                    text = "💊",
                    fontSize = 48.sp
                )
                Spacer(modifier = Modifier.height(dimens.spacingLg))
                Text(
                    text = "No medications scheduled",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(dimens.spacingSm))
                Text(
                    text = "Enjoy your day! If you need to add a new medication, tap the + button below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─── Date Selector Bar ───────────────────────────────────────────────────────

@Composable
fun DateSelectorBar(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit
) {
    val dimens = LocalDimens.current
    val today = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val dates = rememberDatesAround(today)
    val listState = rememberLazyListState()

    // Smooth scroll to the selected date whenever selectedDate changes
    LaunchedEffect(selectedDate) {
        val index = dates.indexOfFirst { it.timeInMillis == selectedDate }
        if (index >= 0) {
            // Offset by 2 items so that the selected date is centered in the viewport
            listState.animateScrollToItem(index = (index - 2).coerceAtLeast(0))
        }
    }
    
    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.spacingSm),
        horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm),
        contentPadding = PaddingValues(horizontal = dimens.spacingSm)
    ) {
        items(dates) { date ->
            val isSelected = date.timeInMillis == selectedDate
            val dayOfWeek = SimpleDateFormat("E", Locale.getDefault()).format(date.time)
            val dayOfMonth = SimpleDateFormat("d", Locale.getDefault()).format(date.time)
            val fullDate = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(date.time)
            
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(80.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                    .clickable { onDateSelected(date.timeInMillis) }
                    .padding(dimens.spacingSm)
                    .semantics {
                        contentDescription = if (isSelected) "$fullDate, selected" else fullDate
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Text(
                        text = dayOfWeek.uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = dayOfMonth,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ─── Daily Progress Card ─────────────────────────────────────────────────────

@Composable
fun DailyProgressCard(doses: List<DoseSchedule>) {
    if (doses.isEmpty()) return

    val dimens = LocalDimens.current
    val total = doses.size
    val taken = doses.count { it.status == LogStatus.TAKEN }
    val targetProgress = if (total > 0) taken.toFloat() / total.toFloat() else 0f

    // Animate progress bar smoothly
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 600),
        label = "progressAnimation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.spacingLg),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(dimens.spacingXl)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Today's Adherence",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$taken of $total doses completed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                
                Text(
                    text = "${(targetProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(dimens.spacingMd))
            
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimens.progressHeight)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
            )
        }
    }
}

// ─── Dose Card ───────────────────────────────────────────────────────────────

@Composable
fun DoseCard(
    dose: DoseSchedule,
    onTake: () -> Unit,
    onSkip: () -> Unit,
    onUndo: () -> Unit,
    onDelete: () -> Unit
) {
    val dimens = LocalDimens.current
    val medicineColor = try {
        Color(android.graphics.Color.parseColor(dose.medicine.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(dose.scheduledTime))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (dose.status) {
                LogStatus.TAKEN -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                LogStatus.SKIPPED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                null -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = dimens.elevationLow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Medicine color stripe indicator
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(medicineColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(dimens.spacingLg)
            ) {
                // Header Row (Name + Time)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MedicineImage(
                                imagePath = dose.medicine.imagePath,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.width(dimens.spacingMd))
                            Text(
                                text = dose.medicine.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dose.medicine.dosage,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Top
                    ) {
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(dimens.spacingXs))
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(dimens.minTouchTarget)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete ${dose.medicine.name}",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Instructions
                if (dose.medicine.instructions.isNotBlank()) {
                    Spacer(modifier = Modifier.height(dimens.spacingSm - 2.dp))
                    Text(
                        text = "Instruction: ${dose.medicine.instructions}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                // Low Stock Warning Banner
                dose.medicine.stockCount?.let { stock ->
                    val threshold = dose.medicine.stockLowThreshold ?: 5
                    if (stock <= threshold) {
                        Spacer(modifier = Modifier.height(dimens.spacingSm))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (stock == 0) "⚠️ Out of stock! Refill immediately."
                                else "⚠️ Low stock alert: $stock left.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(dimens.spacingMd))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (dose.status) {
                        null -> {
                            // Skip Button
                            OutlinedButton(
                                onClick = onSkip,
                                modifier = Modifier
                                    .height(dimens.minTouchTarget)
                                    .weight(1f),
                                shape = MaterialTheme.shapes.small,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(dimens.iconSm))
                                Spacer(modifier = Modifier.width(dimens.spacingXs))
                                Text("Skip", fontWeight = FontWeight.Bold)
                            }
                            
                            Spacer(modifier = Modifier.width(dimens.spacingMd))

                            // Take Button
                            Button(
                                onClick = onTake,
                                modifier = Modifier
                                    .height(dimens.minTouchTarget)
                                    .weight(1f),
                                shape = MaterialTheme.shapes.small,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(dimens.iconSm))
                                Spacer(modifier = Modifier.width(dimens.spacingXs))
                                Text("Take", fontWeight = FontWeight.Bold)
                            }
                        }
                        LogStatus.TAKEN -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "✓ Taken",
                                    color = StatusTaken,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                IconButton(
                                    onClick = onUndo,
                                    modifier = Modifier.size(dimens.minTouchTarget)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Undo taken status for ${dose.medicine.name}",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        LogStatus.SKIPPED -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "✕ Skipped",
                                    color = StatusSkipped,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                IconButton(
                                    onClick = onUndo,
                                    modifier = Modifier.size(dimens.minTouchTarget)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Undo skipped status for ${dose.medicine.name}",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

@Composable
fun rememberDatesAround(anchorDateMillis: Long): List<Calendar> {
    return remember(anchorDateMillis) {
        val list = mutableListOf<Calendar>()
        val base = Calendar.getInstance().apply { timeInMillis = anchorDateMillis }
        // We display 15 days before, the day itself, and 15 days after (31 days total)
        for (i in -15..15) {
            val date = Calendar.getInstance().apply {
                timeInMillis = base.timeInMillis
                add(Calendar.DAY_OF_YEAR, i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            list.add(date)
        }
        list
    }
}
