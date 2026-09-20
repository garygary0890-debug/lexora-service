package com.lexora.service.core.domain

sealed interface PushProviderResult {
    data object Success : PushProviderResult
    data class TransientFailure(val error: String) : PushProviderResult
    data class PermanentFailure(val error: String) : PushProviderResult
    data class InvalidToken(val error: String) : PushProviderResult
}

enum class PushDeliveryDecision { DELIVERED, RETRY, TERMINAL_FAILURE, DISABLE_TOKEN }

data class PushDeliveryOutcome(
    val decision: PushDeliveryDecision,
    val nextAttemptAtEpochMs: Long? = null,
    val disableToken: Boolean = false,
)

object PushDeliveryPolicy {
    const val MAX_ATTEMPTS = 5
    private const val BASE_RETRY_MS = 60_000L
    private const val MAX_RETRY_MS = 15L * 60L * 1000L

    fun afterFailure(
        attemptCount: Int,
        nowEpochMs: Long,
        result: PushProviderResult,
    ): PushDeliveryOutcome = when (result) {
        PushProviderResult.Success -> PushDeliveryOutcome(PushDeliveryDecision.DELIVERED)
        is PushProviderResult.InvalidToken -> PushDeliveryOutcome(
            decision = PushDeliveryDecision.DISABLE_TOKEN,
            disableToken = true,
        )
        is PushProviderResult.PermanentFailure -> PushDeliveryOutcome(PushDeliveryDecision.TERMINAL_FAILURE)
        is PushProviderResult.TransientFailure -> {
            val nextAttemptNumber = attemptCount + 1
            if (nextAttemptNumber >= MAX_ATTEMPTS) {
                PushDeliveryOutcome(PushDeliveryDecision.TERMINAL_FAILURE)
            } else {
                val shift = attemptCount.coerceIn(0, 20)
                val delay = (BASE_RETRY_MS * (1L shl shift)).coerceAtMost(MAX_RETRY_MS)
                PushDeliveryOutcome(
                    decision = PushDeliveryDecision.RETRY,
                    nextAttemptAtEpochMs = nowEpochMs + delay,
                )
            }
        }
    }
}
