package com.medimed.app.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.medimed.app.MediMedApp
import com.medimed.app.presentation.screen.AddEditMedicineScreen
import com.medimed.app.presentation.screen.AdherenceHistoryScreen
import com.medimed.app.presentation.screen.DashboardScreen
import com.medimed.app.presentation.screen.SettingsScreen
import com.medimed.app.presentation.theme.MediMedTheme
import com.medimed.app.presentation.viewmodel.MainViewModel
import com.medimed.app.presentation.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    // Permission launcher for notifications
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Notification reminder permission active", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Reminders won't show unless notification permissions are enabled in system settings.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inject repositories manually from MediMedApp
        val app = application as MediMedApp
        val factory = MainViewModelFactory(app.repository, app.scheduler)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        // Handle permissions
        checkAndRequestPermissions()

        setContent {
            MediMedTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppMainLayout(viewModel)
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        // Notification permissions on API 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Exact alarm permissions on API 31+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                // We ask politely by directing the user when they navigate to Settings,
                // or show a toast to warn them exact scheduling is not permitted by default.
                Toast.makeText(
                    this,
                    "To receive exact on-time reminders, please ensure exact alarms are permitted for MediMed.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

enum class MainTab {
    DASHBOARD,
    HISTORY,
    SETTINGS
}

@Composable
fun AppMainLayout(viewModel: MainViewModel) {
    var activeTab by remember { mutableStateOf(MainTab.DASHBOARD) }
    var showAddScreen by remember { mutableStateOf(false) }

    if (showAddScreen) {
        AddEditMedicineScreen(
            viewModel = viewModel,
            onBack = { showAddScreen = false }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                ) {
                    // Dashboard Navigation
                    NavigationBarItem(
                        selected = activeTab == MainTab.DASHBOARD,
                        onClick = { activeTab = MainTab.DASHBOARD },
                        label = { Text("Dose List", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Daily schedule list") }
                    )

                    // History Navigation
                    NavigationBarItem(
                        selected = activeTab == MainTab.HISTORY,
                        onClick = { activeTab = MainTab.HISTORY },
                        label = { Text("History", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Medication adherence history logs") }
                    )

                    // Settings Navigation
                    NavigationBarItem(
                        selected = activeTab == MainTab.SETTINGS,
                        onClick = { activeTab = MainTab.SETTINGS },
                        label = { Text("Settings", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "App settings and local backup options") }
                    )
                }
            }
        ) { paddingValues ->
            val modifier = Modifier.padding(paddingValues)

            // Crossfade between tabs for smooth transitions
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                },
                label = "tabTransition"
            ) { tab ->
                when (tab) {
                    MainTab.DASHBOARD -> {
                        DashboardScreen(
                            viewModel = viewModel,
                            onAddMedicineClick = { showAddScreen = true },
                            modifier = modifier
                        )
                    }
                    MainTab.HISTORY -> {
                        AdherenceHistoryScreen(
                            viewModel = viewModel,
                            modifier = modifier
                        )
                    }
                    MainTab.SETTINGS -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            modifier = modifier
                        )
                    }
                }
            }
        }
    }
}
