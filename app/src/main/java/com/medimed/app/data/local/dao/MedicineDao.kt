package com.medimed.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.medimed.app.data.local.entity.MedicineEntity
import com.medimed.app.data.local.entity.MedicineLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {

    // Medicine operations
    @Query("SELECT * FROM medicines ORDER BY name ASC")
    fun getAllMedicines(): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveMedicines(): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE id = :id LIMIT 1")
    suspend fun getMedicineById(id: Long): MedicineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: MedicineEntity): Long

    @Update
    suspend fun updateMedicine(medicine: MedicineEntity)

    @Delete
    suspend fun deleteMedicine(medicine: MedicineEntity)

    // Medicine Logs operations
    @Query("SELECT * FROM medicine_logs ORDER BY scheduledTime DESC")
    fun getAllLogs(): Flow<List<MedicineLogEntity>>

    @Query("SELECT * FROM medicine_logs WHERE scheduledTime BETWEEN :start AND :end ORDER BY scheduledTime ASC")
    fun getLogsInRange(start: Long, end: Long): Flow<List<MedicineLogEntity>>

    @Query("SELECT * FROM medicine_logs WHERE medicineId = :medicineId ORDER BY scheduledTime DESC")
    fun getLogsForMedicine(medicineId: Long): Flow<List<MedicineLogEntity>>

    @Query("SELECT * FROM medicine_logs WHERE medicineId = :medicineId AND scheduledTime = :scheduledTime LIMIT 1")
    suspend fun getLogForDose(medicineId: Long, scheduledTime: Long): MedicineLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MedicineLogEntity): Long

    @Query("DELETE FROM medicine_logs WHERE medicineId = :medicineId AND scheduledTime = :scheduledTime")
    suspend fun deleteLogForDose(medicineId: Long, scheduledTime: Long)

    @Delete
    suspend fun deleteLog(log: MedicineLogEntity)
}
