package com.medimed.app.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medimed.app.domain.model.LogStatus
import com.medimed.app.domain.model.Medicine
import com.medimed.app.presentation.component.MedicineImage
import com.medimed.app.presentation.theme.LocalDimens
import com.medimed.app.presentation.theme.StatusTaken
import com.medimed.app.presentation.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdherenceHistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val dimens = LocalDimens.current
    val medicines by viewModel.medicines.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val stats by viewModel.adherenceStats.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Adherence History",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Stats Panel
            AdherenceStatsPanel(
                total = stats.totalDoses,
                taken = stats.takenDoses,
                skipped = stats.skippedDoses,
                rate = stats.complianceRate
            )

            Spacer(modifier = Modifier.height(dimens.spacingLg))

            Text(
                text = "History Logs",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = dimens.spacingXl)
            )

            Spacer(modifier = Modifier.height(dimens.spacingSm))

            if (logs.isEmpty()) {
                EmptyHistoryState(
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(logs, key = { it.id }) { log ->
                        val matchingMedicine = medicines.find { it.id == log.medicineId }
                        HistoryLogCard(
                            log = log,
                            medicine = matchingMedicine,
                            onDeleteClick = {
                                viewModel.deleteDoseLog(log.medicineId, log.scheduledTime)
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─── Empty History State ─────────────────────────────────────────────────────

@Composable
private fun EmptyHistoryState(modifier: Modifier = Modifier) {
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
                    text = "📋",
                    fontSize = 48.sp
                )
                Spacer(modifier = Modifier.height(dimens.spacingLg))
                Text(
                    text = "No history records yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(dimens.spacingSm))
                Text(
                    text = "Your logs will appear here as you record daily doses.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─── Stats Panel ─────────────────────────────────────────────────────────────

@Composable
fun AdherenceStatsPanel(
    total: Int,
    taken: Int,
    skipped: Int,
    rate: Double
) {
    val dimens = LocalDimens.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.spacingLg, vertical = dimens.spacingSm),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = dimens.elevationLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.spacingXl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Overall Compliance",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            
            Text(
                text = "${String.format("%.1f", rate)}%",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(dimens.spacingLg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(label = "Total Doses", count = total.toString())
                StatColumn(label = "Taken", count = taken.toString(), countColor = StatusTaken)
                StatColumn(label = "Skipped", count = skipped.toString(), countColor = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun StatColumn(
    label: String,
    count: String,
    countColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count,
            style = MaterialTheme.typography.titleLarge,
            color = countColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

// ─── History Log Card ────────────────────────────────────────────────────────

@Composable
fun HistoryLogCard(
    log: com.medimed.app.domain.model.MedicineLog,
    medicine: Medicine?,
    onDeleteClick: () -> Unit
) {
    val dimens = LocalDimens.current
    val medicineName = medicine?.name ?: "Deleted Medicine"
    val dosage = medicine?.dosage ?: ""
    val colorHex = medicine?.colorHex ?: "#a53860"
    
    val medicineColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val dateFormat = SimpleDateFormat("EEE, MMM dd, hh:mm a", Locale.getDefault())
    val actionTimeStr = dateFormat.format(Date(log.actionTime))
    val scheduledTimeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(log.scheduledTime))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.medium
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

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MedicineImage(
                            imagePath = medicine?.imagePath,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = medicineName,
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (dosage.isNotBlank()) {
                        Text(
                            text = dosage,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.height(dimens.spacingXs))
                    Text(
                        text = "${if (log.status == LogStatus.TAKEN) "Taken" else "Skipped"} at $actionTimeStr (Scheduled: $scheduledTimeStr)",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (log.status == LogStatus.TAKEN) StatusTaken else MaterialTheme.colorScheme.primary
                    )
                }

                // Undo Button (Delete log entry)
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(dimens.minTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Undo and delete log record for $medicineName",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
