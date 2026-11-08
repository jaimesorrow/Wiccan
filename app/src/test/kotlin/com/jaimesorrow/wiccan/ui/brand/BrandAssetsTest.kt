package com.jaimesorrow.wiccan.ui.brand

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.jaimesorrow.wiccan.ui.theme.WiccanPaletteColors
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks the shipped launcher layers and the prepared Google Play exports
 * against the brand guidelines and Android/Play specs. Files are produced by
 * brand/scripts/build_brand_assets.py from the approved artwork.
 */
class BrandAssetsTest {
    private val res = File("src/main/res").takeIf { it.isDirectory } ?: File("app/src/main/res")
    private val exports = File(res, "../../../../brand/exports").canonicalFile

    private val densities = mapOf("mdpi" to 1.0, "hdpi" to 1.5, "xhdpi" to 2.0, "xxhdpi" to 3.0, "xxxhdpi" to 4.0)

    private fun png(file: File): BufferedImage {
        assertTrue("Missing ${file.path}", file.isFile)
        return requireNotNull(ImageIO.read(file)) { "${file.path} is not a PNG" }
    }

    @Test
    fun `adaptive icons declare background, foreground and a monochrome themed layer`() {
        listOf("ic_launcher.xml", "ic_launcher_round.xml").forEach { name ->
            val xml = File(res, "mipmap-anydpi-v26/$name").readText()
            assertTrue(name, xml.contains("<background android:drawable=\"@color/ic_launcher_background\""))
            assertTrue(name, xml.contains("<foreground android:drawable=\"@mipmap/ic_launcher_foreground\""))
            assertTrue(name, xml.contains("<monochrome android:drawable=\"@mipmap/ic_launcher_monochrome\""))
        }
        assertTrue(File(res, "values/colors.xml").readText().contains("<color name=\"ic_launcher_background\">#0B132B</color>"))
    }

    @Test
    fun `foreground and monochrome layers are 108dp with the emblem inside the 66dp safe zone`() {
        densities.forEach { (density, scale) ->
            listOf("ic_launcher_foreground.png", "ic_launcher_monochrome.png").forEach { name ->
                val image = png(File(res, "mipmap-$density/$name"))
                val canvas = Math.round(108 * scale).toInt()
                assertEquals("$density/$name width", canvas, image.width)
                assertEquals("$density/$name height", canvas, image.height)
                assertTrue("$density/$name must be transparent outside the art", image.colorModel.hasAlpha())
                // Every visible pixel lies within the central 66dp circle: nothing is baked
                // into the area launchers mask away.
                val safeRadius = 33 * scale
                val c = (canvas - 1) / 2.0
                for (y in 0 until image.height) {
                    for (x in 0 until image.width) {
                        val alpha = image.getRGB(x, y) ushr 24
                        if (alpha > 8) {
                            assertTrue("$density/$name has art outside the safe zone at $x,$y", hypot(x - c, y - c) <= safeRadius)
                        }
                    }
                }
                assertEquals("$density/$name corners stay transparent (no baked mask)", 0, image.getRGB(0, 0) ushr 24)
            }
        }
    }

    @Test
    fun `legacy launcher icons exist for API 24 and 25 at 48dp`() {
        densities.forEach { (density, scale) ->
            listOf("ic_launcher.png", "ic_launcher_round.png").forEach { name ->
                val image = png(File(res, "mipmap-$density/$name"))
                assertEquals("$density/$name", Math.round(48 * scale).toInt(), image.width)
                assertEquals("$density/$name", image.width, image.height)
            }
        }
    }

    @Test
    fun `Google Play icon is a 512px 32-bit PNG under 1 MB`() {
        val file = File(exports, "google-play/wiccan_play_icon_512.png")
        val image = png(file)
        assertEquals(512, image.width)
        assertEquals(512, image.height)
        assertEquals("32-bit RGBA", 32, image.colorModel.pixelSize)
        assertTrue("Under 1 MB", file.length() < 1_048_576)
        // Permanent Midnight Blue identity, full-bleed (Play applies its own mask).
        assertEquals(0xFF0B132B.toInt(), image.getRGB(0, 0))
    }

    @Test
    fun `feature graphic is 1024x500 without transparency`() {
        val image = png(File(exports, "google-play/wiccan_feature_graphic_1024x500.png"))
        assertEquals(1024, image.width)
        assertEquals(500, image.height)
        assertTrue("Feature graphic must not use alpha", !image.colorModel.hasAlpha())
    }

    @Test
    fun `emblem colour meets graphical contrast on all six palettes`() {
        mapOf(
            "Near Black" to WiccanPaletteColors.NearBlack.Background,
            "Midnight Blue" to WiccanPaletteColors.MidnightBlue.Background,
            "Royal Plum" to WiccanPaletteColors.RoyalPlum.Background,
            "Deep Forest" to WiccanPaletteColors.DeepForest.Background,
            "Warm Earth" to WiccanPaletteColors.WarmEarth.Background,
        ).forEach { (name, background) ->
            val ratio = contrast(WiccanPaletteColors.Brand.EmblemGold, background)
            assertTrue("Gold emblem on $name is $ratio:1", ratio >= 4.5)
        }
        val ivory = contrast(WiccanPaletteColors.LightIvory.Primary, WiccanPaletteColors.LightIvory.Background)
        assertTrue("Dark-primary emblem on Light/Ivory is $ivory:1", ivory >= 4.5)
    }

    private fun contrast(a: Color, b: Color): Double {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }
}
