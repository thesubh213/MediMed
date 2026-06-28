package com.medimed.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.medimed.app.domain.model.FrequencyType
import com.medimed.app.domain.model.Medicine

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dosage: String,
    val frequencyType: String, 
    val frequencyData: String,  
    val timesOfDay: String,     
    val startDate: Long,
    val endDate: Long?,
    val instructions: String,
    val stockCount: Int?,
    val stockLowThreshold: Int?,
    val isActive: Boolean,
    val colorHex: String,
    val imagePath: String?
) {
    fun toDomain(): Medicine {
        return Medicine(
            id = id,
            name = name,
            dosage = dosage,
            frequencyType = FrequencyType.valueOf(frequencyType),
            frequencyData = frequencyData,
            timesOfDay = if (timesOfDay.isBlank()) emptyList() else timesOfDay.split(","),
            startDate = startDate,
            endDate = endDate,
            instructions = instructions,
            stockCount = stockCount,
            stockLowThreshold = stockLowThreshold,
            isActive = isActive,
            colorHex = colorHex,
            imagePath = imagePath
        )
    }

    companion object {
        fun fromDomain(medicine: Medicine): MedicineEntity {
            return MedicineEntity(
                id = medicine.id,
                name = medicine.name,
                dosage = medicine.dosage,
                frequencyType = medicine.frequencyType.name,
                frequencyData = medicine.frequencyData,
                timesOfDay = medicine.timesOfDay.joinToString(","),
                startDate = medicine.startDate,
                endDate = medicine.endDate,
                instructions = medicine.instructions,
                stockCount = medicine.stockCount,
                stockLowThreshold = medicine.stockLowThreshold,
                isActive = medicine.isActive,
                colorHex = medicine.colorHex,
                imagePath = medicine.imagePath
            )
        }
    }
}
