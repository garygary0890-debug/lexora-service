package com.lexora.service

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ProfileContentSection(
    val title: String,
    val body: String,
    val id: String = title,
)

internal fun upsertProfileSection(
    sections: List<ProfileContentSection>,
    edited: ProfileContentSection?,
    title: String,
    body: String,
): List<ProfileContentSection> {
    val updated = ProfileContentSection(
        title = title,
        body = body,
        id = edited?.id ?: UUID.randomUUID().toString(),
    )
    return if (edited == null) sections + updated else sections.map { if (it.id == edited.id) updated else it }
}

internal fun removeProfileSection(
    sections: List<ProfileContentSection>,
    id: String,
): List<ProfileContentSection> = sections.filterNot { it.id == id }

fun defaultProfileContentSections(): List<ProfileContentSection> = listOf(
    ProfileContentSection("Безопасность", "Смена пароля, подключённые устройства и завершение всех сессий.", "security"),
    ProfileContentSection("Уведомления", "Задачи, сроки, заявки, выезды, платежи и системные сообщения.", "notifications"),
    ProfileContentSection("Синхронизация и данные", "Статус синхронизации, последняя синхронизация, импорт и экспорт данных.", "sync"),
    ProfileContentSection("Дополнительно", "Язык приложения, резервное копирование, помощь и информация о приложении.", "additional"),
)

object ProfileContentPreferences {
    private const val preferencesName = "lexora_profile_content"
    private const val keyPrefix = "sections_"

    fun readOrDefault(context: Context, userId: String): List<ProfileContentSection> {
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        return if (preferences.contains(keyPrefix + userId)) read(context, userId) else defaultProfileContentSections()
    }

    fun read(context: Context, userId: String): List<ProfileContentSection> {
        val raw = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .getString(keyPrefix + userId, null)
            ?: return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            buildList(json.length()) {
                for (index in 0 until json.length()) {
                    val item = json.getJSONObject(index)
                    add(
                        ProfileContentSection(
                            title = item.optString("title"),
                            body = item.optString("body"),
                            id = item.optString("id"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun write(context: Context, userId: String, sections: List<ProfileContentSection>) {
        val json = JSONArray().apply {
            sections.forEach { section ->
                put(
                    JSONObject()
                        .put("id", section.id)
                        .put("title", section.title)
                        .put("body", section.body),
                )
            }
        }
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit()
            .putString(keyPrefix + userId, json.toString())
            .apply()
    }
}
