# MediMed

MediMed is an offline-first, privacy-focused medication reminder and daily dose tracking application for Android. Built with modern native technologies, it ensures your sensitive health logs stay strictly on your local device.

## Key Features

- **Daily Schedule & Logs**: View today's medication tasks and log them as "Taken" or "Skipped" with a single tap.
- **Reliable Reminders**: Integration with Android's `AlarmManager` and `BroadcastReceiver` systems triggers exact alerts on time, even through lock screens and System Alerts overlay overrides.
- **Detailed History Logs**: Keep track of overall adherence rates and compliance history panels.
- **Local Data Portability**: Export and import your schedule and logs as raw JSON backups.
- **Strict Privacy**: 100% offline, zero network permissions, and zero third-party analytics or trackers.

---

## Design System

MediMed implements a customized, warm, Material Design 3 color palette for high contrast, readability, and a premium visual feel:

- **Soft Apricot** (`#F9DBBD`): Card highlights, surface variant background accents.
- **Cotton Candy** (`#FFA5AB`): Interactive action triggers and primary brand identifiers.
- **Blush Rose** (`#DA627D`): Secondary brand highlights and borders.
- **Berry Crush** (`#A53860`): Main primary theme actions.
- **Night Bordeaux** (`#450920`): High-contrast text color, primary text elements, and deep headers.

---

## Architecture & Technology Stack

- **UI Layer**: Jetpack Compose using dynamic themes (`MediMedTheme`) and smooth animated transitions.
- **Storage**: Room database for storing schedules, medication inventory details, and historical adherence logs.
- **Scheduling**: WorkManager, AlarmManager, BroadcastReceiver, and system notifications for exact reminder delivery.
- **Language**: 100% Kotlin utilizing Coroutines and StateFlows for reactive data bindings.

---

## Getting Started

### Prerequisites
- **JDK**: Version 21 or later
- **Android SDK**: API 34+ compile capability

### Building
Compile the debug application package using the Gradle wrapper:

```bash
./gradlew assembleDebug
```

The compiled package can be found at `app/build/outputs/apk/debug/app-debug.apk`.
