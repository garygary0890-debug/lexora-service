package com.lexora.service.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BusinessSupplierEntity::class,
        BusinessPurchaseOrderEntity::class,
        BusinessPurchaseOrderLineEntity::class,
        BusinessPurchaseReceiptEntity::class,
        BusinessPurchaseReceiptLineEntity::class,
        ManagedPaymentEntity::class,
        PaymentOperationEntity::class,
        LinkedServiceTaskEntity::class,
        BusinessAuditEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class BusinessOperationsDatabase : RoomDatabase() {
    abstract fun businessOperationsDao(): BusinessOperationsDao

    companion object {
        @Volatile private var instance: BusinessOperationsDatabase? = null

        fun create(context: Context): BusinessOperationsDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                BusinessOperationsDatabase::class.java,
                "lexora-service-business-operations.db",
            ).build().also { instance = it }
        }
    }
}
