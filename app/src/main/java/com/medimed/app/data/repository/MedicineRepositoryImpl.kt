package com.medimed.app.data.repository

import com.medimed.app.data.local.dao.MedicineDao
import com.medimed.app.data.local.entity.MedicineEntity
import com.medimed.app.data.local.entity.MedicineLogEntity
import com.medimed.app.domain.model.Medicine
import com.medimed.app.domain.model.MedicineLog
import com.medimed.app.domain.repository.MedicineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MedicineRepositoryImpl(private val medicineDao: MedicineDao) : MedicineRepository {

    override fun getAllMedicines(): Flow<List<Medicine>> {
        return medicineDao.getAllMedicines().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getActiveMedicines(): Flow<List<Medicine>> {
        return medicineDao.getActiveMedicines().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getMedicineById(id: Long): Medicine? {
        return medicineDao.getMedicineById(id)?.toDomain()
    }

    override suspend fun insertMedicine(medicine: Medicine): Long {
        return medicineDao.insertMedicine(MedicineEntity.fromDomain(medicine))
    }

    override suspend fun updateMedicine(medicine: Medicine) {
        medicineDao.updateMedicine(MedicineEntity.fromDomain(medicine))
    }

    override suspend fun deleteMedicine(medicine: Medicine) {
        medicineDao.deleteMedicine(MedicineEntity.fromDomain(medicine))
    }

    override fun getAllLogs(): Flow<List<MedicineLog>> {
        return medicineDao.getAllLogs().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getLogsInRange(start: Long, end: Long): Flow<List<MedicineLog>> {
        return medicineDao.getLogsInRange(start, end).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getLogsForMedicine(medicineId: Long): Flow<List<MedicineLog>> {
        return medicineDao.getLogsForMedicine(medicineId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getLogForDose(medicineId: Long, scheduledTime: Long): MedicineLog? {
        return medicineDao.getLogForDose(medicineId, scheduledTime)?.toDomain()
    }

    override suspend fun insertLog(log: MedicineLog): Long {
        return medicineDao.insertLog(MedicineLogEntity.fromDomain(log))
    }

    override suspend fun deleteLogForDose(medicineId: Long, scheduledTime: Long) {
        medicineDao.deleteLogForDose(medicineId, scheduledTime)
    }

    override suspend fun deleteLog(log: MedicineLog) {
        medicineDao.deleteLog(MedicineLogEntity.fromDomain(log))
    }
}
