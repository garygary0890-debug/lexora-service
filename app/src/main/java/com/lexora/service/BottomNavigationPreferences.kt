package com.lexora.service

import android.content.Context

internal object BottomNavigationPreferences {
    private const val PREFERENCES_FILE = "lexora_navigation"
    private fun key(userId: String) = "bottom_sections_$userId"

    fun read(context: Context, userId: String): List<String> =
        context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)
            .getString(key(userId), "")
            .orEmpty()
            .split('|')
            .filter(String::isNotBlank)

    fun write(context: Context, userId: String, routes: List<String>) {
        context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(key(userId), routes.joinToString("|"))
            .apply()
    }
}
