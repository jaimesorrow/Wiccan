package com.wiccan.app.ui.theme

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders every [WiccanPalette] to a real PNG on the JVM - no emulator, no
 * device. This is the only way to actually look at the six palettes in a
 * headless environment: `@Preview` composables are compile-checked but never
 * rasterized by a build.
 *
 * Output: `app/build/outputs/roborazzi/wiccan-palette-<name>.png`.
 *
 * The test asserts each PNG exists and is not a single flat color. Judging
 * the colors themselves is a human (or vision-model) job - emitting the
 * files is the point.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// A plain Application is essential: Robolectric otherwise instantiates
// WiccanApplication, whose onCreate installs the App Check provider and dies
// with IllegalStateException at FirebaseApp.java:179 - no Firebase here.
@Config(
    sdk = [34],
    qualifiers = "w411dp-h891dp-xhdpi",
    application = android.app.Application::class,
)
class WiccanPaletteScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `every palette renders to a PNG`() {
        // setContent may only be called once per rule, so the palette is
        // swapped through state and re-captured instead.
        val current = mutableStateOf(WiccanPalette.entries.first())
        composeRule.setContent { PaletteSpecimen(current.value) }

        val written = mutableListOf<File>()
        for (palette in WiccanPalette.entries) {
            current.value = palette
            composeRule.waitForIdle()

            val path = "build/outputs/roborazzi/wiccan-palette-${palette.name.lowercase()}.png"
            composeRule.onRoot().captureRoboImage(filePath = path)
            written += File(path)
        }

        println("Palette screenshots:")
        for (file in written) {
            check(file.isFile && file.length() > 0) { "${file.name} was not written" }
            val image = BitmapFactory.decodeFile(file.path)
                ?: error("${file.name} is not decodable as a PNG")
            check(!image.isFlat()) { "${file.name} rendered as a single flat color" }
            println("  ${file.name} (${image.width}x${image.height}, ${file.length()} bytes)")
        }
        check(written.size == WiccanPalette.entries.size)
    }

    /**
     * One surface exercising the roles most likely to break contrast:
     * display and body type, a filled button, an outlined button, and a card
     * against the palette's own background.
     */
    @Composable
    private fun PaletteSpecimen(palette: WiccanPalette) {
        MaterialTheme(
            colorScheme = palette.toColorScheme(),
            typography = LocallyResolvableTypography,
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("WICCAN", style = MaterialTheme.typography.displayLarge)
                    Text(palette.displayName, style = MaterialTheme.typography.headlineMedium)
                    Text(palette.moodDescription, style = MaterialTheme.typography.bodyLarge)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Today's lesson", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Body copy on a card surface, to check that " +
                                    "onSurfaceVariant stays readable.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                        Text("Continue")
                    }
                    OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                        Text("Not now")
                    }
                }
            }
        }
    }
}

/**
 * [WiccanTypography]'s real type scale with the font families swapped for
 * system fallbacks.
 *
 * Cinzel and Manrope are *downloadable* Google Fonts served through Play
 * Services (see `Type.kt`). Robolectric has no font provider, so those
 * requests never resolve. Sizes, weights, line heights and letter spacing
 * are the real ones - only glyph shapes are substituted, so these PNGs are
 * accurate for color, contrast and layout, but not for typeface identity.
 */
private val LocallyResolvableTypography: Typography = WiccanTypography.let { t ->
    fun TextStyle.local() = copy(fontFamily = FontFamily.Default)
    Typography(
        displayLarge = t.displayLarge.local(),
        displayMedium = t.displayMedium.local(),
        displaySmall = t.displaySmall.local(),
        headlineLarge = t.headlineLarge.local(),
        headlineMedium = t.headlineMedium.local(),
        headlineSmall = t.headlineSmall.local(),
        titleLarge = t.titleLarge.local(),
        titleMedium = t.titleMedium.local(),
        titleSmall = t.titleSmall.local(),
        bodyLarge = t.bodyLarge.local(),
        bodyMedium = t.bodyMedium.local(),
        bodySmall = t.bodySmall.local(),
        labelLarge = t.labelLarge.local(),
        labelMedium = t.labelMedium.local(),
        labelSmall = t.labelSmall.local(),
    )
}

/**
 * True when every sampled pixel is identical - i.e. nothing really drew.
 *
 * Uses Android's BitmapFactory rather than javax.imageio: ImageIO is not on
 * the Android unit-test compile classpath, so importing it fails with
 * "Unresolved reference 'imageio'" even though the tests run on the JVM.
 */
private fun Bitmap.isFlat(): Boolean {
    val first = getPixel(0, 0)
    val stepX = (width / 16).coerceAtLeast(1)
    val stepY = (height / 16).coerceAtLeast(1)
    var y = 0
    while (y < height) {
        var x = 0
        while (x < width) {
            if (getPixel(x, y) != first) return false
            x += stepX
        }
        y += stepY
    }
    return true
}
