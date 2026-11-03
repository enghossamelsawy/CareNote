package com.hossam.carenote.core.ui.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

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

    /**
     * Collects this flow only while the screen collects [state], which `collectAsStateWithLifecycle` stops in
     * the background. `launchIn(viewModelScope)` would keep going for as long as the ViewModel lives, which for
     * the first screen is as long as the app runs. Stopping waits [STOP_TIMEOUT], so a rotation doesn't restart
     * the flow; starting again collects it from the start, so the state catches up with what changed meanwhile.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    protected fun <T> Flow<T>.collectWhileShown(action: suspend (T) -> Unit): Job {
        val source = this
        return viewModelScope.launch {
            _state.subscriptionCount
                .transformLatest { count ->
                    if (count == 0) delay(STOP_TIMEOUT)
                    emit(count > 0)
                }
                .distinctUntilChanged()
                .collectLatest { shown -> if (shown) source.collect(action) }
        }
    }

    companion object {
        /** How long a flow collected with [collectWhileShown] keeps going after the screen stops collecting. */
        val STOP_TIMEOUT = 5.seconds
    }
}
