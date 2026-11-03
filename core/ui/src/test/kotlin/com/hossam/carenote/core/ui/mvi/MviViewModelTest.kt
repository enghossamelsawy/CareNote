package com.hossam.carenote.core.ui.mvi

import app.cash.turbine.test
import com.hossam.carenote.core.testing.MainDispatcherRule
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class MviViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `given a new view model, when nothing happens, then the initial state is emitted`() = runTest {
        // Given
        val viewModel = CounterViewModel()

        // When
        val state = viewModel.state.value

        // Then
        assertEquals(CounterState(count = 0), state)
    }

    @Test
    fun `given an intent, when handled, then the state is updated`() = runTest {
        // Given
        val viewModel = CounterViewModel()

        // When
        viewModel.onIntent(CounterIntent.Increment)
        viewModel.onIntent(CounterIntent.Increment)

        // Then
        assertEquals(CounterState(count = 2), viewModel.state.value)
    }

    @Test
    fun `given an effect sent before anyone collects, when collecting, then it is still delivered`() = runTest {
        // Given
        val viewModel = CounterViewModel()
        viewModel.onIntent(CounterIntent.ShowCount)

        // When
        viewModel.effects.test {
            // Then
            assertEquals(CounterEffect.Message("Count is 0"), awaitItem())
        }
    }

    @Test
    fun `given an effect, when collected twice, then it is delivered only once`() = runTest {
        // Given
        val viewModel = CounterViewModel()
        viewModel.onIntent(CounterIntent.ShowCount)
        viewModel.effects.test { awaitItem() }

        // When
        viewModel.effects.test {
            // Then
            expectNoEvents()
        }
    }

    @Test
    fun `given a flow collected while shown, when the screen collects the state, then the state follows it`() =
        runTest {
            // Given
            val source = MutableStateFlow(1)
            val viewModel = CounterViewModel(source)
            show(viewModel)

            // When
            source.value = 2

            // Then
            assertEquals(CounterState(count = 2), viewModel.state.value)
        }

    @Test
    fun `given a flow collected while shown, when nothing collects the state, then the flow isn't collected`() =
        runTest {
            // Given
            val source = MutableStateFlow(1)

            // When
            val viewModel = CounterViewModel(source)

            // Then
            assertEquals(0, source.subscriptionCount.value)
            assertEquals(CounterState(count = 0), viewModel.state.value)
        }

    @Test
    fun `given a shown screen, when it stops collecting for longer than the timeout, then the flow stops`() = runTest {
        // Given
        val source = MutableStateFlow(1)
        val viewModel = CounterViewModel(source)
        val screen = show(viewModel)

        // When
        screen.cancel()
        advanceTimeBy(MviViewModel.STOP_TIMEOUT + 1.seconds)

        // Then
        assertEquals(0, source.subscriptionCount.value)
    }

    @Test
    fun `given a shown screen, when it comes back within the timeout, then the flow keeps going`() = runTest {
        // Given
        val source = MutableStateFlow(1)
        val viewModel = CounterViewModel(source)
        val screen = show(viewModel)

        // When
        screen.cancel()
        advanceTimeBy(MviViewModel.STOP_TIMEOUT - 1.seconds)
        show(viewModel)
        advanceTimeBy(MviViewModel.STOP_TIMEOUT)

        // Then
        assertEquals(1, source.subscriptionCount.value)
    }

    @Test
    fun `given a stopped flow, when the screen collects again, then the state catches up`() = runTest {
        // Given
        val source = MutableStateFlow(1)
        val viewModel = CounterViewModel(source)
        show(viewModel).cancel()
        advanceTimeBy(MviViewModel.STOP_TIMEOUT + 1.seconds)
        source.value = 2

        // When
        show(viewModel)

        // Then
        assertEquals(CounterState(count = 2), viewModel.state.value)
    }

    /** Collects the state like a visible screen would, until the returned job is cancelled. */
    private fun TestScope.show(viewModel: CounterViewModel): Job =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
}

private data class CounterState(val count: Int) : UiState

private sealed interface CounterIntent : UiIntent {
    data object Increment : CounterIntent

    data object ShowCount : CounterIntent
}

private sealed interface CounterEffect : UiEffect {
    data class Message(val text: String) : CounterEffect
}

/** The smallest possible screen: just enough to exercise the base class. Its count follows [source] while shown. */
private class CounterViewModel(source: Flow<Int> = emptyFlow()) :
    MviViewModel<CounterState, CounterIntent, CounterEffect>(CounterState(count = 0)) {
    init {
        source.collectWhileShown { count -> setState { copy(count = count) } }
    }

    override fun onIntent(intent: CounterIntent) {
        when (intent) {
            CounterIntent.Increment -> setState { copy(count = count + 1) }
            CounterIntent.ShowCount -> sendEffect(CounterEffect.Message("Count is ${currentState.count}"))
        }
    }
}
