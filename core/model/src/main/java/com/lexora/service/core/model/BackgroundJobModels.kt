package com.lexora.service.core.model

enum class BackgroundJobType { NOTIFICATION, DOCUMENT_GENERATION, BACKUP, INTEGRATION, SYNC_RETRY }
enum class BackgroundJobStatus { QUEUED, RUNNING, RETRY_WAIT, SUCCEEDED, FAILED, CANCELLED }

data class BackgroundJob(
    val id: String,
    val organizationId: String?,
    val type: BackgroundJobType,
    val payloadRef: String?,
    val status: BackgroundJobStatus = BackgroundJobStatus.QUEUED,
    val attempt: Int = 0,
    val maxAttempts: Int = 5,
    val scheduledAtEpochMs: Long,
    val startedAtEpochMs: Long? = null,
    val finishedAtEpochMs: Long? = null,
    val lastErrorCode: String? = null,
)

data class BackgroundJobRetryPolicy(
    val initialDelaySeconds: Long = 30,
    val multiplier: Double = 2.0,
    val maximumDelaySeconds: Long = 3600,
)
