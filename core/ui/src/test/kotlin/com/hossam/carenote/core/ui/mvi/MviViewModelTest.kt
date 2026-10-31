package com.hossam.carenote.core.ui.mvi

import app.cash.turbine.test
import com.hossam.carenote.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

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
}

private data class CounterState(val count: Int) : UiState

private sealed interface CounterIntent : UiIntent {
    data object Increment : CounterIntent

    data object ShowCount : CounterIntent
}

private sealed interface CounterEffect : UiEffect {
    data class Message(val text: String) : CounterEffect
}

/** The smallest possible screen: just enough to exercise the base class. */
private class CounterViewModel : MviViewModel<CounterState, CounterIntent, CounterEffect>(CounterState(count = 0)) {
    override fun onIntent(intent: CounterIntent) {
        when (intent) {
            CounterIntent.Increment -> setState { copy(count = count + 1) }
            CounterIntent.ShowCount -> sendEffect(CounterEffect.Message("Count is ${currentState.count}"))
        }
    }
}
