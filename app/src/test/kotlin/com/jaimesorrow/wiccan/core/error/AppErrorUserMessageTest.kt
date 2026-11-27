package com.jaimesorrow.wiccan.core.error

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.jaimesorrow.wiccan.ui.policy.PolicyAgreement
import com.jaimesorrow.wiccan.ui.policy.labelRes
import com.wiccan.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class AppErrorUserMessageTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `a wrong password is not the generic failure`() {
        val wrongPassword = FirebaseAuthInvalidCredentialsException("ERROR_WRONG_PASSWORD", "The password is invalid")
        val invalidCredential = FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "incorrect")
        val malformedEmail = FirebaseAuthInvalidCredentialsException("ERROR_INVALID_EMAIL", "bad email")

        assertEquals(AppError.InvalidCredentials, wrongPassword.toAppError())
        assertEquals(AppError.InvalidCredentials, invalidCredential.toAppError())
        assertTrue(malformedEmail.toAppError() is AppError.Unknown)

        val credentials = AppError.InvalidCredentials.toUserMessage(context.resources)
        assertEquals("The gate stayed shut. Check your email and password, then try again.", credentials)
        assertNotEquals(AppError.Unknown("The password is invalid").toUserMessage(context.resources), credentials)
    }

    @Test
    fun `restore failure names Google Play and is not the generic failure`() {
        val message = AppError.RestoreFailed.toUserMessage(context.resources)
        assertEquals("The restore faltered. Check this account in Google Play, then try again.", message)
        assertNotEquals(AppError.Unknown(null).toUserMessage(context.resources), message)
    }

    @Test
    fun `required agreements stay plain and specific`() {
        assertTrue(context.getString(PolicyAgreement.NO_GUARANTEE.labelRes()).contains("does not promise or guarantee any result"))
        assertTrue(context.getString(PolicyAgreement.ADULT.labelRes()).contains("18"))
        assertTrue(context.getString(R.string.settings_delete_body).contains("cannot be undone"))
        assertTrue(context.getString(R.string.paywall_trial_disclosure, 7, "$4.99").contains("No charge during the 7-day trial"))
    }

    @Test
    fun `a dropped connection tells the reader to check it`() {
        assertEquals(
            "The spell faltered. Check your connection and try again.",
            AppError.Network.toUserMessage(context.resources),
        )
    }

    @Test
    fun `unreadable content has its own message, not the generic failure`() {
        val message = AppError.UnsupportedContent.toUserMessage(context.resources)
        assertTrue(message.contains("update the app"))
        assertNotEquals(AppError.Unknown(null).toUserMessage(context.resources), message)
    }
}
