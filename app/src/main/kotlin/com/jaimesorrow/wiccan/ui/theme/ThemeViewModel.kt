package com.jaimesorrow.wiccan.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaimesorrow.wiccan.data.local.ThemePreferences
import com.jaimesorrow.wiccan.domain.model.WiccanThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Exposes the persisted [WiccanThemeMode] for [com.jaimesorrow.wiccan.MainActivity]
 * to wrap its whole composition in [WiccanTheme] with. Kept separate from
 * [com.jaimesorrow.wiccan.ui.auth.RootViewModel] since theme resolution has nothing
 * to do with auth/policy state and every screen (including the sign-in and
 * policy-acceptance screens) needs to render in the active palette.
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    themePreferences: ThemePreferences,
) : ViewModel() {
    // Eagerly, not WhileSubscribed: MainActivity's whole composition depends
    // on this being live the instant the theme mode changes, not only while
    // something happens to be actively collecting it.
    val themeMode: StateFlow<WiccanThemeMode> = themePreferences.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = WiccanThemeMode.DEFAULT,
    )
}
