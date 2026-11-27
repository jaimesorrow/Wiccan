package com.jaimesorrow.wiccan.data.repository

import android.content.Context
import android.content.res.Resources
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.jaimesorrow.wiccan.analytics.AnalyticsLogger
import com.jaimesorrow.wiccan.core.error.AppError
import com.jaimesorrow.wiccan.core.result.AppResult
import com.jaimesorrow.wiccan.data.remote.AccountFunctionsDataSource
import com.jaimesorrow.wiccan.data.remote.GoogleCredentialDataSource
import com.jaimesorrow.wiccan.domain.model.ProfileName
import com.jaimesorrow.wiccan.ui.settings.AppLocaleProvider
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    private val accountFunctionsDataSource: AccountFunctionsDataSource = mockk()
    private val appLocaleProvider: AppLocaleProvider = mockk()
    private val analyticsLogger: AnalyticsLogger = mockk(relaxed = true)
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        every { appContext.resources } returns resources
        every { appContext.packageName } returns "com.jaimesorrow.wiccan"
        every { appLocaleProvider.languageTag() } returns "en"
        repository = AuthRepositoryImpl(
            appContext,
            firebaseAuth,
            firestore,
            googleCredentialDataSource,
            accountFunctionsDataSource,
            appLocaleProvider,
            analyticsLogger,
        )
    }

    @Test
    fun `verification email uses the branded callable`() = runTest {
        val user: FirebaseUser = mockk()
        every { firebaseAuth.currentUser } returns user
        every { user.isEmailVerified } returns false
        coEvery { accountFunctionsDataSource.sendVerificationEmail("en") } returns Unit

        assertEquals(AppResult.Success(Unit), repository.sendEmailVerification())

        coVerify(exactly = 1) { accountFunctionsDataSource.sendVerificationEmail("en") }
        verify(exactly = 0) { user.sendEmailVerification() }
    }

    @Test
    fun `verification email forwards the resolved app locale`() = runTest {
        val user: FirebaseUser = mockk()
        every { firebaseAuth.currentUser } returns user
        every { user.isEmailVerified } returns false
        every { appLocaleProvider.languageTag() } returns "pt-BR"
        coEvery { accountFunctionsDataSource.sendVerificationEmail("pt-BR") } returns Unit

        assertEquals(AppResult.Success(Unit), repository.sendEmailVerification())

        coVerify(exactly = 1) { accountFunctionsDataSource.sendVerificationEmail("pt-BR") }
    }

    @Test
    fun `verified account does not request another email`() = runTest {
        val user: FirebaseUser = mockk()
        every { firebaseAuth.currentUser } returns user
        every { user.isEmailVerified } returns true

        assertEquals(AppResult.Success(Unit), repository.sendEmailVerification())

        coVerify(exactly = 0) { accountFunctionsDataSource.sendVerificationEmail("en") }
    }

    @Test
    fun `account creation records signup and username when verification delivery fails`() = runTest {
        val user: FirebaseUser = mockk()
        every { user.uid } returns "uid-new"
        every { firebaseAuth.currentUser } returns user
        every { firebaseAuth.createUserWithEmailAndPassword("new@example.com", "secret1") } returns
            completedTask<AuthResult>(mockk())
        coEvery { accountFunctionsDataSource.reserveUsername("new_witch") } returns Unit
        coEvery { accountFunctionsDataSource.sendVerificationEmail("en") } throws IllegalStateException("smtp unavailable")

        val result = repository.createAccountWithEmail("new@example.com", "secret1", "new_witch")

        assertEquals(AppResult.Failure(AppError.VerificationEmailDeliveryFailed), result)
        verify(exactly = 1) { analyticsLogger.logSignUpCompleted() }
        coVerify(exactly = 1) { accountFunctionsDataSource.reserveUsername("new_witch") }
        coVerify(exactly = 1) { accountFunctionsDataSource.sendVerificationEmail("en") }
        assertTrue(repository.consumeVerificationDeliveryFailure())
        assertFalse(repository.consumeVerificationDeliveryFailure())
    }

    @Test
    fun `verification delivery failure is not exposed to a different signed in account`() = runTest {
        val createdUser: FirebaseUser = mockk()
        every { createdUser.uid } returns "uid-created"
        every { firebaseAuth.currentUser } returns createdUser
        every { firebaseAuth.createUserWithEmailAndPassword(any(), any()) } returns
            completedTask<AuthResult>(mockk())
        coEvery { accountFunctionsDataSource.reserveUsername(any()) } returns Unit
        coEvery { accountFunctionsDataSource.sendVerificationEmail("en") } throws IllegalStateException("smtp unavailable")
        repository.createAccountWithEmail("new@example.com", "secret1", "new_witch")

        val otherUser: FirebaseUser = mockk()
        every { otherUser.uid } returns "uid-other"
        every { firebaseAuth.currentUser } returns otherUser

        assertFalse(repository.consumeVerificationDeliveryFailure())
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

    @Test
    fun `dismissing the Google account picker is reported as cancelled`() = runTest {
        configureGoogleClient()
        coEvery { googleCredentialDataSource.requestGoogleIdToken(activityContext, any()) } throws
            GetCredentialCancellationException()

        assertEquals(AppResult.Failure(AppError.Cancelled), repository.signInWithGoogle(activityContext))
        verify { firebaseAuth wasNot Called }
    }

    @Test
    fun `a device without a Google account falls back to email sign-in`() = runTest {
        configureGoogleClient()
        coEvery { googleCredentialDataSource.requestGoogleIdToken(activityContext, any()) } throws
            NoCredentialException()

        assertEquals(
            AppResult.Failure(AppError.GoogleSignInUnavailable),
            repository.signInWithGoogle(activityContext),
        )
        verify { firebaseAuth wasNot Called }
    }

    private fun configureGoogleClient() {
        every { resources.getIdentifier("default_web_client_id", "string", "com.jaimesorrow.wiccan") } returns 1
        every { appContext.getString(1) } returns "configured-client.apps.googleusercontent.com"
    }

    private fun userDoc(uid: String, task: Task<DocumentSnapshot>) {
        val user: FirebaseUser = mockk()
        every { user.uid } returns uid
        every { firebaseAuth.currentUser } returns user
        every { firestore.collection("users").document(uid).get() } returns task
    }

    private fun <T> completedTask(result: T?, error: Exception? = null): Task<T> {
        val task: Task<T> = mockk()
        every { task.isComplete } returns true
        every { task.isCanceled } returns false
        every { task.exception } returns error
        every { task.result } returns result
        return task
    }

    @Test
    fun `profile name is read from the signed-in user's document`() = runTest {
        val snapshot: DocumentSnapshot = mockk()
        every { snapshot.getString("firstName") } returns "Raven"
        every { snapshot.getString("lastName") } returns "Moon"
        every { snapshot.getString("username") } returns "raven_moon"
        userDoc("uid-1", completedTask(snapshot))

        assertEquals(ProfileName("Raven", "Moon", "raven_moon"), repository.currentProfileName())
    }

    @Test
    fun `profile name is null when signed out, blank, or unreadable`() = runTest {
        every { firebaseAuth.currentUser } returns null
        assertNull(repository.currentProfileName())

        val blank: DocumentSnapshot = mockk()
        every { blank.getString(any()) } returns null
        userDoc("uid-2", completedTask(blank))
        assertNull(repository.currentProfileName())

        userDoc("uid-3", completedTask(null, IllegalStateException("offline")))
        assertNull(repository.currentProfileName())
    }
}
