package com.lexora.service.core.model

enum class ServiceComponentType { WORK, MATERIAL }

data class ServiceRecipe(
    val id: String,
    val organizationId: String,
    val serviceCatalogItemId: String,
    val name: String,
    val description: String? = null,
    val durationMinutes: Int? = null,
    val active: Boolean = true,
    val syncState: SyncState = SyncState.PENDING_CREATE,
)

data class ServiceRecipeComponent(
    val id: String,
    val organizationId: String,
    val recipeId: String,
    val type: ServiceComponentType,
    val code: String? = null,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "шт.",
    val unitCostMinor: Long = 0,
    val sortOrder: Int = 0,
    val active: Boolean = true,
    val syncState: SyncState = SyncState.PENDING_CREATE,
) {
    val totalCostMinor: Long
        get() = (unitCostMinor * quantity).toLong()
}

data class ServiceRecipeSummary(
    val recipe: ServiceRecipe,
    val components: List<ServiceRecipeComponent>,
) {
    val calculatedCostMinor: Long
        get() = components.filter { it.active }.sumOf { it.totalCostMinor }
}
