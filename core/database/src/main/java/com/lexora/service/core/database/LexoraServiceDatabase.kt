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
        ServiceObjectEntity::class,
        EquipmentEntity::class,
        ServiceRequestEntity::class,
        RequestStatusHistoryEntity::class,
    ],
    version = 6,
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
                db.execSQL("CREATE TABLE IF NOT EXISTS service_history (id TEXT NOT NULL PRIMARY KEY, organizationId TEXT NOT NULL, vehicleId TEXT NOT NULL, sourceType TEXT NOT NULL, sourceId TEXT, title TEXT NOT NULL, description TEXT, mileageKm INTEGER, occurredAtEpochMs INTEGER NOT NULL, createdAtEpochMs INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_service_history_organizationId ON service_history(organizationId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_service_history_vehicleId ON service_history(vehicleId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_service_history_occurredAtEpochMs ON service_history(occurredAtEpochMs)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS service_objects (id TEXT NOT NULL PRIMARY KEY, organizationId TEXT NOT NULL, clientId TEXT, name TEXT NOT NULL, address TEXT, accessMode TEXT, responsibleContact TEXT, archived INTEGER NOT NULL, syncState TEXT NOT NULL, createdAtEpochMs INTEGER NOT NULL, updatedAtEpochMs INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_service_objects_organizationId ON service_objects(organizationId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_service_objects_clientId ON service_objects(clientId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS equipment (id TEXT NOT NULL PRIMARY KEY, organizationId TEXT NOT NULL, serviceObjectId TEXT, type TEXT NOT NULL, make TEXT, model TEXT, serialNumber TEXT, inventoryNumber TEXT, barcode TEXT, commissionedNote TEXT, warrantyNote TEXT, archived INTEGER NOT NULL, syncState TEXT NOT NULL, createdAtEpochMs INTEGER NOT NULL, updatedAtEpochMs INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_equipment_organizationId ON equipment(organizationId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_equipment_serviceObjectId ON equipment(serviceObjectId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_equipment_organizationId_serialNumber ON equipment(organizationId, serialNumber)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE branches ADD COLUMN phone TEXT")
                db.execSQL("ALTER TABLE branches ADD COLUMN email TEXT")
                db.execSQL("ALTER TABLE branches ADD COLUMN workSchedule TEXT")
                db.execSQL("ALTER TABLE employees ADD COLUMN phone TEXT")
                db.execSQL("ALTER TABLE employees ADD COLUMN email TEXT")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS service_requests (id TEXT NOT NULL PRIMARY KEY, organizationId TEXT NOT NULL, number TEXT NOT NULL, clientId TEXT, vehicleId TEXT, serviceObjectId TEXT, equipmentId TEXT, branchId TEXT, assigneeEmployeeId TEXT, title TEXT NOT NULL, description TEXT, status TEXT NOT NULL, priority TEXT NOT NULL, plannedAtEpochMs INTEGER, dueAtEpochMs INTEGER, slaDeadlineEpochMs INTEGER, closedAtEpochMs INTEGER, archived INTEGER NOT NULL, syncState TEXT NOT NULL, createdAtEpochMs INTEGER NOT NULL, updatedAtEpochMs INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_service_requests_organizationId ON service_requests(organizationId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_service_requests_clientId ON service_requests(clientId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_service_requests_branchId ON service_requests(branchId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_service_requests_assigneeEmployeeId ON service_requests(assigneeEmployeeId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_service_requests_status ON service_requests(status)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_service_requests_organizationId_number ON service_requests(organizationId, number)")
                db.execSQL("CREATE TABLE IF NOT EXISTS request_status_history (id TEXT NOT NULL PRIMARY KEY, requestId TEXT NOT NULL, fromStatus TEXT, toStatus TEXT NOT NULL, changedByUserId TEXT NOT NULL, changedAtEpochMs INTEGER NOT NULL, comment TEXT)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_request_status_history_requestId ON request_status_history(requestId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_request_status_history_changedAtEpochMs ON request_status_history(changedAtEpochMs)")
            }
        }

        fun create(context: Context): LexoraServiceDatabase =
            Room.databaseBuilder(context.applicationContext, LexoraServiceDatabase::class.java, "lexora-service.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build()
    }
}
