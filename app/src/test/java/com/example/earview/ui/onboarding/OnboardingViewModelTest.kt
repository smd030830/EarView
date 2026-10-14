package com.example.earview.ui.onboarding

import com.example.earview.permission.FakePermissionChecker
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingViewModelTest {
  private val permissions = FakePermissionChecker(microphoneGranted = false, notificationGranted = false)

  private fun state(microphone: PermissionStatus, notification: PermissionStatus) = OnboardingUiState(microphone, notification)

  @Test
  fun initialState_reflectsSystemPermissions() {
    permissions.microphoneGranted = true

    val viewModel = OnboardingViewModel(permissions)

    assertEquals(state(PermissionStatus.Granted, PermissionStatus.Required), viewModel.uiState.value)
  }

  @Test
  fun microphoneResult_updatesOnlyMicrophone() {
    val viewModel = OnboardingViewModel(permissions)

    viewModel.onMicrophoneResult(granted = true)

    assertEquals(state(PermissionStatus.Granted, PermissionStatus.Required), viewModel.uiState.value)
  }

  @Test
  fun notificationDenied_isMarkedDenied() {
    val viewModel = OnboardingViewModel(permissions)

    viewModel.onNotificationResult(granted = false)

    assertEquals(state(PermissionStatus.Required, PermissionStatus.Denied), viewModel.uiState.value)
  }

  @Test
  fun refresh_keepsDenied_whileStillNotGranted() {
    val viewModel = OnboardingViewModel(permissions)
    viewModel.onMicrophoneResult(granted = false)

    viewModel.refresh()

    assertEquals(PermissionStatus.Denied, viewModel.uiState.value.microphone)
  }

  @Test
  fun refresh_afterGrantingInSettings_becomesGranted() {
    val viewModel = OnboardingViewModel(permissions)
    viewModel.onMicrophoneResult(granted = false)

    permissions.microphoneGranted = true
    viewModel.refresh()

    assertEquals(PermissionStatus.Granted, viewModel.uiState.value.microphone)
  }

  @Test
  fun refresh_afterRevokingInSettings_asksAgain() {
    permissions.microphoneGranted = true
    val viewModel = OnboardingViewModel(permissions)

    permissions.microphoneGranted = false
    viewModel.refresh()

    assertEquals(PermissionStatus.Required, viewModel.uiState.value.microphone)
  }
}
