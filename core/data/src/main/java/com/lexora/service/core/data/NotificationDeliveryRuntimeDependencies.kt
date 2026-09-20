package com.lexora.service.core.data

import com.lexora.service.core.domain.NotificationDeliveryOperations
import com.lexora.service.core.model.NotificationDelivery

object NotificationDeliveryRuntimeDependencies {
    @Volatile private var operations: NotificationDeliveryOperations? = null
    @Volatile private var scheduler: ((NotificationDelivery) -> Unit)? = null

    fun install(
        value: NotificationDeliveryOperations,
        schedule: ((NotificationDelivery) -> Unit)? = null,
    ) {
        operations = value
        scheduler = schedule
    }

    fun operations(): NotificationDeliveryOperations =
        requireNotNull(operations) { "NotificationDeliveryOperations is not installed" }

    fun schedule(delivery: NotificationDelivery) {
        scheduler?.invoke(delivery)
    }
}
