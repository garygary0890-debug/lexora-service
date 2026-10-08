package com.lexora.service.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun LexoraTheme(
    selection: LexoraThemeSelection = LexoraThemeSelection(dark = isSystemInDarkTheme()),
    content: @Composable () -> Unit,
) {
    val primary = Color(if (selection.dark) selection.family.darkPrimary else selection.family.lightPrimary)
    val surface = Color(if (selection.dark) selection.family.darkSurface else selection.family.lightSurface)
    MaterialTheme(
        colorScheme = if (selection.dark) {
            darkColorScheme(primary = primary, secondary = primary, tertiary = primary, surface = surface, background = surface)
        } else {
            lightColorScheme(primary = primary, secondary = primary, tertiary = primary, surface = surface, background = surface)
        },
        content = content,
    )
}
