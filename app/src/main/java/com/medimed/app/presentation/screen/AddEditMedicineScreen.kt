package com.medimed.app.presentation.screen

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.toMutableStateList
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medimed.app.domain.model.FrequencyType
import com.medimed.app.domain.model.Medicine
import com.medimed.app.presentation.component.MedicineImage
import com.medimed.app.presentation.theme.LocalDimens
import com.medimed.app.presentation.viewmodel.MainViewModel
import java.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMedicineScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dimens = LocalDimens.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    
    var name by rememberSaveable { mutableStateOf("") }
    var dosage by rememberSaveable { mutableStateOf("") }
    var frequencyType by rememberSaveable { mutableStateOf(FrequencyType.DAILY) }
    
    var imagePath by rememberSaveable { mutableStateOf<String?>(null) }
    var tempCameraFile by remember { mutableStateOf<File?>(null) }
    
    
    val weekdays = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) {
        mutableStateListOf<String>()
    }
    val daysList = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    
    var intervalHours by rememberSaveable { mutableStateOf("8") }

    
    val timesOfDay = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) {
        mutableStateListOf("08:00")
    }

    var instructions by rememberSaveable { mutableStateOf("") }

    
    var isTrackStock by rememberSaveable { mutableStateOf(false) }
    var stockCount by rememberSaveable { mutableStateOf("") }
    var stockThreshold by rememberSaveable { mutableStateOf("5") }

    
    val paletteColors = listOf("#a53860", "#da627d", "#ffa5ab", "#f9dbbd", "#450920")
    val paletteNames = listOf("Berry Crush", "Blush Rose", "Cotton Candy", "Soft Apricot", "Night Bordeaux")
    var selectedColor by rememberSaveable { mutableStateOf(paletteColors[0]) }

    
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var dosageError by rememberSaveable { mutableStateOf<String?>(null) }
    var scheduleError by rememberSaveable { mutableStateOf<String?>(null) }
    var timeError by rememberSaveable { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val savedPath = saveUriToInternalStorage(context, uri)
            if (savedPath != null) {
                imagePath = savedPath
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraFile != null) {
            val savedPath = saveUriToInternalStorage(context, android.net.Uri.fromFile(tempCameraFile!!))
            if (savedPath != null) {
                imagePath = savedPath
            }
            tempCameraFile?.delete()
        }
    }

    val triggerCamera = {
        try {
            val dir = File(context.cacheDir, "camera_pics")
            if (!dir.exists()) dir.mkdirs()
            val file = File.createTempFile("cam_", ".jpg", dir)
            tempCameraFile = file
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Add Medication",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(dimens.spacingXl)
        ) {
            
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (it.isNotBlank()) nameError = null
                },
                label = { Text("Medicine Name (e.g., Aspirin)") },
                placeholder = { Text("Enter name") },
                isError = nameError != null,
                supportingText = { nameError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            Spacer(modifier = Modifier.height(dimens.spacingMd))

            
            OutlinedTextField(
                value = dosage,
                onValueChange = {
                    dosage = it
                    if (it.isNotBlank()) dosageError = null
                },
                label = { Text("Dosage (e.g., 1 pill, 10 ml)") },
                placeholder = { Text("Enter dosage") },
                isError = dosageError != null,
                supportingText = { dosageError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            Spacer(modifier = Modifier.height(dimens.spacingLg))

            
            Text(
                "Medicine Image (Optional)",
                style = MaterialTheme.typography.titleSmall,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(dimens.spacingSm))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimens.spacingLg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        MedicineImage(
                            imagePath = imagePath,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(dimens.spacingLg))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(dimens.spacingSm)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            
                            Button(
                                onClick = { triggerCamera() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = MaterialTheme.shapes.extraSmall,
                                modifier = Modifier.weight(1f).height(40.dp),
                                contentPadding = PaddingValues(horizontal = dimens.spacingXs)
                            ) {
                                Text("Camera", style = MaterialTheme.typography.labelMedium)
                            }
                            
                            Button(
                                onClick = { galleryLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = MaterialTheme.shapes.extraSmall,
                                modifier = Modifier.weight(1f).height(40.dp),
                                contentPadding = PaddingValues(horizontal = dimens.spacingXs)
                            ) {
                                Text("Gallery", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        if (imagePath != null) {
                            TextButton(
                                onClick = { imagePath = null },
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    "Remove Image",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimens.spacingLg))

            
            Text(
                "Select Card Color Accent",
                style = MaterialTheme.typography.titleSmall,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(dimens.spacingSm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.spacingMd)
            ) {
                paletteColors.forEachIndexed { index, hex ->
                    val color = Color(android.graphics.Color.parseColor(hex))
                    val isSelected = selectedColor == hex
                    Box(
                        modifier = Modifier
                            .size(dimens.minTouchTarget)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) dimens.borderAccent else dimens.borderThin,
                                color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = hex }
                            .semantics {
                                contentDescription = "${paletteNames[index]} color${if (isSelected) ", selected" else ""}"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(dimens.spacingSm)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimens.spacingXl))

            
            Text(
                "Frequency Schedule",
                style = MaterialTheme.typography.titleSmall,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(dimens.spacingSm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm)
            ) {
                FrequencyType.values().forEach { type ->
                    val isSelected = frequencyType == type
                    Button(
                        onClick = {
                            frequencyType = type
                            scheduleError = null
                        },
                        modifier = Modifier
                            .height(dimens.minTouchTarget)
                            .weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = when (type) {
                                FrequencyType.DAILY -> "Daily"
                                FrequencyType.WEEKDAYS -> "Days"
                                FrequencyType.INTERVAL -> "Interval"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimens.spacingMd))

            
            when (frequencyType) {
                FrequencyType.DAILY -> {
                    Text(
                        "Reminds you every day of the week.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                FrequencyType.WEEKDAYS -> {
                    Column {
                        Text("Select Days:", fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(dimens.spacingSm - 2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            daysList.forEach { day ->
                                val dayLetter = day.take(3)
                                val isDaySelected = weekdays.contains(day)
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isDaySelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable {
                                            if (isDaySelected) weekdays.remove(day) else weekdays.add(day)
                                            if (weekdays.isNotEmpty()) scheduleError = null
                                        }
                                        .semantics {
                                            contentDescription = "$day${if (isDaySelected) ", selected" else ""}"
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayLetter.take(1),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDaySelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        scheduleError?.let {
                            Spacer(modifier = Modifier.height(dimens.spacingXs))
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                FrequencyType.INTERVAL -> {
                    Column {
                        OutlinedTextField(
                            value = intervalHours,
                            onValueChange = {
                                intervalHours = it
                                if (it.toIntOrNull() != null) scheduleError = null
                            },
                            label = { Text("Interval in Hours") },
                            placeholder = { Text("e.g. 8") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = scheduleError != null,
                            supportingText = { scheduleError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimens.spacingXl))

            
            Text(
                "Scheduled Reminder Times",
                style = MaterialTheme.typography.titleSmall,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(dimens.spacingSm - 2.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(dimens.spacingMd)
            ) {
                
                timesOfDay.forEach { time ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = dimens.spacingXs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        
                        val timeParts = time.split(":")
                        val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 0
                        val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                        }
                        val formattedTime = java.text.SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)

                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(start = dimens.spacingSm)
                        )

                        
                        IconButton(
                            onClick = { timesOfDay.remove(time) },
                            modifier = Modifier.size(dimens.minTouchTarget)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete reminder at $formattedTime",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (timesOfDay.isEmpty()) {
                    Text(
                        text = "No times added yet. Add at least one time below.",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(dimens.spacingSm)
                    )
                }

                Spacer(modifier = Modifier.height(dimens.spacingSm))

                
                Button(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                val formattedHour = String.format("%02d", hourOfDay)
                                val formattedMinute = String.format("%02d", minute)
                                val timeString = "$formattedHour:$formattedMinute"
                                if (!timesOfDay.contains(timeString)) {
                                    timesOfDay.add(timeString)
                                    timesOfDay.sort()
                                    timeError = null
                                }
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            false 
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimens.minTouchTarget),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("+ Add Reminder Time", fontWeight = FontWeight.Bold)
                }
            }
            timeError?.let {
                Spacer(modifier = Modifier.height(dimens.spacingXs))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(dimens.spacingXl))

            
            OutlinedTextField(
                value = instructions,
                onValueChange = { instructions = it },
                label = { Text("Instructions / Notes (Optional)") },
                placeholder = { Text("e.g. Take with water after food") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(dimens.spacingLg))

            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isTrackStock = !isTrackStock }
                    .padding(vertical = dimens.spacingSm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(dimens.spacingSm))
                    Column {
                        Text("Track Medicine Stock", fontWeight = FontWeight.Bold)
                        Text(
                            "Alert you when running low on pills",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
                Switch(
                    checked = isTrackStock,
                    onCheckedChange = { isTrackStock = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            AnimatedVisibility(
                visible = isTrackStock,
                enter = expandVertically(tween(250)),
                exit = shrinkVertically(tween(200))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dimens.spacingSm)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(dimens.spacingLg)
                    ) {
                        OutlinedTextField(
                            value = stockCount,
                            onValueChange = { stockCount = it },
                            label = { Text("Pill Count") },
                            placeholder = { Text("e.g. 30") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium
                        )
                        OutlinedTextField(
                            value = stockThreshold,
                            onValueChange = { stockThreshold = it },
                            label = { Text("Low Alert") },
                            placeholder = { Text("e.g. 5") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimens.spacingXxxl))

            
            Button(
                onClick = {
                    
                    var isValid = true
                    if (name.isBlank()) {
                        nameError = "Medicine name is required"
                        isValid = false
                    }
                    if (dosage.isBlank()) {
                        dosageError = "Dosage details are required"
                        isValid = false
                    }
                    if (timesOfDay.isEmpty()) {
                        timeError = "Add at least one reminder time"
                        isValid = false
                    }
                    if (frequencyType == FrequencyType.WEEKDAYS && weekdays.isEmpty()) {
                        scheduleError = "Please select at least one day"
                        isValid = false
                    }
                    if (frequencyType == FrequencyType.INTERVAL && intervalHours.toIntOrNull() == null) {
                        scheduleError = "Please enter a valid hour interval"
                        isValid = false
                    }
                    
                    if (frequencyType == FrequencyType.INTERVAL) {
                        val hours = intervalHours.toIntOrNull()
                        if (hours != null && (hours < 1 || hours > 168)) {
                            scheduleError = "Interval must be between 1 and 168 hours"
                            isValid = false
                        }
                    }

                    if (isValid) {
                        val frequencyDataString = when (frequencyType) {
                            FrequencyType.DAILY -> ""
                            FrequencyType.WEEKDAYS -> weekdays.joinToString(",")
                            FrequencyType.INTERVAL -> intervalHours
                        }

                        val medicine = Medicine(
                            name = name.trim(),
                            dosage = dosage.trim(),
                            frequencyType = frequencyType,
                            frequencyData = frequencyDataString,
                            timesOfDay = timesOfDay.toList(),
                            startDate = System.currentTimeMillis(),
                            instructions = instructions.trim(),
                            stockCount = if (isTrackStock) stockCount.toIntOrNull() ?: 0 else null,
                            stockLowThreshold = if (isTrackStock) stockThreshold.toIntOrNull() ?: 5 else null,
                            isActive = true,
                            colorHex = selectedColor,
                            imagePath = imagePath
                        )

                        viewModel.saveMedicine(medicine)
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimens.largeTouchTarget),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Save Medicine", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            
            Spacer(modifier = Modifier.height(dimens.spacingLg))
        }
    }
}

private fun saveUriToInternalStorage(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        val resolver = context.contentResolver
        val inputStream = resolver.openInputStream(uri) ?: return null
        val dir = File(context.filesDir, "med_images")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "med_${UUID.randomUUID()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
