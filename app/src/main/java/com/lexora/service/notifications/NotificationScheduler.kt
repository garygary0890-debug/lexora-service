package com.lexora.service.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.lexora.service.core.model.NotificationDelivery
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    fun schedule(context: Context, delivery: NotificationDelivery, nowEpochMs: Long = System.currentTimeMillis()) {
        val request = OneTimeWorkRequestBuilder<NotificationDeliveryWorker>()
            .setInputData(
                Data.Builder()
                    .putString(NotificationDeliveryWorker.KEY_DELIVERY_ID, delivery.id)
                    .build(),
            )
            .setInitialDelay(NotificationSchedulePolicy.initialDelayMillis(delivery, nowEpochMs), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            NotificationSchedulePolicy.uniqueWorkName(delivery.id),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun scheduleAll(context: Context, deliveries: Iterable<NotificationDelivery>, nowEpochMs: Long = System.currentTimeMillis()) {
        deliveries.forEach { schedule(context, it, nowEpochMs) }
    }
}
