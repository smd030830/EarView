package com.example.earview.ui.main

import com.example.earview.detection.DangerSound
import com.example.earview.detection.DetectionController
import com.example.earview.detection.DetectionServiceStarter
import com.example.earview.detection.DetectionState
import com.example.earview.detection.SoundDetector
import com.example.earview.permission.FakePermissionChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {
  private val events = MutableSharedFlow<DangerSound>()
  private val detector = SoundDetector { events }
  private val permissions = FakePermissionChecker()

  private class Fixture(val controller: DetectionController, val viewModel: MainScreenViewModel, val starter: FakeServiceStarter)

  /** Stands in for the foreground service: starting it starts the detection on the shared controller. */
  private class FakeServiceStarter(private val controller: () -> DetectionController) : DetectionServiceStarter {
    var starts = 0

    override fun start() {
      starts++
      controller().start()
    }
  }

  private fun TestScope.fixture(scope: CoroutineScope = backgroundScope): Fixture {
    lateinit var controller: DetectionController
    val starter = FakeServiceStarter { controller }
    controller = DetectionController(detector, scope)
    return Fixture(controller, MainScreenViewModel(controller, starter, permissions), starter).also { runCurrent() }
  }

  @Test
  fun start_launchesService_andMovesToIdle() = runTest {
    val f = fixture()

    f.viewModel.onToggleDetection()
    runCurrent()

    assertEquals(1, f.starter.starts)
    assertEquals(DetectionState.Idle, f.viewModel.uiState.value)
    assertFalse(f.viewModel.permissionGuideVisible.value)
  }

  @Test
  fun stop_whileRunning_movesToOff() = runTest {
    val f = fixture()
    f.viewModel.onToggleDetection()
    runCurrent()

    f.viewModel.onToggleDetection()
    runCurrent()

    assertEquals(DetectionState.Off, f.viewModel.uiState.value)
    assertEquals(1, f.starter.starts)
  }

  @Test
  fun stateIsKeptWhenViewModelIsRecreated() = runTest {
    val f = fixture()
    f.viewModel.onToggleDetection()
    runCurrent()
    events.emit(DangerSound.SIREN)
    runCurrent()

    val recreated = MainScreenViewModel(f.controller, f.starter, permissions)

    assertEquals(DetectionState.Detected(listOf(DangerSound.SIREN), 0), recreated.uiState.value)
  }

  @Test
  fun dismiss_returnsToIdle() = runTest {
    val f = fixture()
    f.viewModel.onToggleDetection()
    runCurrent()
    events.emit(DangerSound.FIRE_ALARM)
    runCurrent()

    f.viewModel.onDismissDetection()

    assertEquals(DetectionState.Idle, f.viewModel.uiState.value)
  }

  // Microphone permission (BL-022)

  @Test
  fun startWithoutMicrophonePermission_doesNotStart_andShowsGuide() = runTest {
    permissions.microphoneGranted = false
    val f = fixture()

    f.viewModel.onToggleDetection()
    runCurrent()

    assertEquals(0, f.starter.starts)
    assertEquals(DetectionState.Off, f.viewModel.uiState.value)
    assertTrue(f.viewModel.permissionGuideVisible.value)
  }

  @Test
  fun dismissPermissionGuide_hidesIt() = runTest {
    permissions.microphoneGranted = false
    val f = fixture()
    f.viewModel.onToggleDetection()

    f.viewModel.onDismissPermissionGuide()

    assertFalse(f.viewModel.permissionGuideVisible.value)
    assertEquals(DetectionState.Off, f.viewModel.uiState.value)
  }

  @Test
  fun startAfterGrantingPermission_starts() = runTest {
    permissions.microphoneGranted = false
    val f = fixture()
    f.viewModel.onToggleDetection()
    f.viewModel.onDismissPermissionGuide()

    permissions.microphoneGranted = true
    f.viewModel.onToggleDetection()
    runCurrent()

    assertEquals(DetectionState.Idle, f.viewModel.uiState.value)
    assertFalse(f.viewModel.permissionGuideVisible.value)
  }

  @Test
  fun notificationPermissionMissing_doesNotBlockStart() = runTest {
    permissions.notificationGranted = false
    val f = fixture()

    f.viewModel.onToggleDetection()
    runCurrent()

    assertEquals(DetectionState.Idle, f.viewModel.uiState.value)
  }
}
