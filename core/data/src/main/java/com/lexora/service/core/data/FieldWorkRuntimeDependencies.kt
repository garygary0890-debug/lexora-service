package com.lexora.service.core.data

/**
 * Lightweight bridge for the legacy app navigation factory. The application container
 * installs these process-wide singletons before FieldWorkViewModel is created.
 */
object FieldWorkRuntimeDependencies {
    @Volatile private var execution: VisitExecutionRepository? = null
    @Volatile private var inventory: InventoryRepository? = null

    fun install(executionRepository: VisitExecutionRepository, inventoryRepository: InventoryRepository) {
        execution = executionRepository
        inventory = inventoryRepository
    }

    fun execution(): VisitExecutionRepository = requireNotNull(execution) { "FieldWork execution repository is not installed" }
    fun inventory(): InventoryRepository = requireNotNull(inventory) { "Inventory repository is not installed" }
}
