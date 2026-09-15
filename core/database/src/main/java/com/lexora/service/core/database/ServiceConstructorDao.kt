package com.lexora.service.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ServiceConstructorDao {
    @Query("SELECT * FROM service_recipes WHERE organizationId = :organizationId ORDER BY name")
    suspend fun recipes(organizationId: String): List<ServiceRecipeEntity>

    @Query("SELECT * FROM service_recipes WHERE id = :id LIMIT 1")
    suspend fun recipe(id: String): ServiceRecipeEntity?

    @Query("SELECT * FROM service_recipes WHERE organizationId = :organizationId AND serviceCatalogItemId = :serviceCatalogItemId LIMIT 1")
    suspend fun recipeForService(organizationId: String, serviceCatalogItemId: String): ServiceRecipeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecipe(value: ServiceRecipeEntity)

    @Query("UPDATE service_recipes SET active = :active, syncState = 'PENDING_UPDATE', updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun setRecipeActive(id: String, active: Boolean, updatedAt: Long)

    @Query("SELECT * FROM service_recipe_components WHERE recipeId = :recipeId ORDER BY sortOrder, name")
    suspend fun components(recipeId: String): List<ServiceRecipeComponentEntity>

    @Query("SELECT * FROM service_recipe_components WHERE id = :id LIMIT 1")
    suspend fun component(id: String): ServiceRecipeComponentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertComponent(value: ServiceRecipeComponentEntity)

    @Query("UPDATE service_recipe_components SET active = :active, syncState = 'PENDING_UPDATE', updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun setComponentActive(id: String, active: Boolean, updatedAt: Long)
}
