package com.lexora.service.core.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class LexoraViewModel<S : Any>(initialState: S) : ViewModel() {
    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<S> = mutableState.asStateFlow()
    protected val currentState: S get() = mutableState.value

    protected fun updateState(transform: (S) -> S) = mutableState.update(transform)
    protected fun setState(value: S) { mutableState.value = value }

    protected fun launchSafely(onError: (Throwable) -> Unit = {}, block: suspend () -> Unit) {
        val handler = CoroutineExceptionHandler { _, error -> onError(error) }
        viewModelScope.launch(handler) { block() }
    }
}

class LexoraViewModelFactory<VM : ViewModel>(private val create: () -> VM) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
}
