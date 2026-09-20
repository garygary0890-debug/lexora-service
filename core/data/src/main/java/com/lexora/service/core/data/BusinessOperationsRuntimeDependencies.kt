package com.lexora.service.core.data

/**
 * Runtime bridge used by feature modules while the project is being migrated to constructor DI.
 * The provider is installed once by the application container and resolves the active actor lazily,
 * so organization/user switching does not leave a stale audit actor in the UI.
 */
object BusinessOperationsRuntimeDependencies {
    @Volatile private var repository: BusinessOperationsRepository? = null
    @Volatile private var actorProvider: (() -> String)? = null

    fun install(repository: BusinessOperationsRepository, actorUserId: () -> String) {
        this.repository = repository
        this.actorProvider = actorUserId
    }

    fun repository(): BusinessOperationsRepository =
        requireNotNull(repository) { "BusinessOperationsRepository is not installed" }

    fun actorUserId(): String =
        requireNotNull(actorProvider) { "Business actor provider is not installed" }.invoke()
}
