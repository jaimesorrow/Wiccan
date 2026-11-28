package com.jaimesorrow.wiccan.ui.theme

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.jaimesorrow.wiccan.ui.theme.WiccanPaletteColors.Grimoire
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** All eight moon row glyphs at 28dp height on the Midnight Grimoire ground. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h320dp-xhdpi", application = android.app.Application::class)
class GrimoireMoonPhasesScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `capture all eight moon phases`() {
        composeRule.setContent {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Grimoire.Background)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
            ) {
                repeat(8) { index ->
                    GrimoireMoonPhases(active = index, activePhaseName = null)
                }
            }
        }
        composeRule.waitForIdle()
        val path = "build/outputs/roborazzi/wiccan-moon-phases-all.png"
        composeRule.onRoot().captureRoboImage(filePath = path)
        check(BitmapFactory.decodeFile(File(path).path) != null) { "$path not written" }
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w820dp-h140dp-xhdpi", application = android.app.Application::class)
class GrimoireMoonPhasesLargeScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `capture large glyphs for visual review`() {
        composeRule.setContent {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Grimoire.Background)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                repeat(8) { index ->
                    GrimoireMoonPhaseGlyph(index, Modifier.size(96.dp))
                }
            }
        }
        composeRule.waitForIdle()
        val path = "build/outputs/roborazzi/wiccan-moon-phases-large.png"
        composeRule.onRoot().captureRoboImage(filePath = path)
        check(BitmapFactory.decodeFile(File(path).path) != null) { "$path not written" }
    }
}
