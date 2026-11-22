package com.jaimesorrow.wiccan.ui.auth

import com.jaimesorrow.wiccan.domain.model.AuthState
import com.jaimesorrow.wiccan.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map

/**
 * Emits the new uid (null = signed out) each time the signed-in account
 * changes, skipping the current one. The pre-app screens' ViewModels are
 * activity-scoped and outlive sign-in/sign-out, so they use this to drop
 * whatever the previous account typed or chose.
 */
fun AuthRepository.accountChanges(): Flow<String?> = observeAuthState()
    .map { (it as? AuthState.SignedIn)?.uid }
    .distinctUntilChanged()
    .drop(1)
