package com.medimed.app

import android.app.Application
import com.medimed.app.data.local.MediMedDatabase
import com.medimed.app.data.repository.MedicineRepositoryImpl
import com.medimed.app.domain.repository.MedicineRepository
import com.medimed.app.domain.scheduler.MedicineAlarmScheduler
import com.medimed.app.notification.NotificationHelper

class MediMedApp : Application() {

    
    val database: MediMedDatabase by lazy {
        MediMedDatabase.getDatabase(this)
    }

    val repository: MedicineRepository by lazy {
        MedicineRepositoryImpl(database.medicineDao())
    }

    val scheduler: MedicineAlarmScheduler by lazy {
        MedicineAlarmScheduler(this)
    }

    val notificationHelper: NotificationHelper by lazy {
        NotificationHelper(this)
    }

    override fun onCreate() {
        super.onCreate()
        
        
        notificationHelper
    }
}
