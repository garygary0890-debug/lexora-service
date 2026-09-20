package com.lexora.service.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [NotificationPreferenceEntity::class, NotificationDeliveryEntity::class, PushDeviceTokenEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class NotificationRuntimeDatabase : RoomDatabase() {
    abstract fun notificationDeliveryDao(): NotificationDeliveryDao

    companion object {
        @Volatile private var instance: NotificationRuntimeDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS push_device_tokens (id TEXT NOT NULL PRIMARY KEY, organizationId TEXT NOT NULL, userId TEXT NOT NULL, deviceId TEXT NOT NULL, provider TEXT NOT NULL, token TEXT NOT NULL, active INTEGER NOT NULL, updatedAtEpochMs INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_push_device_tokens_organizationId ON push_device_tokens(organizationId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_push_device_tokens_userId ON push_device_tokens(userId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_push_device_tokens_deviceId ON push_device_tokens(deviceId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_push_device_tokens_active ON push_device_tokens(active)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_push_device_tokens_organizationId_userId_deviceId_provider ON push_device_tokens(organizationId, userId, deviceId, provider)")
            }
        }

        fun create(context: Context): NotificationRuntimeDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                NotificationRuntimeDatabase::class.java,
                "lexora-service-notification-runtime.db",
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}
