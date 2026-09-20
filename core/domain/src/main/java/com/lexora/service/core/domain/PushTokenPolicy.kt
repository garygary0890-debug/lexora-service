package com.lexora.service.core.domain

import com.lexora.service.core.model.PushDeviceToken

class PushTokenPolicy {
    fun select(tokens: List<PushDeviceToken>): PushDeviceToken? =
        tokens.asSequence().filter { it.active && it.token.isNotBlank() }
            .maxByOrNull { it.updatedAtEpochMs }
}
