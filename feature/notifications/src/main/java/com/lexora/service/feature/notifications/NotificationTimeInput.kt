package com.lexora.service.feature.notifications

object NotificationTimeInput {
    private val pattern = Regex("^(\\d{2}):(\\d{2})$")

    fun parseMinutes(value: String): Int? {
        val match = pattern.matchEntire(value.trim()) ?: return null
        val hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }

    fun formatMinutes(value: Int?): String = if (value in 0..1439) {
        "%02d:%02d".format(value!! / 60, value % 60)
    } else ""
}
