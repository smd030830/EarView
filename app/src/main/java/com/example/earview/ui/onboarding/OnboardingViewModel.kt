package com.example.earview.ui.onboarding

import androidx.lifecycle.ViewModel
import com.example.earview.permission.PermissionChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PermissionStatus {
  /** Not granted yet; the system dialog can still be shown. */
  Required,

  /** The user refused. The system dialog may no longer appear, so send them to settings (BL-023). */
  Denied,

  Granted,
}

data class OnboardingUiState(val microphone: PermissionStatus, val notification: PermissionStatus)

class OnboardingViewModel(private val permissions: PermissionChecker) : ViewModel() {
  private val _uiState = MutableStateFlow(OnboardingUiState(PermissionStatus.Required, PermissionStatus.Required))
  val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

  init {
    refresh()
  }

  /** Re-reads the system state, e.g. after returning from the settings screen. */
  fun refresh() {
    _uiState.value =
      OnboardingUiState(
        microphone = resolve(permissions.isMicrophoneGranted(), _uiState.value.microphone),
        notification = resolve(permissions.isNotificationGranted(), _uiState.value.notification),
      )
  }

  fun onMicrophoneResult(granted: Boolean) {
    _uiState.value = _uiState.value.copy(microphone = if (granted) PermissionStatus.Granted else PermissionStatus.Denied)
  }

  fun onNotificationResult(granted: Boolean) {
    _uiState.value = _uiState.value.copy(notification = if (granted) PermissionStatus.Granted else PermissionStatus.Denied)
  }

  private fun resolve(granted: Boolean, previous: PermissionStatus): PermissionStatus = when {
    granted -> PermissionStatus.Granted
    // Revoked in settings after being granted: ask again rather than claiming it was refused.
    previous == PermissionStatus.Granted -> PermissionStatus.Required
    else -> previous
  }
}
