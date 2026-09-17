package com.lexora.service.core.model

enum class StoredObjectKind { PHOTO, PDF, OFFICE_DOCUMENT, SIGNATURE, EXPORT, OTHER }
enum class StoredObjectState { LOCAL, UPLOAD_PENDING, UPLOADING, AVAILABLE, FAILED, ARCHIVED }

data class StoredObjectRef(
    val id: String,
    val organizationId: String,
    val ownerEntityType: String,
    val ownerEntityId: String,
    val kind: StoredObjectKind,
    val fileName: String,
    val mimeType: String,
    val objectKey: String?,
    val checksumSha256: String?,
    val sizeBytes: Long?,
    val version: Int = 1,
    val createdByUserId: String,
    val createdAtEpochMs: Long,
    val state: StoredObjectState = StoredObjectState.LOCAL,
)

data class MultipartUploadSession(
    val uploadId: String,
    val objectId: String,
    val chunkSizeBytes: Long,
    val uploadedParts: Set<Int> = emptySet(),
    val expiresAtEpochMs: Long,
)

data class StoredObjectAccessGrant(
    val objectId: String,
    val userId: String,
    val canRead: Boolean,
    val canReplace: Boolean,
    val canArchive: Boolean,
)
