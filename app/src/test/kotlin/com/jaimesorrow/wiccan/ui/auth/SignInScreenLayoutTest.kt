package com.jaimesorrow.wiccan.ui.auth

import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import com.jaimesorrow.wiccan.ui.legal.LegalDocumentId
import com.jaimesorrow.wiccan.ui.theme.LocallyResolvableTypography
import com.jaimesorrow.wiccan.ui.theme.WiccanPalette
import com.jaimesorrow.wiccan.ui.theme.toColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w320dp-h640dp-xhdpi", application = android.app.Application::class)
class SignInScreenLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `compact sign in remains readable and interactive`() = verifyLayout(SignInMode.SIGN_IN, 1f)

    @Test
    fun `compact account creation remains readable and interactive`() = verifyLayout(SignInMode.CREATE_ACCOUNT, 1f)

    @Test
    fun `sign in scrolls to every action at 200 percent text`() = verifyLayout(SignInMode.SIGN_IN, 2f)

    @Test
    fun `account creation scrolls to every action at 200 percent text`() = verifyLayout(SignInMode.CREATE_ACCOUNT, 2f)

    private fun verifyLayout(mode: SignInMode, fontScale: Float) {
        val events = mutableListOf<SignInUiEvent>()
        val legal = mutableListOf<LegalDocumentId>()
        var googleClicks = 0
        composeRule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                MaterialTheme(
                    colorScheme = (if (mode == SignInMode.SIGN_IN) WiccanPalette.ROYAL_PLUM else WiccanPalette.LIGHT_IVORY)
                        .toColorScheme(),
                    typography = LocallyResolvableTypography,
                ) {
                    SignInScreenContent(
                        state = SignInUiState(mode = mode, isGoogleSignInAvailable = true),
                        onEvent = { events += it },
                        onGoogleSignInClick = { googleClicks++ },
                        onOpenLegalDocument = { legal += it },
                    )
                }
            }
        }
        composeRule.onNodeWithText(HEADLINE).assertIsDisplayed()
        for (text in listOf(HEADLINE, SUPPORTING_COPY)) {
            composeRule.onNodeWithText(text).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                val layouts = mutableListOf<TextLayoutResult>()
                assertTrue(action(layouts))
                assertTrue(layouts.isNotEmpty())
                assertFalse("Introductory text must wrap without truncation", layouts.any { it.hasVisualOverflow })
            }
        }
        val prefix = "build/outputs/roborazzi/wiccan-auth-${mode.name.lowercase()}-${(fontScale * 100).toInt()}"
        composeRule.onRoot().captureRoboImage(filePath = "$prefix-top.png")
        val glyph = composeRule.onNodeWithTag("auth-crescent").fetchSemanticsNode().boundsInRoot
        val bitmap = requireNotNull(BitmapFactory.decodeFile("$prefix-top.png"))
        assertEquals(
            "Decorative glyph must not clear the screen background",
            255,
            Color.alpha(bitmap.getPixel(glyph.center.x.toInt(), glyph.center.y.toInt())),
        )

        composeRule.onNodeWithText("Continue with Google").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(1, googleClicks)
        val submitLabel = if (mode == SignInMode.SIGN_IN) "Sign In" else "Create Account"
        composeRule.onNodeWithText(submitLabel).performScrollTo().assertIsDisplayed().performClick()
        assertTrue(events.contains(SignInUiEvent.SubmitEmailAuth))
        composeRule.onRoot().captureRoboImage(filePath = "$prefix-controls.png")

        val toggleLabel = if (mode == SignInMode.SIGN_IN) "Need an account? Create one" else "Have an account? Sign in"
        composeRule.onNodeWithText(toggleLabel).performScrollTo().assertIsDisplayed().performClick()
        if (mode == SignInMode.SIGN_IN) {
            composeRule.onNodeWithText("Forgot password?").performScrollTo().assertIsDisplayed().performClick()
            assertTrue(events.contains(SignInUiEvent.ForgotPasswordClicked))
        }
        assertTrue(events.contains(SignInUiEvent.ToggleMode))
        composeRule.onNodeWithText("Terms").performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Privacy").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(listOf(LegalDocumentId.TERMS, LegalDocumentId.PRIVACY), legal)
    }

    private companion object {
        const val HEADLINE = "Your path into Wicca begins here."
        const val SUPPORTING_COPY = "Explore candle spells, herbal rituals, cord and knot work, and lunar practices inspired by " +
            "historical grimoires. Learn through a new guided lesson each day—and build a personal Grimoire filled with your discoveries."
    }
}
