package com.lexora.service.core.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.lexora.service.core.database.AuditEventEntity
import com.lexora.service.core.database.LexoraServiceDatabase
import com.lexora.service.core.database.ServiceDao
import com.lexora.service.core.model.SyncState
import java.io.File
import java.util.UUID

class DocumentTransferRepository(
    private val context: Context,
    private val dao: ServiceDao,
) {
    suspend fun importAttachment(
        organizationId: String,
        userId: String,
        documentId: String,
        sourceUri: Uri,
    ): Result<String> = runCatching {
        val document = requireNotNull(dao.serviceDocument(documentId)) { "Документ не найден" }
        require(document.organizationId == organizationId) { "Документ принадлежит другой организации" }

        val originalName = resolveDisplayName(sourceUri)
            ?.takeIf { it.isNotBlank() }
            ?: "attachment-${System.currentTimeMillis()}"
        val safeName = sanitizeFileName(originalName)
        val directory = File(context.filesDir, "lexora-documents/$documentId").apply { mkdirs() }
        val target = File(directory, safeName)

        context.contentResolver.openInputStream(sourceUri).use { input ->
            requireNotNull(input) { "Не удалось открыть выбранный файл" }
            target.outputStream().use { output -> input.copyTo(output) }
        }

        val now = System.currentTimeMillis()
        dao.upsertServiceDocument(
            document.copy(
                externalFileRef = target.absolutePath,
                syncState = SyncState.PENDING_UPDATE.name,
                updatedAtEpochMs = now,
            ),
        )
        audit(
            organizationId = organizationId,
            userId = userId,
            documentId = documentId,
            action = "IMPORT_ATTACHMENT",
            summary = "${document.number}: $safeName",
            occurredAt = now,
        )
        safeName
    }

    suspend fun exportDocument(
        organizationId: String,
        userId: String,
        documentId: String,
        destinationUri: Uri,
    ): Result<String> = runCatching {
        val document = requireNotNull(dao.serviceDocument(documentId)) { "Документ не найден" }
        require(document.organizationId == organizationId) { "Документ принадлежит другой организации" }
        val attachment = document.externalFileRef?.let(::File)?.takeIf { it.isFile }

        context.contentResolver.openOutputStream(destinationUri, "w").use { output ->
            requireNotNull(output) { "Не удалось открыть файл назначения" }
            if (attachment != null) {
                attachment.inputStream().use { input -> input.copyTo(output) }
            } else {
                val body = buildString {
                    appendLine("Lexora Service")
                    appendLine("Документ: ${document.number}")
                    appendLine("Тип: ${document.type}")
                    appendLine("Статус: ${document.status}")
                    appendLine("Сумма: ${document.totalMinor / 100.0} ${document.currency}")
                    document.requestId?.let { appendLine("Заявка ID: $it") }
                    document.visitId?.let { appendLine("Выезд ID: $it") }
                    document.clientId?.let { appendLine("Клиент ID: $it") }
                    document.note?.let { appendLine("Примечание: $it") }
                }
                output.write(body.toByteArray(Charsets.UTF_8))
            }
        }

        val now = System.currentTimeMillis()
        audit(
            organizationId = organizationId,
            userId = userId,
            documentId = documentId,
            action = "EXPORT_DOCUMENT",
            summary = document.number,
            occurredAt = now,
        )
        attachment?.name ?: "${document.number}.txt"
    }

    suspend fun attachmentName(documentId: String): String? {
        val ref = dao.serviceDocument(documentId)?.externalFileRef ?: return null
        return File(ref).takeIf { it.isFile }?.name ?: ref.substringAfterLast('/').ifBlank { null }
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
    }

    private fun sanitizeFileName(value: String): String {
        val cleaned = value.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        return cleaned.take(160).ifBlank { "attachment-${System.currentTimeMillis()}" }
    }

    private suspend fun audit(
        organizationId: String,
        userId: String,
        documentId: String,
        action: String,
        summary: String,
        occurredAt: Long,
    ) {
        dao.insertAuditEvent(
            AuditEventEntity(
                id = UUID.randomUUID().toString(),
                organizationId = organizationId,
                userId = userId,
                entityType = "SERVICE_DOCUMENT",
                entityId = documentId,
                action = action,
                summary = summary,
                occurredAtEpochMs = occurredAt,
            ),
        )
    }

    companion object {
        fun create(context: Context): DocumentTransferRepository =
            DocumentTransferRepository(
                context = context.applicationContext,
                dao = LexoraServiceDatabase.create(context.applicationContext).serviceDao(),
            )
    }
}
