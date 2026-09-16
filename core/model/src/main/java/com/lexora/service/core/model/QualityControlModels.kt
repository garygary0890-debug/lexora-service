package com.lexora.service.core.model

enum class QualityInspectionResult { ACCEPTED, REWORK_REQUIRED, REJECTED }

data class QualityChecklistItem(
    val code: String,
    val title: String,
    val required: Boolean = false,
    val passed: Boolean? = null,
    val note: String? = null,
)

data class QualityInspection(
    val id: String,
    val organizationId: String,
    val branchId: String? = null,
    val entityType: String,
    val entityId: String,
    val checklistCode: String,
    val checklistVersion: Int,
    val items: List<QualityChecklistItem>,
    val controllerEmployeeId: String? = null,
    val selfControl: Boolean = false,
    val result: QualityInspectionResult,
    val reason: String? = null,
    val requiredPhotoAttachmentIds: List<String> = emptyList(),
    val inspectedAtEpochMs: Long,
    val reworkStartedAtEpochMs: Long? = null,
    val reworkCompletedAtEpochMs: Long? = null,
)
