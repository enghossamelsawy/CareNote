package com.hossam.carenote.feature.resident.note

import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.hossam.carenote.core.designsystem.theme.CareNoteTheme
import com.hossam.carenote.core.domain.vitals.VitalKind
import com.hossam.carenote.core.domain.vitals.VitalsAssessor
import com.hossam.carenote.core.model.SyncStatus
import com.hossam.carenote.core.model.Vitals
import com.hossam.carenote.feature.resident.NoteItem
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
class NoteDetailScreenScreenshotTest {
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
    fun `given an edited note, when it is shown in full, then it matches the screenshots`() {
        screenshots("note_detail_edited") {
            NoteDetailScreen(
                state = NoteDetailState(
                    isLoading = false,
                    note = note(
                        transcript = "Temperature is 37 and pulse is 75. Slept well, ate a full breakfast and " +
                            "walked to the garden with help. No pain reported.",
                        vitals = Vitals(temperatureCelsius = 37.0, pulse = 75),
                        status = SyncStatus.SYNCED,
                        editedAt = Instant.parse("2026-11-01T10:15:00Z"),
                    ),
                    earlier = listOf(
                        EarlierVersion(
                            id = UUID(0, 1),
                            writtenAt = Instant.parse("2026-11-01T05:29:00Z"),
                            transcript = "Temperature is 38.5 and pulse is 75.",
                            vitals = Vitals(temperatureCelsius = 38.5, pulse = 75),
                            changed = setOf(VitalKind.Temperature),
                        ),
                    ),
                ),
                onIntent = {},
            )
        }
    }

    @Test
    fun `given an edit whose earlier version was refused, when it is shown in full, then it matches the screenshots`() {
        screenshots("note_detail_earlier_failed") {
            NoteDetailScreen(
                state = NoteDetailState(
                    isLoading = false,
                    note = note(
                        transcript = "Pulse is 76. Slept well.",
                        vitals = Vitals(pulse = 76),
                        status = SyncStatus.PENDING,
                        editedAt = Instant.parse("2026-11-01T10:15:00Z"),
                    ).copy(earlierNotSynced = true),
                    earlier = listOf(
                        EarlierVersion(
                            id = UUID(0, 1),
                            writtenAt = Instant.parse("2026-11-01T05:29:00Z"),
                            transcript = "Pulse is 72. Slept well.",
                            status = SyncStatus.FAILED,
                            vitals = Vitals(pulse = 72),
                            changed = setOf(VitalKind.Pulse),
                        ),
                    ),
                ),
                onIntent = {},
            )
        }
    }

    @Test
    fun `given a refused note with a high value, when it is shown in full, then it matches the screenshots`() {
        screenshots("note_detail_failed") {
            NoteDetailScreen(
                state = NoteDetailState(
                    isLoading = false,
                    note = note(
                        transcript = "Blood pressure 160 over 95, pulse 72.",
                        vitals = Vitals(systolic = 160, diastolic = 95, pulse = 72),
                        status = SyncStatus.FAILED,
                    ),
                ),
                onIntent = {},
            )
        }
    }

    @Test
    fun `given an unknown note, when the screen shows, then not found matches the screenshots`() {
        screenshots("note_detail_not_found") {
            NoteDetailScreen(state = NoteDetailState(isLoading = false, notFound = true), onIntent = {})
        }
    }

    private fun note(transcript: String, vitals: Vitals, status: SyncStatus, editedAt: Instant? = null) = NoteItem(
        id = UUID(0, 2),
        createdAt = Instant.parse("2026-11-01T05:29:00Z"),
        transcript = transcript,
        vitals = vitals,
        assessment = VitalsAssessor().assess(vitals),
        status = status,
        editedAt = editedAt,
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
