package com.hossam.carenote.feature.resident

import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.hossam.carenote.core.designsystem.theme.CareNoteTheme
import com.hossam.carenote.core.domain.vitals.VitalsAssessor
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

/**
 * The images are recorded on Windows and checked on CI's Linux, which smooths text a little differently.
 * Up to 1% changed pixels passes. A changed layout or colour is far above that, but a small text change can
 * stay below it, so the recorded images are still worth a look.
 */
private val screenshotOptions = RoborazziOptions(
    compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f),
)

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
                        note(id = 4, SyncStatus.FAILED, "Refused breakfast, drank tea.", Vitals()),
                        note(
                            id = 3,
                            SyncStatus.SYNCING,
                            "Blood pressure 160 over 95, pulse 72, temperature 38.4, SpO2 93",
                            Vitals(
                                systolic = 160,
                                diastolic = 95,
                                pulse = 72,
                                temperatureCelsius = 38.4,
                                oxygenSaturation = 93,
                            ),
                        ),
                        note(id = 2, SyncStatus.PENDING, "Pulse 72, temperature 36.8").copy(
                            editedAt = Instant.parse("2026-11-01T10:30:00Z"),
                            earlierNotSynced = true,
                        ),
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

    private fun note(
        id: Long,
        status: SyncStatus,
        transcript: String,
        vitals: Vitals = Vitals(pulse = 72, temperatureCelsius = 36.8),
    ) = NoteItem(
        id = UUID(0, id),
        createdAt = Instant.parse("2026-11-01T08:00:00Z").plusSeconds(id * 3_600),
        transcript = transcript,
        vitals = vitals,
        assessment = VitalsAssessor().assess(vitals),
        status = status,
    )

    @Test
    fun `given colleagues' notes haven't arrived, when a resident has none, then it matches the screenshots`() {
        screenshots("resident_notes_not_arrived") {
            ResidentScreen(
                state = ResidentState(
                    isLoading = false,
                    residentId = "1",
                    name = "Anna Becker",
                    room = "101",
                    colleagueNotesNotArrived = true,
                ),
                onIntent = {},
            )
        }
    }

    private fun screenshots(name: String, content: @Composable () -> Unit) {
        for (darkTheme in listOf(false, true)) {
            val theme = if (darkTheme) "dark" else "light"
            captureRoboImage("src/test/screenshots/${name}_$theme.png", roborazziOptions = screenshotOptions) {
                CareNoteTheme(darkTheme = darkTheme) { content() }
            }
        }
    }
}
