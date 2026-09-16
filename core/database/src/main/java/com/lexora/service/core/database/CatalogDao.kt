package com.lexora.service.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CatalogDao {
    @Query("SELECT * FROM service_catalog WHERE organizationId = :organizationId ORDER BY category, name")
    suspend fun services(organizationId: String): List<ServiceCatalogItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertService(value: ServiceCatalogItemEntity)

    @Query("UPDATE service_catalog SET active = :active, syncState = :syncState, updatedAtEpochMs = :updatedAt WHERE id = :id AND organizationId = (SELECT id FROM organizations WHERE isActive = 1 LIMIT 1)")
    suspend fun setServiceActive(id: String, active: Boolean, syncState: String, updatedAt: Long)

    @Query("SELECT * FROM price_lists WHERE organizationId = :organizationId ORDER BY effectiveFromEpochMs DESC")
    suspend fun priceLists(organizationId: String): List<PriceListEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPriceList(value: PriceListEntity)

    @Query("SELECT * FROM price_list_items WHERE priceListId = :priceListId AND priceListId IN (SELECT id FROM price_lists WHERE organizationId = (SELECT id FROM organizations WHERE isActive = 1 LIMIT 1)) ORDER BY id")
    suspend fun priceListItems(priceListId: String): List<PriceListItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPriceListItem(value: PriceListItemEntity)
}
