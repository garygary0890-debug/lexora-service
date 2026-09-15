package com.lexora.service.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "service_catalog",
    indices = [Index("organizationId"), Index(value = ["organizationId", "code"], unique = true), Index("active")],
)
data class ServiceCatalogItemEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val code: String,
    val name: String,
    val category: String?,
    val unit: String,
    val durationMinutes: Int?,
    val active: Boolean,
    val syncState: String,
    val updatedAtEpochMs: Long,
)

@Entity(
    tableName = "price_lists",
    indices = [Index("organizationId"), Index("active"), Index("effectiveFromEpochMs")],
)
data class PriceListEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val name: String,
    val currency: String,
    val effectiveFromEpochMs: Long,
    val effectiveToEpochMs: Long?,
    val active: Boolean,
    val syncState: String,
    val updatedAtEpochMs: Long,
)

@Entity(
    tableName = "price_list_items",
    indices = [Index("priceListId"), Index("serviceCatalogItemId"), Index(value = ["priceListId", "serviceCatalogItemId"], unique = true)],
)
data class PriceListItemEntity(
    @PrimaryKey val id: String,
    val priceListId: String,
    val serviceCatalogItemId: String,
    val priceMinor: Long,
    val syncState: String,
    val updatedAtEpochMs: Long,
)
