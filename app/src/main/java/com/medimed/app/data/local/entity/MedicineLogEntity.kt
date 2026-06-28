package com.medimed.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.medimed.app.domain.model.LogStatus
import com.medimed.app.domain.model.MedicineLog

@Entity(
    tableName = "medicine_logs",
    foreignKeys = [
        ForeignKey(
            entity = MedicineEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["medicineId"])]
)
data class MedicineLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicineId: Long,
    val scheduledTime: Long,
    val status: String, 
    val actionTime: Long,
    val notes: String?
) {
    fun toDomain(): MedicineLog {
        return MedicineLog(
            id = id,
            medicineId = medicineId,
            scheduledTime = scheduledTime,
            status = LogStatus.valueOf(status),
            actionTime = actionTime,
            notes = notes
        )
    }

    companion object {
        fun fromDomain(log: MedicineLog): MedicineLogEntity {
            return MedicineLogEntity(
                id = log.id,
                medicineId = log.medicineId,
                scheduledTime = log.scheduledTime,
                status = log.status.name,
                actionTime = log.actionTime,
                notes = log.notes
            )
        }
    }
}
