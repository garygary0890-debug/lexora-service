package com.lexora.service

import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileSettingsContentTest {
    @Test
    fun adds_new_section() {
        val result = upsertProfileSection(emptyList(), null, "Безопасность", "Смена пароля")
        assertEquals(1, result.size)
        assertEquals("Безопасность", result.single().title)
        assertEquals("Смена пароля", result.single().body)
    }

    @Test
    fun edits_existing_section_without_changing_order() {
        val existing = listOf(
            ProfileContentSection("Безопасность", "Смена пароля"),
            ProfileContentSection("Уведомления", "Задачи"),
        )
        val result = upsertProfileSection(
            existing,
            existing.first(),
            "Безопасность аккаунта",
            "Пароль и устройства",
        )
        assertEquals(2, result.size)
        assertEquals("Безопасность аккаунта", result[0].title)
        assertEquals("Пароль и устройства", result[0].body)
        assertEquals(existing[0].id, result[0].id)
        assertEquals(existing[1], result[1])
    }

    @Test
    fun removes_section_by_id() {
        val existing = listOf(
            ProfileContentSection("Безопасность", "Смена пароля"),
            ProfileContentSection("Уведомления", "Задачи"),
        )
        assertEquals(listOf(existing[1]), removeProfileSection(existing, existing.first().id))
    }
}
