package com.hossam.carenote.core.common.logging

import android.util.Log
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import timber.log.Timber

class TimberLoggerTest {
    private val tree = RecordingTree()
    private val logger = TimberLogger()

    @Before
    fun setUp() {
        Timber.plant(tree)
    }

    @After
    fun tearDown() {
        Timber.uprootAll()
    }

    @Test
    fun `given a planted tree, when logging debug, then tag, level and message reach the tree`() {
        // Given
        val tag = "Sync"

        // When
        logger.d(tag, "Outbox has 2 entries")

        // Then
        val entry = tree.entries.single()
        assertEquals(Log.DEBUG, entry.priority)
        assertEquals(tag, entry.tag)
        assertEquals("Outbox has 2 entries", entry.message)
        assertNull(entry.throwable)
    }

    @Test
    fun `given a planted tree, when logging info, then the info level is used`() {
        // Given
        val tag = "Speech"

        // When
        logger.i(tag, "Using ML Kit engine")

        // Then
        assertEquals(Log.INFO, tree.entries.single().priority)
    }

    @Test
    fun `given a throwable, when logging a warning, then the throwable reaches the tree`() {
        // Given
        val error = IllegalStateException("timeout")

        // When
        logger.w("Sync", "Upload failed, will retry", error)

        // Then
        val entry = tree.entries.single()
        assertEquals(Log.WARN, entry.priority)
        assertSame(error, entry.throwable)
    }

    @Test
    fun `given a throwable, when logging an error, then the throwable reaches the tree`() {
        // Given
        val error = IllegalArgumentException("bad id")

        // When
        logger.e("Sync", "Giving up on note", error)

        // Then
        val entry = tree.entries.single()
        assertEquals(Log.ERROR, entry.priority)
        assertSame(error, entry.throwable)
    }

    private class RecordingTree : Timber.Tree() {
        data class Entry(val priority: Int, val tag: String?, val message: String, val throwable: Throwable?)

        val entries = mutableListOf<Entry>()

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            entries += Entry(priority, tag, message, t)
        }
    }
}
