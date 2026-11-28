package com.jaimesorrow.wiccan.ui.theme

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
@Config(sdk = [34], qualifiers = "w360dp-h120dp-xhdpi", application = android.app.Application::class)
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

    @Test
    fun `capture moon phase before and after comparison`() {
        composeRule.setContent {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Grimoire.Background)
                    .padding(8.dp),
            ) {
                Row {
                    Column(Modifier.weight(1f)) {
                        repeat(8) { index ->
                            LegacyMoonPhaseRow(active = index)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        repeat(8) { index ->
                            GrimoireMoonPhases(active = index, activePhaseName = null)
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
        val path = "build/outputs/roborazzi/wiccan-moon-phases-before-after.png"
        composeRule.onRoot().captureRoboImage(filePath = path)
        val artifact = File("/opt/cursor/artifacts/moon-before-after.png")
        artifact.parentFile.mkdirs()
        File(path).copyTo(artifact, overwrite = true)
        check(artifact.exists()) { "artifact not written" }
    }
}

/** Pre-fix offset-circle drawing kept for regression comparison only. */
@Composable
private fun LegacyMoonPhaseRow(active: Int) {
    val lit = Grimoire.Gold
    val shadow = Grimoire.Background
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(28.dp),
    ) {
        val count = 8
        val base = size.height * 0.30f
        for (index in 0 until count) {
            val center = Offset(size.width * (index + 0.5f) / count, size.height / 2f)
            val radius = if (index == active) base * 1.18f else base
            drawLegacyMoonPhase(index, center, radius, lit, shadow)
        }
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
