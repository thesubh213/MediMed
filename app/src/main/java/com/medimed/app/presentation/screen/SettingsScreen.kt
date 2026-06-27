package com.medimed.app.presentation.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medimed.app.presentation.theme.LocalDimens
import com.medimed.app.presentation.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dimens = LocalDimens.current
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showImportWarningDialog by remember { mutableStateOf(false) }
    var selectedImportUri by remember { mutableStateOf<Uri?>(null) }

    // System File picker for JSON files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedImportUri = uri
            showImportWarningDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings & Privacy",
                        style = MaterialTheme.typography.titleLarge
                    )
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
                .verticalScroll(scrollState)
                .padding(dimens.spacingXl),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingXl)
        ) {
            // Privacy Box
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier.padding(dimens.spacingLg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Privacy information",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(dimens.iconXl)
                    )
                    Spacer(modifier = Modifier.width(dimens.spacingMd))
                    Column {
                        Text(
                            text = "Offline & Private by Default",
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "All your medical schedules, dosages, and compliance logs are stored securely on this device. No network connections are made, and no tracking is active.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Section: Data Portability
            Text(
                "Data Backup & Restore",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(dimens.spacingLg),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
            ) {
                Text(
                    text = "Export and import your schedule data locally. This is useful for moving your records to a new phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                // Export Button
                Button(
                    onClick = {
                        viewModel.exportData(
                            context = context,
                            onSuccess = { path ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Backup exported to Downloads folder",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            onError = { error ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Export failed: ${error.message}",
                                        duration = SnackbarDuration.Long
                                    )
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimens.minTouchTarget),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(dimens.iconMd))
                    Spacer(modifier = Modifier.width(dimens.spacingSm))
                    Text("Export Data (JSON)", fontWeight = FontWeight.Bold)
                }

                // Import Button
                OutlinedButton(
                    onClick = {
                        filePickerLauncher.launch(arrayOf("application/json", "*/*"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimens.minTouchTarget),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Import Data (JSON)", fontWeight = FontWeight.Bold)
                }
            }

            // Section: System Permissions
            Text(
                "System Notifications & Battery",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(dimens.spacingLg),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingMd)
            ) {
                Text(
                    text = "If reminders are delayed or fail to show, ensure notification permissions are active and battery optimization is set to 'Unrestricted' for MediMed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                // Notification Settings Link
                Button(
                    onClick = {
                        val intent = Intent().apply {
                            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimens.minTouchTarget),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(dimens.iconMd))
                    Spacer(modifier = Modifier.width(dimens.spacingSm))
                    Text("Notification Settings", fontWeight = FontWeight.Bold)
                }

                // Battery Settings Link
                OutlinedButton(
                    onClick = {
                        val intent = Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimens.minTouchTarget),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("App Optimization Details", fontWeight = FontWeight.Bold)
                }
            }

            // Version Indicator
            Text(
                text = "MediMed Version 1.0.0\nDesigned for Care and Privacy.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dimens.spacingMd)
            )
        }
    }

    // Warning Dialog for Importing Data
    if (showImportWarningDialog) {
        AlertDialog(
            onDismissRequest = { showImportWarningDialog = false },
            title = { Text("Overwrite Database?") },
            text = { Text("Importing data will completely overwrite all your current medications, history records, and scheduled notifications. This action cannot be undone.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    onClick = {
                        showImportWarningDialog = false
                        selectedImportUri?.let { uri ->
                            viewModel.importData(
                                context = context,
                                uri = uri,
                                onSuccess = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Database imported successfully!",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                },
                                onError = { msg ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Import failed: $msg",
                                            duration = SnackbarDuration.Long
                                        )
                                    }
                                }
                            )
                        }
                    }
                ) {
                    Text("Overwrite & Import", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportWarningDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
