package com.hossam.carenote.feature.resident

import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.hossam.carenote.core.designsystem.theme.CareNoteTheme
import com.hossam.carenote.core.model.SyncStatus
import com.hossam.carenote.core.model.Vitals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/** Record with `recordRoborazziDebug`, check with `verifyRoborazziDebug`; images live in src/test/screenshots. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class ResidentScreenScreenshotTest {
    private val originalTimeZone = TimeZone.getDefault()
    private val originalLocale = Locale.getDefault()

    // Note times are shown in the phone's zone and format; fix both so the images match on every machine.
    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `given notes in every sync status, when the resident is shown, then it matches the screenshots`() {
        screenshots("resident_notes") {
            ResidentScreen(
                state = ResidentState(
                    isLoading = false,
                    residentId = "1",
                    name = "Anna Becker",
                    room = "101",
                    notes = listOf(
                        note(id = 4, SyncStatus.FAILED, "Refused breakfast, drank tea."),
                        note(id = 3, SyncStatus.SYNCING, "Blood pressure 120 over 80"),
                        note(id = 2, SyncStatus.PENDING, "Pulse 72, temperature 36.8"),
                        note(id = 1, SyncStatus.SYNCED, "Slept well."),
                    ),
                ),
                onIntent = {},
            )
        }
    }

    @Test
    fun `given no notes, when the resident is shown, then the empty state matches the screenshots`() {
        screenshots("resident_no_notes") {
            ResidentScreen(
                state = ResidentState(isLoading = false, residentId = "1", name = "Anna Becker", room = "101"),
                onIntent = {},
            )
        }
    }

    private fun note(id: Long, status: SyncStatus, transcript: String) = NoteItem(
        id = UUID(0, id),
        createdAt = Instant.parse("2026-11-01T08:00:00Z").plusSeconds(id * 3_600),
        transcript = transcript,
        vitals = Vitals(pulse = 72, temperatureCelsius = 36.8),
        status = status,
    )

    private fun screenshots(name: String, content: @Composable () -> Unit) {
        for (darkTheme in listOf(false, true)) {
            val theme = if (darkTheme) "dark" else "light"
            captureRoboImage("src/test/screenshots/${name}_$theme.png") {
                CareNoteTheme(darkTheme = darkTheme) { content() }
            }
        }
    }
}
