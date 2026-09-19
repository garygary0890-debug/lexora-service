package com.lexora.service.feature.search

import com.lexora.service.core.domain.GlobalSearchUseCase
import com.lexora.service.core.model.GlobalSearchEntityType
import com.lexora.service.core.model.GlobalSearchResult
import com.lexora.service.core.presentation.LexoraViewModel

data class GlobalSearchUiState(
    val query: String = "",
    val loading: Boolean = false,
    val selectedTypes: Set<GlobalSearchEntityType> = emptySet(),
    val results: List<GlobalSearchResult> = emptyList(),
    val error: String? = null,
)

class GlobalSearchViewModel(
    private val organizationId: String,
    private val search: GlobalSearchUseCase,
) : LexoraViewModel<GlobalSearchUiState>(GlobalSearchUiState()) {
    fun setQuery(value: String) = updateState { it.copy(query = value) }

    fun toggleType(type: GlobalSearchEntityType) = updateState { current ->
        val next = current.selectedTypes.toMutableSet().apply { if (!add(type)) remove(type) }
        current.copy(selectedTypes = next)
    }

    fun search() = launchSafely(onError = { error ->
        updateState { it.copy(loading = false, error = error.message ?: "search_failed") }
    }) {
        val current = state.value
        updateState { it.copy(loading = true, error = null) }
        val all = search(organizationId, current.query)
        val filtered = if (current.selectedTypes.isEmpty()) all else all.filter { it.entityType in current.selectedTypes }
        updateState { it.copy(loading = false, results = filtered) }
    }
}

