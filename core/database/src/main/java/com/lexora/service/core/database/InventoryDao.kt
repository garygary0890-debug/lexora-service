package com.lexora.service.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_locations WHERE organizationId = :organizationId AND (:branchId IS NULL OR branchId = :branchId) ORDER BY name")
    suspend fun locations(organizationId: String, branchId: String? = null): List<InventoryLocationEntity>

    @Query("SELECT * FROM inventory_locations WHERE id = :id AND organizationId = :organizationId LIMIT 1")
    suspend fun location(organizationId: String, id: String): InventoryLocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocation(value: InventoryLocationEntity)

    @Query("SELECT * FROM inventory_items WHERE organizationId = :organizationId AND (:includeInactive = 1 OR active = 1) ORDER BY name")
    suspend fun items(organizationId: String, includeInactive: Boolean = false): List<InventoryItemEntity>

    @Query("SELECT * FROM inventory_items WHERE id = :id AND organizationId = :organizationId LIMIT 1")
    suspend fun item(organizationId: String, id: String): InventoryItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(value: InventoryItemEntity)

    @Query("SELECT * FROM inventory_balances WHERE organizationId = :organizationId AND (:locationId IS NULL OR locationId = :locationId) ORDER BY locationId, itemId")
    suspend fun balances(organizationId: String, locationId: String? = null): List<InventoryBalanceEntity>

    @Query("SELECT * FROM inventory_balances WHERE organizationId = :organizationId AND locationId = :locationId AND itemId = :itemId LIMIT 1")
    suspend fun balance(organizationId: String, locationId: String, itemId: String): InventoryBalanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBalance(value: InventoryBalanceEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMovement(value: InventoryMovementEntity)

    @Query("SELECT * FROM inventory_movements WHERE organizationId = :organizationId AND (:itemId IS NULL OR itemId = :itemId) AND (:locationId IS NULL OR locationId = :locationId OR targetLocationId = :locationId) ORDER BY occurredAtEpochMs DESC")
    suspend fun movements(organizationId: String, itemId: String? = null, locationId: String? = null): List<InventoryMovementEntity>

    @Transaction
    suspend fun applyMovement(value: InventoryMovementEntity) {
        require(value.quantity > 0.0) { "Количество должно быть больше нуля" }
        val item = item(value.organizationId, value.itemId) ?: error("Материал не найден")
        require(item.active) { "Материал неактивен" }
        require(location(value.organizationId, value.locationId)?.active == true) { "Склад не найден или неактивен" }

        val source = balance(value.organizationId, value.locationId, value.itemId)
            ?: InventoryBalanceEntity(value.organizationId, value.locationId, value.itemId, 0.0, 0.0)
        val available = source.quantity - source.reservedQuantity

        when (value.type) {
            "RECEIPT" -> upsertBalance(source.copy(quantity = source.quantity + value.quantity))
            "ISSUE", "WRITE_OFF" -> {
                require(available + EPS >= value.quantity) { "Недостаточно свободного остатка" }
                upsertBalance(source.copy(quantity = source.quantity - value.quantity))
            }
            "RESERVATION" -> {
                require(available + EPS >= value.quantity) { "Недостаточно свободного остатка для резерва" }
                require(!value.requestId.isNullOrBlank() || !value.workOrderDocumentId.isNullOrBlank()) { "Резерв должен быть связан с заказом или заявкой" }
                upsertBalance(source.copy(reservedQuantity = source.reservedQuantity + value.quantity))
            }
            "RELEASE_RESERVATION" -> {
                require(source.reservedQuantity + EPS >= value.quantity) { "Освобождаемое количество превышает резерв" }
                upsertBalance(source.copy(reservedQuantity = (source.reservedQuantity - value.quantity).coerceAtLeast(0.0)))
            }
            "TRANSFER" -> {
                val targetId = requireNotNull(value.targetLocationId) { "Для перемещения нужен склад назначения" }
                require(targetId != value.locationId) { "Склад назначения должен отличаться от склада отправления" }
                require(location(value.organizationId, targetId)?.active == true) { "Склад назначения не найден или неактивен" }
                require(available + EPS >= value.quantity) { "Недостаточно свободного остатка для перемещения" }
                val target = balance(value.organizationId, targetId, value.itemId)
                    ?: InventoryBalanceEntity(value.organizationId, targetId, value.itemId, 0.0, 0.0)
                upsertBalance(source.copy(quantity = source.quantity - value.quantity))
                upsertBalance(target.copy(quantity = target.quantity + value.quantity))
            }
            "ADJUSTMENT" -> {
                require(source.reservedQuantity <= value.quantity + EPS) { "Новый остаток не может быть меньше уже зарезервированного" }
                upsertBalance(source.copy(quantity = value.quantity))
            }
            else -> error("Неподдерживаемый тип складского движения: ${value.type}")
        }
        insertMovement(value)
    }

    companion object { private const val EPS = 0.000001 }
}
