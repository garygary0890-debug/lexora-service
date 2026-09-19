package com.lexora.service

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ServiceSyncScheduler {
    private const val UNIQUE_IMMEDIATE = "lexora-service-sync-immediate"
    private const val UNIQUE_PERIODIC = "lexora-service-sync-periodic"

    fun ensurePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<ServiceSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(networkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun enqueue(context: Context, delayMs: Long = 0L) {
        val builder = OneTimeWorkRequestBuilder<ServiceSyncWorker>()
            .setConstraints(networkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
        if (delayMs > 0L) builder.setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            UNIQUE_IMMEDIATE,
            ExistingWorkPolicy.REPLACE,
            builder.build(),
        )
    }

    private fun networkConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
}
