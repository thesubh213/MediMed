package com.medimed.app.domain.model

enum class FrequencyType {
    DAILY,     // Every day
    WEEKDAYS,  // Specific days of week (e.g. Monday, Wednesday, Friday)
    INTERVAL   // Every X hours (e.g. every 8 hours)
}

enum class LogStatus {
    TAKEN,
    SKIPPED
}

data class Medicine(
    val id: Long = 0,
    val name: String,
    val dosage: String,
    val frequencyType: FrequencyType,
    val frequencyData: String, // Comma-separated days (e.g., "Monday,Wednesday") or interval in hours (e.g. "8")
    val timesOfDay: List<String>, // Format: "HH:mm" (e.g., "08:00", "20:00")
    val startDate: Long,
    val endDate: Long? = null,
    val instructions: String = "",
    val stockCount: Int? = null,
    val stockLowThreshold: Int? = null,
    val isActive: Boolean = true,
    val colorHex: String = "#a53860", // Default to Berry Crush
    val imagePath: String? = null
)

data class MedicineLog(
    val id: Long = 0,
    val medicineId: Long,
    val scheduledTime: Long, // Epoch millisecond representing when the dose was due
    val status: LogStatus,
    val actionTime: Long, // Epoch millisecond representing when action was recorded
    val notes: String? = null
)

data class DoseSchedule(
    val medicine: Medicine,
    val scheduledTime: Long, // Scheduled time for this specific dose
    val status: LogStatus? = null, // TAKEN, SKIPPED, or null if pending
    val logId: Long? = null,
    val actionTime: Long? = null
)
