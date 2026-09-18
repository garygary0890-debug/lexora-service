package com.lexora.service.core.model

enum class CriticalScenarioCode {
    CLIENT_TO_CLOSED_REQUEST,
    OFFLINE_SYNC,
    RESCHEDULE_AND_NOTIFY,
    WORK_AND_MATERIAL_TOTAL,
    ROLE_RESTRICTIONS,
    ORGANIZATION_ISOLATION,
    WASH_FLOW,
    TIRE_FLOW,
    MODULE_DISABLE_ENABLE,
    LOYALTY,
    SERVICE_HISTORY,
    WORK_ORDER_APPROVAL,
    BRANCH_ISOLATION,
    PRICE_HISTORY,
    ARCHIVE_RESTORE,
    LICENSE_RESTRICTION,
    EXTERNAL_READ_ONLY_ACCESS,
}

data class CriticalScenarioDefinition(
    val code: CriticalScenarioCode,
    val title: String,
    val requiredModuleIds: Set<String> = emptySet(),
    val expectedInReleaseSmoke: Boolean = true,
)
