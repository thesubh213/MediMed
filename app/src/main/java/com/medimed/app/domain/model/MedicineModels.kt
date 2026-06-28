package com.medimed.app.domain.model

enum class FrequencyType {
    DAILY,     
    WEEKDAYS,  
    INTERVAL   
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
    val frequencyData: String, 
    val timesOfDay: List<String>, 
    val startDate: Long,
    val endDate: Long? = null,
    val instructions: String = "",
    val stockCount: Int? = null,
    val stockLowThreshold: Int? = null,
    val isActive: Boolean = true,
    val colorHex: String = "#a53860", 
    val imagePath: String? = null
)

data class MedicineLog(
    val id: Long = 0,
    val medicineId: Long,
    val scheduledTime: Long, 
    val status: LogStatus,
    val actionTime: Long, 
    val notes: String? = null
)

data class DoseSchedule(
    val medicine: Medicine,
    val scheduledTime: Long, 
    val status: LogStatus? = null, 
    val logId: Long? = null,
    val actionTime: Long? = null
)
