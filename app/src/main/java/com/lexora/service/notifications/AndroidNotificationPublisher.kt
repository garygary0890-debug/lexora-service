package com.lexora.service.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.lexora.service.MainActivity
import com.lexora.service.core.model.NotificationDelivery
import com.lexora.service.core.model.ServiceNotificationPriority

class AndroidNotificationPublisher(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    fun canPublish(): Boolean = Build.VERSION.SDK_INT < 33 ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun publish(delivery: NotificationDelivery): Boolean {
        if (!canPublish()) return false
        ensureChannels()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_NOTIFICATION_ENTITY_TYPE, delivery.entityType)
            putExtra(EXTRA_NOTIFICATION_ENTITY_ID, delivery.entityId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            delivery.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, channelFor(delivery))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(delivery.title)
            .setContentText(delivery.body)
            .setStyle(Notification.BigTextStyle().bigText(delivery.body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .build()
        manager.notify(delivery.id.hashCode(), notification)
        return true
    }

    private fun ensureChannels() {
        val channels = listOf(
            NotificationChannel(CHANNEL_CRITICAL, "Критические события", NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(CHANNEL_WARNING, "Предупреждения", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(CHANNEL_REMINDERS, "Напоминания", NotificationManager.IMPORTANCE_DEFAULT),
        )
        manager.createNotificationChannels(channels)
    }

    private fun channelFor(delivery: NotificationDelivery): String = when {
        delivery.priority == ServiceNotificationPriority.CRITICAL -> CHANNEL_CRITICAL
        delivery.eventCode.contains("REMINDER", ignoreCase = true) -> CHANNEL_REMINDERS
        else -> CHANNEL_WARNING
    }

    companion object {
        const val CHANNEL_CRITICAL = "service_critical"
        const val CHANNEL_WARNING = "service_warning"
        const val CHANNEL_REMINDERS = "service_reminders"
        const val EXTRA_NOTIFICATION_ENTITY_TYPE = "lexora.notification.entityType"
        const val EXTRA_NOTIFICATION_ENTITY_ID = "lexora.notification.entityId"
    }
}
