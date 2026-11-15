package com.jaimesorrow.wiccan.ui.brand

import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.jaimesorrow.wiccan.ui.theme.WiccanPalette
import com.jaimesorrow.wiccan.ui.theme.toColorScheme
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the brand placements (full logo on identity surfaces, compact
 * emblem on content headers) in all six palettes to
 * `app/build/outputs/roborazzi/wiccan-brand-<palette>.png`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h640dp-xhdpi", application = android.app.Application::class)
class BrandPaletteScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `brand marks render in every palette`() {
        val current = mutableStateOf(WiccanPalette.entries.first())
        composeRule.setContent {
            MaterialTheme(colorScheme = current.value.toColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        WiccanFullLogo(width = 200.dp)
                        Text(current.value.displayName, color = MaterialTheme.colorScheme.onBackground)
                        WiccanEmblem(size = 56.dp, contentDescription = "emblem")
                    }
                }
            }
        }
        WiccanPalette.entries.forEach { palette ->
            current.value = palette
            composeRule.waitForIdle()
            composeRule.onNodeWithContentDescription("Wiccan").assertExists()
            val path = "build/outputs/roborazzi/wiccan-brand-${palette.name.lowercase()}.png"
            composeRule.onRoot().captureRoboImage(filePath = path)
            check(BitmapFactory.decodeFile(File(path).path) != null) { "$path not written" }
        }
    }
}
