package com.lexora.service.core.data

enum class IntegrationState { PLANNED, READY_FOR_CONFIGURATION, CONNECTED, ERROR }

data class IntegrationDescriptor(
    val id: String,
    val title: String,
    val description: String,
    val state: IntegrationState,
)

fun defaultIntegrationRegistry(): List<IntegrationDescriptor> = listOf(
    IntegrationDescriptor(
        id = "server_api",
        title = "Lexora Service Server API",
        description = "Транспорт для двусторонней синхронизации offline-first данных.",
        state = IntegrationState.READY_FOR_CONFIGURATION,
    ),
    IntegrationDescriptor(
        id = "document_exchange",
        title = "Импорт и экспорт документов",
        description = "Точка расширения для файлового обмена документами и внешними ЭДО-сценариями.",
        state = IntegrationState.READY_FOR_CONFIGURATION,
    ),
    IntegrationDescriptor(
        id = "client_cabinet",
        title = "Клиентский кабинет / онлайн-запись",
        description = "Зарезервированная интеграционная точка для будущего web-кабинета без установки приложения.",
        state = IntegrationState.PLANNED,
    ),
    IntegrationDescriptor(
        id = "external_apps",
        title = "Внешние приложения Lexora и партнёры",
        description = "Общий адаптерный контур для будущих интеграций без зависимости отраслевых модулей от конкретного API.",
        state = IntegrationState.PLANNED,
    ),
)
