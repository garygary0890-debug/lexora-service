package com.lexora.service.core.model

enum class ReferenceEntryStatus { ACTIVE, ARCHIVED }

data class ReferenceCatalog(
    val id: String,
    val organizationId: String,
    val code: String,
    val name: String,
    val moduleCode: String? = null,
    val system: Boolean = false,
    val editable: Boolean = true,
)

data class ReferenceCatalogEntry(
    val id: String,
    val catalogId: String,
    val organizationId: String,
    val code: String,
    val name: String,
    val sortOrder: Int = 0,
    val status: ReferenceEntryStatus = ReferenceEntryStatus.ACTIVE,
    val metadata: Map<String, String> = emptyMap(),
)
