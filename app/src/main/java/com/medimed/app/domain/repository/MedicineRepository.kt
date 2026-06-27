package com.medimed.app.domain.repository

import com.medimed.app.domain.model.Medicine
import com.medimed.app.domain.model.MedicineLog
import kotlinx.coroutines.flow.Flow

interface MedicineRepository {
    fun getAllMedicines(): Flow<List<Medicine>>
    fun getActiveMedicines(): Flow<List<Medicine>>
    suspend fun getMedicineById(id: Long): Medicine?
    suspend fun insertMedicine(medicine: Medicine): Long
    suspend fun updateMedicine(medicine: Medicine)
    suspend fun deleteMedicine(medicine: Medicine)

    fun getAllLogs(): Flow<List<MedicineLog>>
    fun getLogsInRange(start: Long, end: Long): Flow<List<MedicineLog>>
    fun getLogsForMedicine(medicineId: Long): Flow<List<MedicineLog>>
    suspend fun getLogForDose(medicineId: Long, scheduledTime: Long): MedicineLog?
    suspend fun insertLog(log: MedicineLog): Long
    suspend fun deleteLogForDose(medicineId: Long, scheduledTime: Long)
    suspend fun deleteLog(log: MedicineLog)
}
