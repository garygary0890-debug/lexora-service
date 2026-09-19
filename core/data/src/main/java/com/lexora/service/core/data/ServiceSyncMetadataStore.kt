package com.lexora.service.core.data

import android.content.Context

class ServiceSyncMetadataStore(context: Context) : SyncCursorStore {
    private val prefs = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override suspend fun cursor(organizationId: String): Long =
        prefs.getLong(cursorKey(organizationId), 0L)

    override suspend fun saveCursor(organizationId: String, cursor: Long) {
        require(cursor >= 0L)
        prefs.edit().putLong(cursorKey(organizationId), cursor).apply()
    }

    fun version(organizationId: String, entityType: String, entityId: String): Long? {
        val key = versionKey(organizationId, entityType, entityId)
        return if (prefs.contains(key)) prefs.getLong(key, 0L) else null
    }

    fun setVersion(organizationId: String, entityType: String, entityId: String, version: Long) {
        require(version >= 0L)
        prefs.edit().putLong(versionKey(organizationId, entityType, entityId), version).apply()
    }

    fun clearOrganization(organizationId: String) {
        val prefix = "version:$organizationId:"
        val edit = prefs.edit().remove(cursorKey(organizationId))
        prefs.all.keys.filter { it.startsWith(prefix) }.forEach(edit::remove)
        edit.apply()
    }

    private fun cursorKey(organizationId: String) = "cursor:$organizationId"
    private fun versionKey(organizationId: String, entityType: String, entityId: String) =
        "version:$organizationId:$entityType:$entityId"

    private companion object { const val FILE_NAME = "lexora_service_sync_metadata" }
}
