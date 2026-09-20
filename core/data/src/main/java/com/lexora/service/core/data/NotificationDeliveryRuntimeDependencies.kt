package com.lexora.service.core.data

import com.lexora.service.core.domain.NotificationDeliveryOperations

object NotificationDeliveryRuntimeDependencies {
    @Volatile private var operations: NotificationDeliveryOperations? = null

    fun install(value: NotificationDeliveryOperations) {
        operations = value
    }

    fun operations(): NotificationDeliveryOperations =
        requireNotNull(operations) { "NotificationDeliveryOperations is not installed" }
}
