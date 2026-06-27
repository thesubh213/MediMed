package com.medimed.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.medimed.app.data.local.dao.MedicineDao
import com.medimed.app.data.local.entity.MedicineEntity
import com.medimed.app.data.local.entity.MedicineLogEntity

@Database(
    entities = [MedicineEntity::class, MedicineLogEntity::class],
    version = 2,
    exportSchema = false
)
abstract class MediMedDatabase : RoomDatabase() {

    abstract fun medicineDao(): MedicineDao

    companion object {
        @Volatile
        private var INSTANCE: MediMedDatabase? = null

        fun getDatabase(context: Context): MediMedDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MediMedDatabase::class.java,
                    "medimed_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
