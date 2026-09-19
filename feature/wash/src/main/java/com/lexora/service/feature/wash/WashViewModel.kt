package com.lexora.service.feature.wash

import com.lexora.service.core.domain.WashOperations
import com.lexora.service.core.model.*
import com.lexora.service.core.presentation.LexoraViewModel

data class WashUiState(
    val loading: Boolean = true,
    val posts: List<WashPost> = emptyList(),
    val queue: List<WashQueueItem> = emptyList(),
    val techCards: List<WashTechCard> = emptyList(),
    val chemicalUsage: List<WashChemicalUsage> = emptyList(),
    val error: String? = null,
)

class WashViewModel(
    private val organizationId: String,
    private val wash: WashOperations,
) : LexoraViewModel<WashUiState>(WashUiState()) {
    init { reload() }
    fun reload() = launchSafely(::fail) { refresh() }
    fun addPost() = launchSafely(::fail) { wash.addPost(organizationId, null, "РџРѕСЃС‚ ${state.value.posts.size + 1}"); refresh() }
    fun togglePost(id: String) = launchSafely(::fail) { state.value.posts.firstOrNull { it.id == id }?.let { wash.togglePostStatus(it) }; refresh() }
    fun addQueueItem() = launchSafely(::fail) { wash.addQueueItem(organizationId); refresh() }
    fun advanceQueue(id: String) = launchSafely(::fail) { state.value.queue.firstOrNull { it.id == id }?.let { wash.advanceQueueItem(organizationId, it) }; refresh() }
    fun addTechCard() = launchSafely(::fail) { wash.addTechCard(organizationId); refresh() }
    fun addChemicalUsage() = launchSafely(::fail) { wash.addChemicalUsage(organizationId); refresh() }

    private suspend fun refresh() {
        updateState { it.copy(loading = true, error = null) }
        setState(WashUiState(false, wash.posts(organizationId), wash.queue(organizationId), wash.techCards(organizationId), wash.chemicalUsage(organizationId)))
    }
    private fun fail(error: Throwable) = updateState { it.copy(loading = false, error = error.message ?: "wash_failed") }
}

