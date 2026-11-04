package com.hossam.carenote.feature.record

import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.hossam.carenote.core.designsystem.theme.CareNoteTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

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
class RecordScreenScreenshotTest {
    private val review = RecordState(
        residentName = "Anna Becker",
        phase = RecordPhase.Done,
        transcript = "Blood pressure 120 over 80, pulse 72, temperature 36.8. Slept well.",
        permission = MicPermission.Granted,
        vitals = VitalsForm(
            values = mapOf(
                VitalField.Systolic to "120",
                VitalField.Diastolic to "80",
                VitalField.Pulse to "72",
                VitalField.Temperature to "36.8",
            ),
        ),
    )

    @Test
    fun `given nothing recorded yet, when the screen opens, then it matches the screenshots`() {
        screenshots("record_idle") {
            RecordScreen(state = RecordState(residentName = "Anna Becker"), onIntent = {})
        }
    }

    @Test
    fun `given recording, when words are heard, then the screen matches the screenshots`() {
        screenshots("record_recording") {
            RecordScreen(
                state = RecordState(
                    residentName = "Anna Becker",
                    phase = RecordPhase.Recording,
                    transcript = "Blood pressure 120 over 80, pulse",
                    permission = MicPermission.Granted,
                ),
                onIntent = {},
            )
        }
    }

    @Test
    fun `given a finished note, when reviewing, then the form matches the screenshots`() {
        screenshots("record_review") {
            RecordScreen(state = review, onIntent = {})
        }
    }

    @Test
    fun `given an implausible pulse, when reviewing, then the field error matches the screenshots`() {
        screenshots("record_review_error") {
            RecordScreen(
                state = review.copy(
                    vitals = review.vitals.copy(
                        values = review.vitals.values + (VitalField.Pulse to "720"),
                        errors = mapOf(VitalField.Pulse to FieldError.OutOfRange(min = "20", max = "250")),
                    ),
                ),
                onIntent = {},
            )
        }
    }

    @Test
    fun `given the microphone was refused, when the screen shows, then the explanation matches the screenshots`() {
        screenshots("record_permission_denied") {
            RecordScreen(
                state = RecordState(residentName = "Anna Becker", permission = MicPermission.Denied),
                onIntent = {},
            )
        }
    }

    @Test
    fun `given the note to edit couldn't be read, when the screen shows, then the message matches the screenshots`() {
        screenshots("record_note_unreadable") {
            RecordScreen(
                state = RecordState(residentName = "Anna Becker", cannotEdit = CannotEdit.Unreadable),
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
