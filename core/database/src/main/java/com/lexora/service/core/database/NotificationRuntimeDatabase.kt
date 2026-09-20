package com.lexora.service.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [NotificationPreferenceEntity::class, NotificationDeliveryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class NotificationRuntimeDatabase : RoomDatabase() {
    abstract fun notificationDeliveryDao(): NotificationDeliveryDao

    companion object {
        @Volatile private var instance: NotificationRuntimeDatabase? = null

        fun create(context: Context): NotificationRuntimeDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                NotificationRuntimeDatabase::class.java,
                "lexora-service-notification-runtime.db",
            ).build().also { instance = it }
        }
    }
}
