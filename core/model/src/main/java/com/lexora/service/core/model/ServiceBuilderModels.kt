package com.lexora.service.core.model

data class ServiceDefinition(
    val id: String,
    val organizationId: String,
    val code: String,
    val name: String,
    val categoryCode: String,
    val moduleCode: String? = null,
    val priceListItemId: String? = null,
    val durationMinutes: Int? = null,
    val allowedBranchIds: Set<String> = emptySet(),
    val allowedPostTypes: Set<String> = emptySet(),
    val technologyCardId: String? = null,
    val qualityChecklistCode: String? = null,
    val active: Boolean = true,
)

data class ServiceMaterialRequirement(
    val serviceDefinitionId: String,
    val materialId: String,
    val quantity: Double,
    val unit: String,
    val required: Boolean = true,
)
