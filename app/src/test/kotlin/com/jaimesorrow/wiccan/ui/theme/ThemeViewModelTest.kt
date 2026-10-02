package com.jaimesorrow.wiccan.ui.theme

import com.jaimesorrow.wiccan.data.local.ThemePreferences
import com.jaimesorrow.wiccan.domain.model.WiccanThemeMode
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `default value is automatic seasonal before the DataStore flow emits`() {
        val preferences: ThemePreferences = mockk()
        every { preferences.themeMode } returns MutableStateFlow(WiccanThemeMode.DEFAULT)

        val viewModel = ThemeViewModel(preferences)

        assertEquals(WiccanThemeMode.AUTOMATIC_SEASONAL, viewModel.themeMode.value)
    }

    @Test
    fun `reflects whatever mode ThemePreferences reports`() = runTest(dispatcher) {
        val preferences: ThemePreferences = mockk()
        every { preferences.themeMode } returns MutableStateFlow(WiccanThemeMode.ROYAL_PLUM)

        val viewModel = ThemeViewModel(preferences)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(WiccanThemeMode.ROYAL_PLUM, viewModel.themeMode.value)
    }
}
