package com.lexora.service.notifications

import com.lexora.service.core.domain.PushProviderResult
import com.lexora.service.core.model.NotificationDelivery
import com.lexora.service.core.model.PushDeviceToken

interface PushProvider {
    val name: String
    val configured: Boolean
    suspend fun send(delivery: NotificationDelivery, token: PushDeviceToken): PushProviderResult
}

object NoConfiguredPushProvider : PushProvider {
    override val name: String = "not-configured"
    override val configured: Boolean = false

    override suspend fun send(delivery: NotificationDelivery, token: PushDeviceToken): PushProviderResult =
        PushProviderResult.PermanentFailure("push_provider_not_configured")
}

object PushProviderRuntime {
    @Volatile private var provider: PushProvider = NoConfiguredPushProvider

    fun install(value: PushProvider) {
        provider = value
    }

    fun current(): PushProvider = provider
}
