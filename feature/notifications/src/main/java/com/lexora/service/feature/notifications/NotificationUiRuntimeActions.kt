package com.lexora.service.feature.notifications

object NotificationUiRuntimeActions {
    @Volatile private var dndEnabled: ((Boolean) -> Unit)? = null
    @Volatile private var localEnabled: ((Boolean) -> Unit)? = null
    @Volatile private var pushEnabled: ((Boolean) -> Unit)? = null
    @Volatile private var allowCritical: ((Boolean) -> Unit)? = null
    @Volatile private var dndStartChanged: ((String) -> Unit)? = null
    @Volatile private var dndEndChanged: ((String) -> Unit)? = null
    @Volatile private var saveDndWindow: (() -> Unit)? = null

    fun install(
        onDndEnabled: (Boolean) -> Unit,
        onLocalEnabled: (Boolean) -> Unit,
        onPushEnabled: (Boolean) -> Unit,
        onAllowCritical: (Boolean) -> Unit,
        onDndStartChanged: (String) -> Unit,
        onDndEndChanged: (String) -> Unit,
        onSaveDndWindow: () -> Unit,
    ) {
        dndEnabled = onDndEnabled
        localEnabled = onLocalEnabled
        pushEnabled = onPushEnabled
        allowCritical = onAllowCritical
        dndStartChanged = onDndStartChanged
        dndEndChanged = onDndEndChanged
        saveDndWindow = onSaveDndWindow
    }

    fun setDndEnabled(value: Boolean) = dndEnabled?.invoke(value)
    fun setLocalEnabled(value: Boolean) = localEnabled?.invoke(value)
    fun setPushEnabled(value: Boolean) = pushEnabled?.invoke(value)
    fun setAllowCritical(value: Boolean) = allowCritical?.invoke(value)
    fun setDndStart(value: String) = dndStartChanged?.invoke(value)
    fun setDndEnd(value: String) = dndEndChanged?.invoke(value)
    fun saveDndWindow() = saveDndWindow?.invoke()
}
