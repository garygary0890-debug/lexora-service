package com.lexora.service.core.model

data class FeatureDoneChecklist(
    val srvId: String,
    val requirementDefined: Boolean,
    val matchesSpecification: Boolean,
    val buildSuccessful: Boolean,
    val statesHandled: Boolean,
    val permissionsChecked: Boolean,
    val migrationsVerified: Boolean,
    val testsUpdated: Boolean,
    val journalUpdated: Boolean,
    val regressionChecked: Boolean,
    val documentationUpdated: Boolean,
) {
    val complete: Boolean
        get() = requirementDefined && matchesSpecification && buildSuccessful && statesHandled &&
            permissionsChecked && migrationsVerified && testsUpdated && journalUpdated &&
            regressionChecked && documentationUpdated
}
