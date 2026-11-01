package com.hossam.carenote.feature.residents

import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.hossam.carenote.core.designsystem.theme.CareNoteTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Record with `recordRoborazziDebug`, check with `verifyRoborazziDebug`; images live in src/test/screenshots. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class ResidentsScreenScreenshotTest {
    private val residents = listOf(
        ResidentItem(id = "1", name = "Anna Becker", room = "101", pendingCount = 2),
        ResidentItem(id = "2", name = "Karl Fischer", room = "102", pendingCount = 0),
        ResidentItem(id = "3", name = "Greta Hoffmann", room = "103", pendingCount = 1),
    )

    @Test
    fun `given residents, when the list is shown, then it matches the screenshots`() {
        screenshots("residents_list") {
            ResidentsScreen(state = ResidentsState(isLoading = false, residents = residents), onIntent = {})
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

    private fun screenshots(name: String, content: @Composable () -> Unit) {
        for (darkTheme in listOf(false, true)) {
            val theme = if (darkTheme) "dark" else "light"
            captureRoboImage("src/test/screenshots/${name}_$theme.png") {
                CareNoteTheme(darkTheme = darkTheme) { content() }
            }
        }
    }
}
