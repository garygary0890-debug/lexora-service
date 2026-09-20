package com.lexora.service.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AdditionalWorkApprovalEventEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class WorkOrderCommercialDatabase : RoomDatabase() {
    abstract fun dao(): WorkOrderCommercialDao

    companion object {
        @Volatile private var instance: WorkOrderCommercialDatabase? = null

        fun create(context: Context): WorkOrderCommercialDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                WorkOrderCommercialDatabase::class.java,
                "lexora-service-work-order-commercial.db",
            ).build().also { instance = it }
        }
    }
}
