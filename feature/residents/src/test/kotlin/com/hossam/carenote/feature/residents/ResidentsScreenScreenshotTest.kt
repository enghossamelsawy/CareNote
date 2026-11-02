package com.hossam.carenote.feature.residents

import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.hossam.carenote.core.designsystem.theme.CareNoteTheme
import com.hossam.carenote.core.domain.vitals.VitalLevel
import com.hossam.carenote.core.domain.vitals.VitalsAlert
import com.hossam.carenote.core.domain.vitals.VitalsAssessment
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
class ResidentsScreenScreenshotTest {
    private val originalTimeZone = TimeZone.getDefault()
    private val originalLocale = Locale.getDefault()

    private val residents = listOf(
        ResidentItem(id = "1", name = "Anna Becker", room = "101", pendingCount = 2),
        ResidentItem(id = "2", name = "Karl Fischer", room = "102", pendingCount = 0),
        ResidentItem(id = "3", name = "Greta Hoffmann", room = "103", pendingCount = 1),
    )

    // Alert times are shown in the phone's zone and format; fix both so the images match on every machine.
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
    fun `given residents, when the list is shown, then it matches the screenshots`() {
        screenshots("residents_list") {
            ResidentsScreen(state = ResidentsState(isLoading = false, residents = residents), onIntent = {})
        }
    }

    @Test
    fun `given residents whose vitals need attention, when the list is shown, then it matches the screenshots`() {
        val alerted = listOf(
            residents[0].copy(
                alert = VitalsAlert(
                    assessment = VitalsAssessment(temperature = VitalLevel.High, pulse = VitalLevel.High),
                    takenAt = Instant.parse("2026-11-01T05:29:00Z"),
                ),
            ),
            residents[1],
            residents[2].copy(
                alert = VitalsAlert(
                    assessment = VitalsAssessment(oxygenSaturation = VitalLevel.Low),
                    takenAt = Instant.parse("2026-10-31T21:05:00Z"),
                ),
            ),
        )
        screenshots("residents_attention") {
            ResidentsScreen(state = ResidentsState(isLoading = false, residents = alerted), onIntent = {})
        }
    }

    @Test
    fun `given the device is offline, when the list is shown, then the banner matches the screenshots`() {
        screenshots("residents_offline") {
            ResidentsScreen(
                state = ResidentsState(isLoading = false, residents = residents, isOffline = true),
                onIntent = {},
            )
        }
    }

    @Test
    fun `given no residents, when the list is shown, then the empty state matches the screenshots`() {
        screenshots("residents_empty") {
            ResidentsScreen(state = ResidentsState(isLoading = false), onIntent = {})
        }
    }

    @Test
    fun `given the first download is running, when the list is empty, then it matches the screenshots`() {
        screenshots("residents_downloading") {
            ResidentsScreen(state = ResidentsState(isLoading = false, isRefreshing = true), onIntent = {})
        }
    }

    @Test
    fun `given the server is unreachable, when the list is empty, then it matches the screenshots`() {
        screenshots("residents_unreachable") {
            ResidentsScreen(
                state = ResidentsState(isLoading = false, refreshProblem = RefreshProblem.Unavailable),
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
