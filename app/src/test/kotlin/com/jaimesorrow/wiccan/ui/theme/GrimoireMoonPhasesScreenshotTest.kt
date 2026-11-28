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
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.takahirom.roborazzi.captureRoboImage
import com.jaimesorrow.wiccan.ui.theme.WiccanPaletteColors.Grimoire
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private val PHASE_LABELS = listOf(
    "0 New",
    "1 Wax cres",
    "2 1st Q",
    "3 Wax gib",
    "4 Full",
    "5 Wan gib",
    "6 Last Q",
    "7 Wan cres",
)

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

/** Labeled before/after sheet at 96dp — writes only under build/outputs/roborazzi. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w820dp-h520dp-xhdpi", application = android.app.Application::class)
class GrimoireMoonPhasesLabeledSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `capture labeled legacy and fixed rows`() {
        composeRule.setContent {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Grimoire.Background)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Before (legacy offset circles)", color = Grimoire.Gold, fontSize = 14.sp)
                moonPhaseLabeledRow(useLegacy = true)
                Text("After (terminator)", color = Grimoire.Gold, fontSize = 14.sp)
                moonPhaseLabeledRow(useLegacy = false)
            }
        }
        composeRule.waitForIdle()
        val path = "build/outputs/roborazzi/wiccan-moon-phases-labeled-sheet.png"
        composeRule.onRoot().captureRoboImage(filePath = path)
        check(BitmapFactory.decodeFile(File(path).path) != null) { "$path not written" }
    }
}

@androidx.compose.runtime.Composable
private fun moonPhaseLabeledRow(useLegacy: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        repeat(8) { index ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(PHASE_LABELS[index], color = Grimoire.Muted, fontSize = 10.sp)
                if (useLegacy) {
                    LegacyMoonPhaseGlyph(index, Modifier.size(96.dp))
                } else {
                    GrimoireMoonPhaseGlyph(index, Modifier.size(96.dp))
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun LegacyMoonPhaseGlyph(index: Int, modifier: Modifier) {
    val lit = Grimoire.Gold
    val shadow = Grimoire.Background
    androidx.compose.foundation.Canvas(modifier) {
        val radius = size.minDimension * 0.46f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawLegacyMoonPhase(index, center, radius, lit, shadow)
    }
}

private fun DrawScope.drawLegacyMoonPhase(index: Int, center: Offset, radius: Float, lit: Color, shadow: Color) {
    drawCircle(shadow, radius, center)
    drawCircle(lit.copy(alpha = 0.85f), radius, center, style = Stroke(width = 1.dp.toPx()))
    if (index == 0) return
    if (index == 4) {
        drawCircle(lit, radius * 0.92f, center)
        return
    }
    val shift = when (index) {
        1 -> -radius * 1.35f
        2 -> -radius
        3 -> -radius * 0.42f
        5 -> radius * 0.42f
        6 -> radius
        else -> radius * 1.35f
    }
    clipPath(Path().apply { addOval(Rect(center, radius)) }) {
        drawCircle(lit, radius, center)
        drawCircle(shadow, radius, Offset(center.x + shift, center.y))
    }
    drawCircle(lit.copy(alpha = 0.9f), radius, center, style = Stroke(width = 1.dp.toPx()))
}
