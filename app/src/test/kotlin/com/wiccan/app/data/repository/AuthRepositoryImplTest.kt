package com.wiccan.app.data.repository

import android.content.Context
import android.content.res.Resources
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.wiccan.app.core.error.AppError
import com.wiccan.app.core.result.AppResult
import com.wiccan.app.data.remote.GoogleCredentialDataSource
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryImplTest {

    private val appContext: Context = mockk()
    private val activityContext: Context = mockk()
    private val resources: Resources = mockk()
    private val firebaseAuth: FirebaseAuth = mockk()
    private val firestore: FirebaseFirestore = mockk()
    private val googleCredentialDataSource: GoogleCredentialDataSource = mockk()
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        every { appContext.resources } returns resources
        every { appContext.packageName } returns "com.jaimesorrow.wiccan"
        repository = AuthRepositoryImpl(
            appContext,
            firebaseAuth,
            firestore,
            googleCredentialDataSource,
            mockk(),
            mockk(),
        )
    }

    @Test
    fun `missing OAuth configuration prevents Google credential and Firebase calls`() = runTest {
        every { resources.getIdentifier("default_web_client_id", "string", "com.jaimesorrow.wiccan") } returns 0

        assertFalse(repository.isGoogleSignInAvailable())
        val result = repository.signInWithGoogle(activityContext)

        assertEquals(AppResult.Failure(AppError.GoogleSignInUnavailable), result)
        verify {
            googleCredentialDataSource wasNot Called
            firebaseAuth wasNot Called
            firestore wasNot Called
        }
    }

    @Test
    fun `blank OAuth configuration is unavailable`() = runTest {
        every { resources.getIdentifier("default_web_client_id", "string", "com.jaimesorrow.wiccan") } returns 1
        every { appContext.getString(1) } returns "   "

        assertFalse(repository.isGoogleSignInAvailable())
        assertEquals(
            AppResult.Failure(AppError.GoogleSignInUnavailable),
            repository.signInWithGoogle(activityContext),
        )
        verify {
            googleCredentialDataSource wasNot Called
            firebaseAuth wasNot Called
        }
    }

    @Test
    fun `configured Google sign-in uses the OAuth client and Activity context`() = runTest {
        every { resources.getIdentifier("default_web_client_id", "string", "com.jaimesorrow.wiccan") } returns 1
        every { appContext.getString(1) } returns "configured-client.apps.googleusercontent.com"
        coEvery {
            googleCredentialDataSource.requestGoogleIdToken(activityContext, "configured-client.apps.googleusercontent.com")
        } throws IllegalStateException("account picker cancelled")

        assertTrue(repository.isGoogleSignInAvailable())
        val result = repository.signInWithGoogle(activityContext)

        assertEquals(AppResult.Failure(AppError.Unknown("account picker cancelled")), result)
        coVerify(exactly = 1) {
            googleCredentialDataSource.requestGoogleIdToken(activityContext, "configured-client.apps.googleusercontent.com")
        }
        verify { firebaseAuth wasNot Called }
    }
}
