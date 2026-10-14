package com.example.earview.ui.main

import androidx.lifecycle.ViewModel
import com.example.earview.detection.DetectionController
import com.example.earview.detection.DetectionServiceStarter
import com.example.earview.detection.DetectionState
import com.example.earview.permission.PermissionChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State and actions of the main screen. Detection state lives in the shared [DetectionController]
 * so it survives this ViewModel and keeps running in the foreground service (BL-018).
 */
class MainScreenViewModel(
  private val controller: DetectionController,
  private val serviceStarter: DetectionServiceStarter,
  private val permissions: PermissionChecker,
) : ViewModel() {
  val uiState: StateFlow<DetectionState> = controller.state

  private val _permissionGuideVisible = MutableStateFlow(false)

  /** True while the "microphone permission needed" guidance is shown (BL-022). */
  val permissionGuideVisible: StateFlow<Boolean> = _permissionGuideVisible.asStateFlow()

  /**
   * Start/stop button (FR-08). Starting without the microphone permission shows guidance instead
   * (FR-11). Otherwise the foreground service is started and it starts the detection.
   */
  fun onToggleDetection() {
    when {
      controller.isRunning -> controller.stop()
      !permissions.isMicrophoneGranted() -> _permissionGuideVisible.value = true
      else -> serviceStarter.start()
    }
  }

  /** Tap on the screen while in the detected state (FR-07). Ignored in other states. */
  fun onDismissDetection() = controller.dismissAlert()

  fun onDismissPermissionGuide() {
    _permissionGuideVisible.value = false
  }
}
