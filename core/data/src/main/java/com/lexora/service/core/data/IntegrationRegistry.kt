package com.lexora.service.core.data

enum class IntegrationState { PLANNED, READY_FOR_CONFIGURATION, CONNECTED, ERROR }

data class IntegrationDescriptor(
    val id: String,
    val title: String,
    val description: String,
    val state: IntegrationState,
    val supportsIdempotency: Boolean = true,
    val supportsRetry: Boolean = true,
)

fun defaultIntegrationRegistry(): List<IntegrationDescriptor> = listOf(
    IntegrationDescriptor(
        id = "server_api",
        title = "Lexora Service Server API",
        description = "Версионируемый транспорт двусторонней offline-first синхронизации данных.",
        state = IntegrationState.READY_FOR_CONFIGURATION,
    ),
    IntegrationDescriptor(
        id = "document_exchange",
        title = "Импорт и экспорт документов",
        description = "Файловый обмен документами и внешние ЭДО-сценарии с журналом операций.",
        state = IntegrationState.READY_FOR_CONFIGURATION,
    ),
    IntegrationDescriptor(
        id = "client_cabinet",
        title = "Клиентский кабинет / онлайн-запись",
        description = "Web-кабинет и публичная онлайн-запись без обязательной установки приложения.",
        state = IntegrationState.READY_FOR_CONFIGURATION,
    ),
    IntegrationDescriptor(
        id = "accounting_1c",
        title = "1С / бухгалтерия",
        description = "Адаптер обмена контрагентами, счетами, оплатами, услугами и документами без привязки доменной модели к формату 1С.",
        state = IntegrationState.READY_FOR_CONFIGURATION,
    ),
    IntegrationDescriptor(
        id = "erp",
        title = "ERP",
        description = "Обобщённый адаптер заказов, справочников, закупок, остатков и финансовых операций.",
        state = IntegrationState.READY_FOR_CONFIGURATION,
    ),
    IntegrationDescriptor(
        id = "telephony",
        title = "Телефония",
        description = "Входящие/исходящие события звонков и создание обращения из интеграционного канала.",
        state = IntegrationState.READY_FOR_CONFIGURATION,
    ),
    IntegrationDescriptor(
        id = "messengers",
        title = "Мессенджеры",
        description = "Адаптер сообщений и уведомлений с нормализованными DTO и контролем повторной доставки.",
        state = IntegrationState.READY_FOR_CONFIGURATION,
    ),
    IntegrationDescriptor(
        id = "iot_telemetry",
        title = "IoT / телеметрия",
        description = "Приём метрик и событий оборудования с возможностью создания сервисного обращения по правилу.",
        state = IntegrationState.READY_FOR_CONFIGURATION,
    ),
    IntegrationDescriptor(
        id = "payments",
        title = "Онлайн-оплата",
        description = "Платёжный адаптер; секреты провайдера должны храниться только на серверной стороне.",
        state = IntegrationState.PLANNED,
    ),
    IntegrationDescriptor(
        id = "external_apps",
        title = "Внешние приложения Lexora и партнёры",
        description = "Общий адаптерный контур для интеграций без зависимости отраслевых модулей от конкретного поставщика API.",
        state = IntegrationState.READY_FOR_CONFIGURATION,
    ),
)
