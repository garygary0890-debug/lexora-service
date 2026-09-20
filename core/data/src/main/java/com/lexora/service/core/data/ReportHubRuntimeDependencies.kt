package com.lexora.service.core.data

import com.lexora.service.core.domain.ReportHubOperations

object ReportHubRuntimeDependencies {
    @Volatile private var operations: ReportHubOperations? = null

    fun install(value: ReportHubOperations) { operations = value }

    fun operations(): ReportHubOperations =
        requireNotNull(operations) { "ReportHubOperations is not installed" }
}
