package com.lexora.service.core.data

import android.content.Context
import com.lexora.service.core.database.AuditEventEntity
import com.lexora.service.core.database.LexoraServiceDatabase
import com.lexora.service.core.database.ReferenceDataDao
import com.lexora.service.core.database.ReferenceDirectoryEntity
import com.lexora.service.core.database.ReferenceDirectoryItemEntity
import com.lexora.service.core.database.ServiceDao
import com.lexora.service.core.model.ReferenceDirectory
import com.lexora.service.core.model.ReferenceDirectoryItem
import com.lexora.service.core.model.SyncState
import java.util.UUID

class ReferenceDataRepository(
    private val dao: ReferenceDataDao,
    private val serviceDao: ServiceDao,
) {
    suspend fun directories(organizationId: String): List<ReferenceDirectory> =
        dao.directories(organizationId).map(ReferenceDirectoryEntity::toModel)

    suspend fun items(directoryId: String): List<ReferenceDirectoryItem> =
        dao.items(directoryId).map(ReferenceDirectoryItemEntity::toModel)

    suspend fun createDirectory(
        organizationId: String,
        userId: String,
        code: String,
        name: String,
        description: String? = null,
    ): ReferenceDirectory {
        val normalizedCode = code.trim().uppercase()
        require(normalizedCode.isNotBlank())
        require(name.isNotBlank())
        require(dao.directories(organizationId).none { it.code.equals(normalizedCode, ignoreCase = true) })
        val now = System.currentTimeMillis()
        val entity = ReferenceDirectoryEntity(
            id = UUID.randomUUID().toString(),
            organizationId = organizationId,
            code = normalizedCode,
            name = name.trim(),
            description = description?.trim()?.ifBlank { null },
            system = false,
            active = true,
            syncState = SyncState.PENDING_CREATE.name,
            updatedAtEpochMs = now,
        )
        dao.upsertDirectory(entity)
        audit(organizationId, userId, "REFERENCE_DIRECTORY", entity.id, "CREATE", "${entity.code} · ${entity.name}", now)
        return entity.toModel()
    }

    suspend fun createItem(
        organizationId: String,
        userId: String,
        directoryId: String,
        code: String,
        name: String,
    ): ReferenceDirectoryItem {
        val directory = requireNotNull(dao.directory(directoryId))
        require(directory.organizationId == organizationId)
        val normalizedCode = code.trim().uppercase()
        require(normalizedCode.isNotBlank())
        require(name.isNotBlank())
        val existing = dao.items(directoryId)
        require(existing.none { it.code.equals(normalizedCode, ignoreCase = true) })
        val now = System.currentTimeMillis()
        val entity = ReferenceDirectoryItemEntity(
            id = UUID.randomUUID().toString(),
            organizationId = organizationId,
            directoryId = directoryId,
            code = normalizedCode,
            name = name.trim(),
            sortOrder = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1,
            active = true,
            syncState = SyncState.PENDING_CREATE.name,
            updatedAtEpochMs = now,
        )
        dao.upsertItem(entity)
        audit(organizationId, userId, "REFERENCE_DIRECTORY_ITEM", entity.id, "CREATE", "${directory.code}/${entity.code} · ${entity.name}", now)
        return entity.toModel()
    }

    suspend fun setDirectoryActive(organizationId: String, userId: String, id: String, active: Boolean) {
        val directory = requireNotNull(dao.directory(id))
        require(directory.organizationId == organizationId)
        require(!directory.system || active)
        val now = System.currentTimeMillis()
        dao.setDirectoryActive(id, active, now)
        audit(organizationId, userId, "REFERENCE_DIRECTORY", id, if (active) "ACTIVATE" else "DEACTIVATE", directory.name, now)
    }

    suspend fun setItemActive(organizationId: String, userId: String, id: String, active: Boolean) {
        val item = requireNotNull(dao.item(id))
        require(item.organizationId == organizationId)
        val now = System.currentTimeMillis()
        dao.setItemActive(id, active, now)
        audit(organizationId, userId, "REFERENCE_DIRECTORY_ITEM", id, if (active) "ACTIVATE" else "DEACTIVATE", item.name, now)
    }

    private suspend fun audit(organizationId: String, userId: String, type: String, id: String, action: String, summary: String, now: Long) {
        serviceDao.insertAuditEvent(AuditEventEntity(UUID.randomUUID().toString(), organizationId, userId, type, id, action, summary, now))
    }

    companion object {
        fun create(context: Context): ReferenceDataRepository {
            val db = LexoraServiceDatabase.create(context.applicationContext)
            return ReferenceDataRepository(db.referenceDataDao(), db.serviceDao())
        }
    }
}

private fun ReferenceDirectoryEntity.toModel() = ReferenceDirectory(
    id, organizationId, code, name, description, system, active, SyncState.valueOf(syncState),
)

private fun ReferenceDirectoryItemEntity.toModel() = ReferenceDirectoryItem(
    id, organizationId, directoryId, code, name, sortOrder, active, SyncState.valueOf(syncState),
)
