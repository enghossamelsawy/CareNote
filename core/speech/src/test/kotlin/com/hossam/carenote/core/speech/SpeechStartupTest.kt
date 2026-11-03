package com.hossam.carenote.core.speech

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechStartupTest {
    private val speechModel = FakeSpeechModel()
    private val unmeteredNetwork = FakeUnmeteredNetwork()

    @Test
    fun `given a downloadable model on mobile data, when the app starts, then it waits instead of downloading`() =
        runTest {
            // Given
            speechModel.downloadable = true

            // When
            start()

            // Then
            assertEquals(1, unmeteredNetwork.waitCount)
            assertEquals(0, speechModel.downloadCount)
        }

    @Test
    fun `given the app is waiting for Wi-Fi, when the phone joins one, then the model downloads`() = runTest {
        // Given
        speechModel.downloadable = true
        start()

        // When
        unmeteredNetwork.join()

        // Then
        assertEquals(1, speechModel.downloadCount)
    }

    @Test
    fun `given a model that can't be downloaded, when the app starts, then it neither waits nor downloads`() = runTest {
        // Given
        speechModel.downloadable = false

        // When
        start()

        // Then
        assertEquals(0, unmeteredNetwork.waitCount)
        assertEquals(0, speechModel.downloadCount)
    }

    /** Runs in [TestScope.backgroundScope], like the app scope; unconfined, so each step happens at once. */
    private fun TestScope.start() {
        val appScope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler))
        SpeechStartup(speechModel, unmeteredNetwork, appScope).start()
    }
}

private class FakeSpeechModel : SpeechModel {
    var downloadable = false
    var downloadCount = 0
        private set

    override suspend fun canDownloadModel(): Boolean = downloadable

    override suspend fun downloadModel() {
        downloadCount++
    }
}

/** On mobile data until [join] is called. */
private class FakeUnmeteredNetwork : UnmeteredNetwork {
    private val joined = CompletableDeferred<Unit>()
    var waitCount = 0
        private set

    fun join() {
        joined.complete(Unit)
    }

    override suspend fun await() {
        waitCount++
        joined.await()
    }
}
