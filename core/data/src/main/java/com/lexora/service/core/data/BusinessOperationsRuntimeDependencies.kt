package com.lexora.service.core.data

/**
 * Runtime bridge used by feature modules while the project is being migrated to constructor DI.
 * The application container installs the repository once; the workspace updates the active actor
 * whenever the authenticated/scoped user changes so audit events never use a stale identity.
 */
object BusinessOperationsRuntimeDependencies {
    @Volatile private var repository: BusinessOperationsRepository? = null
    @Volatile private var activeActorUserId: String? = null

    fun install(repository: BusinessOperationsRepository) {
        this.repository = repository
    }

    fun setActorUserId(userId: String) {
        require(userId.isNotBlank()) { "Business actor user id is blank" }
        activeActorUserId = userId
    }

    fun repository(): BusinessOperationsRepository =
        requireNotNull(repository) { "BusinessOperationsRepository is not installed" }

    fun actorUserId(): String =
        requireNotNull(activeActorUserId) { "Business actor is not initialized by workspace" }
}
