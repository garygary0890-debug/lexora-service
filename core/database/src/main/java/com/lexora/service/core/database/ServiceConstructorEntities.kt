package com.lexora.service.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "service_recipes",
    indices = [
        Index("organizationId"),
        Index("serviceCatalogItemId"),
        Index(value = ["organizationId", "serviceCatalogItemId"], unique = true),
        Index("active"),
    ],
)
data class ServiceRecipeEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val serviceCatalogItemId: String,
    val name: String,
    val description: String?,
    val durationMinutes: Int?,
    val active: Boolean,
    val syncState: String,
    val updatedAtEpochMs: Long,
)

@Entity(
    tableName = "service_recipe_components",
    indices = [
        Index("organizationId"),
        Index("recipeId"),
        Index("type"),
        Index("active"),
        Index("sortOrder"),
    ],
)
data class ServiceRecipeComponentEntity(
    @PrimaryKey val id: String,
    val organizationId: String,
    val recipeId: String,
    val type: String,
    val code: String?,
    val name: String,
    val quantity: Double,
    val unit: String,
    val unitCostMinor: Long,
    val sortOrder: Int,
    val active: Boolean,
    val syncState: String,
    val updatedAtEpochMs: Long,
)
