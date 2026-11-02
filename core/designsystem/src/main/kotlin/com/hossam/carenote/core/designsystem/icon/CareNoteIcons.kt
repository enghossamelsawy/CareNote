package com.hossam.carenote.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Every icon the app uses, drawn as small vectors so we don't pull in the large Material icons library.
 * Features use these like any other design token; the colour comes from the Icon's tint, not from here.
 */
object CareNoteIcons {
    /** Mirrored in right-to-left layouts. */
    val ArrowBack: ImageVector by lazy {
        ImageVector.Builder(
            name = "ArrowBack",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
            autoMirror = true,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20f, 11f)
                horizontalLineTo(7.83f)
                lineToRelative(5.59f, -5.59f)
                lineTo(12f, 4f)
                lineToRelative(-8f, 8f)
                lineToRelative(8f, 8f)
                lineToRelative(1.41f, -1.41f)
                lineTo(7.83f, 13f)
                horizontalLineTo(20f)
                close()
            }
        }.build()
    }

    /** A value above its usual range. */
    val ArrowUp: ImageVector by lazy {
        icon(name = "ArrowUp", pathData = "M4,12l1.41,1.41L11,7.83V20h2V7.83l5.58,5.59L20,12l-8,-8 -8,8z")
    }

    /** A value below its usual range. */
    val ArrowDown: ImageVector by lazy {
        icon(name = "ArrowDown", pathData = "M20,12l-1.41,-1.41L13,16.17V4h-2v12.17l-5.58,-5.59L4,12l8,8 8,-8z")
    }

    /** Done or fine, such as a note that reached the server. */
    val Check: ImageVector by lazy {
        icon(name = "Check", pathData = "M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z")
    }

    /** A pencil; used to edit a note. */
    val Edit: ImageVector by lazy {
        icon(
            name = "Edit",
            pathData = "M3,17.25V21h3.75L17.81,9.94l-3.75,-3.75L3,17.25z" +
                "M20.71,7.04c0.39,-0.39 0.39,-1.02 0,-1.41l-2.34,-2.34c-0.39,-0.39 -1.02,-0.39 -1.41,0" +
                "l-1.83,1.83 3.75,3.75 1.83,-1.83z",
        )
    }

    /** People; used when there are no residents yet. */
    val Group: ImageVector by lazy {
        icon(
            name = "Group",
            pathData = "M16,11c1.66,0 2.99,-1.34 2.99,-3S17.66,5 16,5c-1.66,0 -3,1.34 -3,3s1.34,3 3,3z" +
                "M8,11c1.66,0 2.99,-1.34 2.99,-3S9.66,5 8,5C6.34,5 5,6.34 5,8s1.34,3 3,3z" +
                "M8,13c-2.33,0 -7,1.17 -7,3.5V19h14v-2.5c0,-2.33 -4.67,-3.5 -7,-3.5z" +
                "M16,13c-0.29,0 -0.62,0.02 -0.97,0.05 1.16,0.84 1.97,1.97 1.97,3.45V19h6v-2.5" +
                "c0,-2.33 -4.67,-3.5 -7,-3.5z",
        )
    }

    /** A page of notes; used when a resident has none yet. */
    val Notes: ImageVector by lazy {
        icon(
            name = "Notes",
            pathData = "M14,2H6c-1.1,0 -1.99,0.9 -1.99,2L4,20c0,1.1 0.89,2 1.99,2H18c1.1,0 2,-0.9 2,-2V8l-6,-6z" +
                "M16,18H8v-2h8v2zM16,14H8v-2h8v2zM13,9V3.5L18.5,9H13z",
        )
    }

    /** A person crossed out; used when a resident no longer exists. */
    val PersonOff: ImageVector by lazy {
        icon(
            name = "PersonOff",
            pathData = "M8.65,5.82C9.36,4.72 10.6,4 12,4c2.21,0 4,1.79 4,4c0,1.4 -0.72,2.64 -1.82,3.35L8.65,5.82z" +
                "M20,17.17c-0.02,-1.1 -0.63,-2.11 -1.61,-2.62c-0.54,-0.28 -1.13,-0.54 -1.77,-0.76L20,17.17z" +
                "M21.19,21.19L2.81,2.81L1.39,4.22l8.89,8.89c-1.81,0.23 -3.39,0.79 -4.67,1.45" +
                "C4.61,15.07 4,16.1 4,17.22V20h13.17l2.61,2.61L21.19,21.19z",
        )
    }

    val Mic: ImageVector by lazy {
        icon(
            name = "Mic",
            pathData = "M12,14c1.66,0 3,-1.34 3,-3V5c0,-1.66 -1.34,-3 -3,-3S9,3.34 9,5v6c0,1.66 1.34,3 3,3z" +
                "M17.3,11c0,3 -2.54,5.1 -5.3,5.1S6.7,14 6.7,11H5c0,3.41 2.72,6.23 6,6.72V21h2v-3.28" +
                "c3.28,-0.48 6,-3.3 6,-6.72h-1.7z",
        )
    }

    /** A clock; used when a resident has had no note for a while. */
    val Schedule: ImageVector by lazy {
        icon(
            name = "Schedule",
            pathData = "M11.99,2C6.47,2 2,6.48 2,12s4.47,10 9.99,10C17.52,22 22,17.52 22,12S17.52,2 11.99,2z" +
                "M12,20c-4.42,0 -8,-3.58 -8,-8s3.58,-8 8,-8 8,3.58 8,8 -3.58,8 -8,8z" +
                "M12.5,7H11v6l5.25,3.15 0.75,-1.23 -4.5,-2.67z",
        )
    }

    val Stop: ImageVector by lazy { icon(name = "Stop", pathData = "M6,6h12v12H6z") }

    /** Settings sliders; used for the debug menu. */
    val Tune: ImageVector by lazy {
        icon(
            name = "Tune",
            pathData = "M3,17v2h6v-2H3zM3,5v2h10V5H3zM13,21v-2h8v-2h-8v-2h-2v6h2zM7,9v2H3v2h4v2h2V9H7z" +
                "M21,13v-2H11v2h10zM15,9h2V7h4V5h-4V3h-2v6z",
        )
    }

    /** A 24dp icon from SVG path data, as found in Material's icon files. */
    private fun icon(name: String, pathData: String): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black)).build()
}
