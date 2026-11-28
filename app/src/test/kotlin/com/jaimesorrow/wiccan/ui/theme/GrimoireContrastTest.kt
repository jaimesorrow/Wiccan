package com.jaimesorrow.wiccan.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class GrimoireContrastTest {
    @Test
    fun `near-black cards, gold, and parchment ink clear WCAG AA`() {
        val grimoire = WiccanPaletteColors.Grimoire
        val parchment = WiccanPaletteColors.Parchment
        assertTrue(contrast(grimoire.Text, grimoire.Background) >= 4.5)
        assertTrue(contrast(grimoire.Text, grimoire.CardFill) >= 4.5)
        assertTrue(contrast(grimoire.Text, grimoire.Raised) >= 4.5)
        assertTrue(contrast(grimoire.Muted, grimoire.CardFill) >= 4.5)
        assertTrue(contrast(grimoire.Muted, grimoire.Background) >= 4.5)
        assertTrue(contrast(grimoire.Gold, grimoire.CardFill) >= 4.5)
        assertTrue(contrast(grimoire.Gold, grimoire.Background) >= 4.5)
        listOf(grimoire.Background, grimoire.Raised, grimoire.CardFill).forEach { surface ->
            assertTrue(
                "Bronze must not be used as body/label text on dark Grimoire surfaces",
                contrast(grimoire.Bronze, surface) < 4.5,
            )
        }
        assertTrue(contrast(grimoire.OnGold, grimoire.Gold) >= 4.5)
        assertTrue(contrast(grimoire.OnGold, grimoire.Border) >= 4.5)
        assertTrue(contrast(parchment.Ink, parchment.Background) >= 4.5)
        assertTrue(contrast(grimoire.Bronze, parchment.Background) >= 4.5)
    }

    private fun contrast(foreground: Color, background: Color): Double {
        val lighter = maxOf(foreground.luminance(), background.luminance())
        val darker = minOf(foreground.luminance(), background.luminance())
        return (lighter + 0.05) / (darker + 0.05)
    }
}
