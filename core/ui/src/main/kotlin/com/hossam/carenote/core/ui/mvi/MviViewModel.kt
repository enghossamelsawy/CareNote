package com.hossam.carenote.core.ui.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base for every screen's ViewModel: intents come in through [onIntent], the UI reads [state] and
 * reacts to [effects]. Only subclasses can change the state, so data flows one way.
 * Subclasses call use cases only, never repositories directly.
 */
abstract class MviViewModel<S : UiState, I : UiIntent, E : UiEffect>(initialState: S) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    // Buffered, so an effect sent while nobody collects (e.g. during rotation) waits instead of being dropped.
    private val _effects = Channel<E>(Channel.BUFFERED)

    /** One-time effects; each one is delivered to a single collector, once. */
    val effects: Flow<E> = _effects.receiveAsFlow()

    protected val currentState: S
        get() = _state.value

    abstract fun onIntent(intent: I)

    /** Atomic: concurrent updates are applied one after the other, none is lost. */
    protected fun setState(reduce: S.() -> S) {
        _state.update(reduce)
    }

    protected fun sendEffect(effect: E) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
