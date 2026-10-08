package com.lexora.service.core.designsystem

import android.content.Context

enum class LexoraThemeFamily(
    val title: String,
    val lightPrimary: Long,
    val darkPrimary: Long,
    val lightSurface: Long,
    val darkSurface: Long,
) {
    PEARL("Жемчуг", 0xFF526172, 0xFFD1D9E5, 0xFFF8F7F4, 0xFF202329),
    GRAPHITE("Графит", 0xFF41464F, 0xFFCFD3DA, 0xFFF4F4F3, 0xFF191B1F),
    BURGUNDY("Бургунди", 0xFF8A344B, 0xFFFFB1C2, 0xFFFFF7F7, 0xFF29191E),
    EMERALD("Изумруд", 0xFF286B55, 0xFF83D7B4, 0xFFF5FAF7, 0xFF17231E),
    SAPPHIRE("Сапфир", 0xFF315E9A, 0xFFA9C7FF, 0xFFF5F8FD, 0xFF17202E),
    AMETHYST("Аметист", 0xFF684A8E, 0xFFD2B9FF, 0xFFFAF7FD, 0xFF211B2A),
    PETROL("Петроль", 0xFF176A72, 0xFF83DDE2, 0xFFF3FAFA, 0xFF162427),
    STEEL("Сталь", 0xFF536875, 0xFFB5CCD8, 0xFFF5F8F9, 0xFF1B2428),
    GOLD("Золото", 0xFF82621C, 0xFFF2CE78, 0xFFFCF9F0, 0xFF292316),
    SILVER("Серебро", 0xFF62666B, 0xFFD0D2D5, 0xFFF8F8F8, 0xFF222326),
    BRONZE("Бронза", 0xFF82543A, 0xFFE9B897, 0xFFFBF7F4, 0xFF2A201A),
}

data class LexoraThemeSelection(
    val family: LexoraThemeFamily = LexoraThemeFamily.PEARL,
    val dark: Boolean = false,
) {
    fun save(context: Context) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
            .putString(KEY_FAMILY, family.name)
            .putBoolean(KEY_DARK, dark)
            .apply()
    }

    companion object {
        private const val PREFERENCES = "lexora_theme"
        private const val KEY_FAMILY = "family"
        private const val KEY_DARK = "dark"

        fun read(context: Context): LexoraThemeSelection {
            val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            val family = runCatching {
                LexoraThemeFamily.valueOf(preferences.getString(KEY_FAMILY, null).orEmpty())
            }.getOrDefault(LexoraThemeFamily.PEARL)
            return LexoraThemeSelection(family, preferences.getBoolean(KEY_DARK, false))
        }
    }
}
