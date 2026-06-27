package com.medimed.app.presentation.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.medimed.app.domain.model.DoseSchedule
import com.medimed.app.domain.model.FrequencyType
import com.medimed.app.domain.model.LogStatus
import com.medimed.app.domain.model.Medicine
import com.medimed.app.domain.model.MedicineLog
import com.medimed.app.domain.repository.MedicineRepository
import com.medimed.app.domain.scheduler.MedicineAlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainViewModel(
    private val repository: MedicineRepository,
    private val scheduler: MedicineAlarmScheduler
) : ViewModel() {

    private val TAG = "MainViewModel"

    // Selected day in the calendar dashboard, starting at midnight of that day
    val selectedDate = MutableStateFlow<Long>(getTodayMidnight())

    // All active and inactive medicines in the database
    val medicines = repository.getAllMedicines().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // All completion logs in the database
    val logs = repository.getAllLogs().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Daily schedules for the currently selected date
    val selectedDateDoses: StateFlow<List<DoseSchedule>> = combine(
        selectedDate,
        medicines,
        logs
    ) { date, medicinesList, logsList ->
        computeDosesForDate(date, medicinesList, logsList)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Adherence Stats
    val adherenceStats: StateFlow<AdherenceSummary> = logs.combine(medicines) { logsList, medsList ->
        val total = logsList.size
        val taken = logsList.count { it.status == LogStatus.TAKEN }
        val skipped = logsList.count { it.status == LogStatus.SKIPPED }
        val complianceRate = if (total > 0) (taken.toDouble() / total.toDouble() * 100.0) else 100.0
        AdherenceSummary(total, taken, skipped, complianceRate)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AdherenceSummary(0, 0, 0, 100.0)
    )

    fun selectDate(dateMillis: Long) {
        selectedDate.value = getMidnightOf(dateMillis)
    }

    fun takeDose(medicineId: Long, scheduledTime: Long) {
        viewModelScope.launch {
            try {
                val medicine = repository.getMedicineById(medicineId) ?: return@launch

                // 1. Insert log as TAKEN
                val log = MedicineLog(
                    medicineId = medicineId,
                    scheduledTime = scheduledTime,
                    status = LogStatus.TAKEN,
                    actionTime = System.currentTimeMillis()
                )
                repository.insertLog(log)

                // 2. Decrement stock with floor guard to prevent negative values
                medicine.stockCount?.let { count ->
                    val newCount = (count - 1).coerceAtLeast(0)
                    repository.updateMedicine(medicine.copy(stockCount = newCount))
                }

                // 3. Immediately trigger scheduling of the next dose
                scheduler.scheduleAlarmForMedicine(medicine)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record taken dose for medicineId=$medicineId", e)
            }
        }
    }

    fun skipDose(medicineId: Long, scheduledTime: Long) {
        viewModelScope.launch {
            try {
                val medicine = repository.getMedicineById(medicineId) ?: return@launch

                // 1. Insert log as SKIPPED
                val log = MedicineLog(
                    medicineId = medicineId,
                    scheduledTime = scheduledTime,
                    status = LogStatus.SKIPPED,
                    actionTime = System.currentTimeMillis()
                )
                repository.insertLog(log)

                // 2. Trigger scheduling of the next dose
                scheduler.scheduleAlarmForMedicine(medicine)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record skipped dose for medicineId=$medicineId", e)
            }
        }
    }

    fun deleteDoseLog(medicineId: Long, scheduledTime: Long) {
        viewModelScope.launch {
            try {
                val medicine = repository.getMedicineById(medicineId) ?: return@launch
                val existingLog = repository.getLogForDose(medicineId, scheduledTime)

                // Revert stock count if it was marked TAKEN previously
                if (existingLog?.status == LogStatus.TAKEN) {
                    medicine.stockCount?.let { count ->
                        repository.updateMedicine(medicine.copy(stockCount = count + 1))
                    }
                }

                // Delete log
                repository.deleteLogForDose(medicineId, scheduledTime)

                // Reschedule in case it was the current upcoming alarm
                scheduler.scheduleAlarmForMedicine(medicine)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete dose log for medicineId=$medicineId", e)
            }
        }
    }

    fun saveMedicine(medicine: Medicine) {
        viewModelScope.launch {
            try {
                val id = repository.insertMedicine(medicine)
                val savedMedicine = medicine.copy(id = id)
                scheduler.scheduleAlarmForMedicine(savedMedicine)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save medicine: ${medicine.name}", e)
            }
        }
    }

    fun deleteMedicine(medicine: Medicine) {
        viewModelScope.launch {
            try {
                scheduler.cancelAlarmForMedicine(medicine)
                repository.deleteMedicine(medicine)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete medicine: ${medicine.name}", e)
            }
        }
    }

    // --- Helper calculations ---
    private fun getTodayMidnight(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getMidnightOf(timeMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun computeDosesForDate(
        dateMillis: Long,
        medicinesList: List<Medicine>,
        logsList: List<MedicineLog>
    ): List<DoseSchedule> {
        val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val startOfDay = dateMillis
        val endOfDay = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val dayOfWeekStr = when (dayOfWeek) {
            Calendar.SUNDAY -> "sunday"
            Calendar.MONDAY -> "monday"
            Calendar.TUESDAY -> "tuesday"
            Calendar.WEDNESDAY -> "wednesday"
            Calendar.THURSDAY -> "thursday"
            Calendar.FRIDAY -> "friday"
            Calendar.SATURDAY -> "saturday"
            else -> ""
        }

        val doses = mutableListOf<DoseSchedule>()

        for (medicine in medicinesList) {
            if (!medicine.isActive) continue
            // Check start/end dates
            if (medicine.startDate > endOfDay) continue
            if (medicine.endDate != null && medicine.endDate < startOfDay) continue

            // Check if medicine matches schedule for this date
            var isScheduled = false
            when (medicine.frequencyType) {
                FrequencyType.DAILY -> {
                    isScheduled = true
                }
                FrequencyType.WEEKDAYS -> {
                    val activeDays = medicine.frequencyData.split(",")
                        .map { it.trim().lowercase(Locale.ROOT) }
                    isScheduled = activeDays.contains(dayOfWeekStr)
                }
                FrequencyType.INTERVAL -> {
                    // Check if selected date contains any step times
                    val intervalHours = medicine.frequencyData.toIntOrNull() ?: 0
                    if (intervalHours > 0) {
                        val intervalMillis = intervalHours.toLong() * 60 * 60 * 1000
                        val baseTimeStr = medicine.timesOfDay.firstOrNull() ?: "08:00"
                        val timeParts = baseTimeStr.split(":")
                        val startHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 8
                        val startMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

                        val sequenceStart = Calendar.getInstance().apply {
                            timeInMillis = medicine.startDate
                            set(Calendar.HOUR_OF_DAY, startHour)
                            set(Calendar.MINUTE, startMinute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis

                        if (endOfDay >= sequenceStart) {
                            // Find steps that land on this date
                            val diff = startOfDay - sequenceStart
                            var stepIndex = if (diff < 0) 0 else diff / intervalMillis
                            var candidate = sequenceStart + (stepIndex * intervalMillis)
                            
                            while (candidate <= endOfDay) {
                                if (candidate >= startOfDay && candidate >= medicine.startDate) {
                                    if (medicine.endDate == null || candidate <= medicine.endDate) {
                                        val existingLog = logsList.find { it.medicineId == medicine.id && it.scheduledTime == candidate }
                                        doses.add(
                                            DoseSchedule(
                                                medicine = medicine,
                                                scheduledTime = candidate,
                                                status = existingLog?.status,
                                                logId = existingLog?.id,
                                                actionTime = existingLog?.actionTime
                                            )
                                        )
                                    }
                                }
                                stepIndex++
                                candidate = sequenceStart + (stepIndex * intervalMillis)
                            }
                        }
                    }
                }
            }

            // For DAILY and WEEKDAYS, evaluate times of day
            if (isScheduled && medicine.frequencyType != FrequencyType.INTERVAL) {
                for (timeStr in medicine.timesOfDay) {
                    val timeParts = timeStr.split(":")
                    if (timeParts.size != 2) continue
                    val hour = timeParts[0].toIntOrNull() ?: continue
                    val minute = timeParts[1].toIntOrNull() ?: continue

                    val scheduledTime = Calendar.getInstance().apply {
                        timeInMillis = dateMillis
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    if (scheduledTime >= medicine.startDate && (medicine.endDate == null || scheduledTime <= medicine.endDate)) {
                        val existingLog = logsList.find { it.medicineId == medicine.id && it.scheduledTime == scheduledTime }
                        doses.add(
                            DoseSchedule(
                                medicine = medicine,
                                scheduledTime = scheduledTime,
                                status = existingLog?.status,
                                logId = existingLog?.id,
                                actionTime = existingLog?.actionTime
                            )
                        )
                    }
                }
            }
        }

        return doses.sortedBy { it.scheduledTime }
    }

    // --- JSON Backup Export / Import ---
    fun exportData(context: Context, onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val allMeds = medicines.value
                val allLogs = logs.value

                val rootJson = JSONObject()
                
                // Export Medicines
                val medsArray = JSONArray()
                for (med in allMeds) {
                    val medJson = JSONObject().apply {
                        put("id", med.id)
                        put("name", med.name)
                        put("dosage", med.dosage)
                        put("frequencyType", med.frequencyType.name)
                        put("frequencyData", med.frequencyData)
                        put("timesOfDay", JSONArray(med.timesOfDay))
                        put("startDate", med.startDate)
                        put("endDate", med.endDate ?: JSONObject.NULL)
                        put("instructions", med.instructions)
                        put("stockCount", med.stockCount ?: JSONObject.NULL)
                        put("stockLowThreshold", med.stockLowThreshold ?: JSONObject.NULL)
                        put("isActive", med.isActive)
                        put("colorHex", med.colorHex)
                        put("imagePath", med.imagePath ?: JSONObject.NULL)
                    }
                    medsArray.put(medJson)
                }
                rootJson.put("medicines", medsArray)

                // Export Logs
                val logsArray = JSONArray()
                for (log in allLogs) {
                    val logJson = JSONObject().apply {
                        put("id", log.id)
                        put("medicineId", log.medicineId)
                        put("scheduledTime", log.scheduledTime)
                        put("status", log.status.name)
                        put("actionTime", log.actionTime)
                        put("notes", log.notes ?: JSONObject.NULL)
                    }
                    logsArray.put(logJson)
                }
                rootJson.put("logs", logsArray)

                // Write file to external downloads folder
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "medimed_backup_$timestamp.json"
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val backupFile = File(downloadsDir, fileName)

                FileOutputStream(backupFile).use { fos ->
                    fos.write(rootJson.toString(4).toByteArray())
                }

                onSuccess(backupFile.absolutePath)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export data", e)
                onError(e)
            }
        }
    }

    fun importData(context: Context, uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    onError("Failed to read selection")
                    return@launch
                }
                
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                val rootJson = JSONObject(jsonString)

                if (!rootJson.has("medicines") || !rootJson.has("logs")) {
                    onError("Invalid backup file structure")
                    return@launch
                }

                val medsArray = rootJson.getJSONArray("medicines")
                val logsArray = rootJson.getJSONArray("logs")

                // First wipe current table and cancel alarms
                val oldMeds = repository.getAllMedicines().first()
                for (med in oldMeds) {
                    scheduler.cancelAlarmForMedicine(med)
                    repository.deleteMedicine(med)
                }

                // Insert Medicines
                val medIdMapping = mutableMapOf<Long, Long>() // Map old ID to newly generated ID
                for (i in 0 until medsArray.length()) {
                    val item = medsArray.getJSONObject(i)
                    val oldId = item.getLong("id")
                    
                    val timesArray = item.getJSONArray("timesOfDay")
                    val timesList = mutableListOf<String>()
                    for (j in 0 until timesArray.length()) {
                        timesList.add(timesArray.getString(j))
                    }

                    val med = Medicine(
                        name = item.getString("name"),
                        dosage = item.getString("dosage"),
                        frequencyType = FrequencyType.valueOf(item.getString("frequencyType")),
                        frequencyData = item.getString("frequencyData"),
                        timesOfDay = timesList,
                        startDate = item.getLong("startDate"),
                        endDate = if (item.isNull("endDate")) null else item.getLong("endDate"),
                        instructions = item.optString("instructions", ""),
                        stockCount = if (item.isNull("stockCount")) null else item.getInt("stockCount"),
                        stockLowThreshold = if (item.isNull("stockLowThreshold")) null else item.getInt("stockLowThreshold"),
                        isActive = item.optBoolean("isActive", true),
                        colorHex = item.optString("colorHex", "#a53860"),
                        imagePath = if (item.isNull("imagePath")) null else item.getString("imagePath")
                    )
                    val newId = repository.insertMedicine(med)
                    medIdMapping[oldId] = newId
                }

                // Insert Logs
                for (i in 0 until logsArray.length()) {
                    val item = logsArray.getJSONObject(i)
                    val oldMedId = item.getLong("medicineId")
                    val newMedId = medIdMapping[oldMedId]

                    // If medicine exists, insert log
                    if (newMedId != null) {
                        val log = MedicineLog(
                            medicineId = newMedId,
                            scheduledTime = item.getLong("scheduledTime"),
                            status = LogStatus.valueOf(item.getString("status")),
                            actionTime = item.getLong("actionTime"),
                            notes = if (item.isNull("notes")) null else item.getString("notes")
                        )
                        repository.insertLog(log)
                    }
                }

                // Reschedule all alarms for imported medicines
                scheduler.rescheduleAllAlarms(repository)
                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import data", e)
                onError(e.localizedMessage ?: "Unknown error occurred")
            }
        }
    }
}

data class AdherenceSummary(
    val totalDoses: Int,
    val takenDoses: Int,
    val skippedDoses: Int,
    val complianceRate: Double
)

class MainViewModelFactory(
    private val repository: MedicineRepository,
    private val scheduler: MedicineAlarmScheduler
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository, scheduler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
