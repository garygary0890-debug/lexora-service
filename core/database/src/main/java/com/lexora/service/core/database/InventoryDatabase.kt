package com.lexora.service.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Warehouse storage is isolated from the operational database so warehouse evolution
 * does not force destructive changes to the core Service schema.
 */
@Database(
    entities = [
        InventoryLocationEntity::class,
        InventoryItemEntity::class,
        InventoryBalanceEntity::class,
        InventoryMovementEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class InventoryDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao

    companion object {
        @Volatile private var instance: InventoryDatabase? = null

        fun create(context: Context): InventoryDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                InventoryDatabase::class.java,
                "lexora-service-inventory.db",
            ).build().also { instance = it }
        }
    }
}
