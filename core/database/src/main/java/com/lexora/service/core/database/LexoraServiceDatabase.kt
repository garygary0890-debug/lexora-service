package com.lexora.service.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        OrganizationEntity::class,
        ClientEntity::class,
        VehicleEntity::class,
        BranchEntity::class,
        EmployeeEntity::class,
        ModuleSettingEntity::class,
        AuditEventEntity::class,
        ServiceHistoryEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class LexoraServiceDatabase : RoomDatabase() {
    abstract fun serviceDao(): ServiceDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE clients ADD COLUMN kpp TEXT")
                db.execSQL("ALTER TABLE clients ADD COLUMN registrationAddress TEXT")
                db.execSQL("ALTER TABLE clients ADD COLUMN actualAddress TEXT")
                db.execSQL("ALTER TABLE clients ADD COLUMN note TEXT")
                db.execSQL("ALTER TABLE clients ADD COLUMN consentPersonalData INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vehicles ADD COLUMN bodyType TEXT")
                db.execSQL("ALTER TABLE vehicles ADD COLUMN color TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS service_history (
                        id TEXT NOT NULL PRIMARY KEY,
                        organizationId TEXT NOT NULL,
                        vehicleId TEXT NOT NULL,
                        sourceType TEXT NOT NULL,
                        sourceId TEXT,
                        title TEXT NOT NULL,
                        description TEXT,
                        mileageKm INTEGER,
                        occurredAtEpochMs INTEGER NOT NULL,
                        createdAtEpochMs INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_service_history_organizationId ON service_history(organizationId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_service_history_vehicleId ON service_history(vehicleId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_service_history_occurredAtEpochMs ON service_history(occurredAtEpochMs)")
            }
        }

        fun create(context: Context): LexoraServiceDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                LexoraServiceDatabase::class.java,
                "lexora-service.db",
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
