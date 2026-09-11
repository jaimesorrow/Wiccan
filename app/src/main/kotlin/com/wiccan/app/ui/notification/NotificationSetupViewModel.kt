package com.wiccan.app.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiccan.app.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationSetupViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    fun onPermissionGranted() {
        viewModelScope.launch { notificationRepository.refreshAndRegisterToken() }
    }
}
