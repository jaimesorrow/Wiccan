package com.jaimesorrow.wiccan.core.error

import com.google.firebase.functions.FirebaseFunctionsException
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ThrowableToAppErrorTest {

    @Test
    fun `cancellation is rethrown rather than classified`() {
        assertThrows(CancellationException::class.java) {
            CancellationException("cancelled").toAppError()
        }
    }

    @Test
    fun `Cloud Function codes map onto the shared taxonomy`() {
        assertEquals(AppError.Unauthorized, functionsError(FirebaseFunctionsException.Code.UNAUTHENTICATED))
        assertEquals(AppError.NotFound, functionsError(FirebaseFunctionsException.Code.NOT_FOUND))
        assertEquals(AppError.Network, functionsError(FirebaseFunctionsException.Code.UNAVAILABLE))
        assertEquals(AppError.Network, functionsError(FirebaseFunctionsException.Code.DEADLINE_EXCEEDED))
    }

    private fun functionsError(code: FirebaseFunctionsException.Code): AppError {
        val error = mockk<FirebaseFunctionsException>()
        every { error.code } returns code
        every { error.message } returns code.name
        return error.toAppError()
    }
}
