package com.jaimesorrow.wiccan.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class WiccanColorSchemesTest {

    @Test
    fun `every palette builds a color scheme whose isLight flag matches the palette`() {
        WiccanPalette.entries.forEach { palette ->
            val luminanceProxy = palette.toColorScheme().background
            if (palette.isLight) {
                assert(luminanceProxy.red > 0.8f && luminanceProxy.green > 0.8f && luminanceProxy.blue > 0.8f) {
                    "$palette is marked light but its background isn't bright: $luminanceProxy"
                }
            } else {
                assert(luminanceProxy.red < 0.3f && luminanceProxy.green < 0.3f && luminanceProxy.blue < 0.3f) {
                    "$palette is marked dark but its background isn't dark: $luminanceProxy"
                }
            }
        }
    }

    @Test
    fun `every palette has a distinct background from every other palette`() {
        val backgrounds = WiccanPalette.entries.map { it.toColorScheme().background }
        assertEquals(WiccanPalette.entries.size, backgrounds.distinct().size)
    }

    @Test
    fun `no palette reuses Material's default jarring red for error`() {
        val materialRed = androidx.compose.ui.graphics.Color(0xFFB3261E)
        WiccanPalette.entries.forEach { palette ->
            assertNotEquals(materialRed, palette.toColorScheme().error)
        }
    }

    @Test
    fun `every palette fills the extended Material3 surface roles from Wiccan colors`() {
        WiccanPalette.entries.forEach { palette ->
            val scheme = palette.toColorScheme()
            assertEquals(scheme.primary, scheme.surfaceTint)
            assertNotEquals(scheme.surface, scheme.surfaceContainerHigh)
            assertNotEquals(scheme.outline, scheme.outlineVariant)
        }
    }
}
