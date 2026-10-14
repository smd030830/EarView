package com.example.earview.ui.main

import com.example.earview.detection.DangerSound
import com.example.earview.detection.SoundDetector
import com.example.earview.permission.FakePermissionChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {
  private val detector = FakeSoundDetector()
  private val permissions = FakePermissionChecker()

  @Before
  fun setUp() {
    Dispatchers.setMain(StandardTestDispatcher())
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun TestScope.createViewModel() = MainScreenViewModel(detector, permissions).also { runCurrent() }

  private fun TestScope.start(viewModel: MainScreenViewModel) {
    viewModel.onToggleDetection()
    runCurrent()
  }

  private suspend fun TestScope.detect(vararg sounds: DangerSound) {
    sounds.forEach { detector.emit(it) }
    runCurrent()
  }

  private fun TestScope.advance(millis: Long) {
    advanceTimeBy(millis)
    runCurrent()
  }

  private fun detected(vararg sounds: DangerSound, overflow: Int = 0) = MainScreenUiState.Detected(sounds.toList(), overflow)

  // Off / Idle

  @Test
  fun initialState_isOff_andNotListening() = runTest {
    val viewModel = createViewModel()

    assertEquals(MainScreenUiState.Off, viewModel.uiState.value)
    assertFalse(detector.isListening)
  }

  @Test
  fun start_movesToIdle_andStartsListening() = runTest {
    val viewModel = createViewModel()

    start(viewModel)

    assertEquals(MainScreenUiState.Idle, viewModel.uiState.value)
    assertTrue(detector.isListening)
  }

  @Test
  fun stopFromIdle_movesToOff_andStopsListening() = runTest {
    val viewModel = createViewModel()
    start(viewModel)

    viewModel.onToggleDetection()
    runCurrent()

    assertEquals(MainScreenUiState.Off, viewModel.uiState.value)
    assertFalse(detector.isListening)
  }

  @Test
  fun startAfterStop_returnsToIdle() = runTest {
    val viewModel = createViewModel()
    start(viewModel)
    viewModel.onToggleDetection()
    runCurrent()

    start(viewModel)

    assertEquals(MainScreenUiState.Idle, viewModel.uiState.value)
    assertTrue(detector.isListening)
  }

  // Detected

  @Test
  fun detection_movesIdleToDetected() = runTest {
    val viewModel = createViewModel()
    start(viewModel)

    detect(DangerSound.FIRE_ALARM)

    assertEquals(detected(DangerSound.FIRE_ALARM), viewModel.uiState.value)
  }

  @Test
  fun stopWhileDetected_movesToOff_andClearsSounds() = runTest {
    val viewModel = createViewModel()
    start(viewModel)
    detect(DangerSound.SIREN)

    viewModel.onToggleDetection()
    runCurrent()

    assertEquals(MainScreenUiState.Off, viewModel.uiState.value)
    assertFalse(detector.isListening)

    start(viewModel)
    assertEquals(MainScreenUiState.Idle, viewModel.uiState.value)
  }

  @Test
  fun pendingTimers_afterStop_doNotChangeState() = runTest {
    val viewModel = createViewModel()
    start(viewModel)
    detect(DangerSound.SIREN)
    viewModel.onToggleDetection()
    runCurrent()

    advance(ALERT_AUTO_RETURN_MILLIS)

    assertEquals(MainScreenUiState.Off, viewModel.uiState.value)
  }

  // Auto-return after 10s

  @Test
  fun detected_returnsToIdle_after10Seconds() = runTest {
    val viewModel = createViewModel()
    start(viewModel)
    detect(DangerSound.FIRE_ALARM)

    advance(ALERT_AUTO_RETURN_MILLIS - 1)
    assertTrue(viewModel.uiState.value is MainScreenUiState.Detected)

    advance(1)
    assertEquals(MainScreenUiState.Idle, viewModel.uiState.value)
    assertTrue(detector.isListening)
  }

  @Test
  fun newDetection_restartsAutoReturnTimer() = runTest {
    val viewModel = createViewModel()
    start(viewModel)
    detect(DangerSound.FIRE_ALARM)
    advance(6_000)

    detect(DangerSound.FIRE_ALARM)
    advance(ALERT_AUTO_RETURN_MILLIS - 1)
    assertTrue(viewModel.uiState.value is MainScreenUiState.Detected)

    advance(1)
    assertEquals(MainScreenUiState.Idle, viewModel.uiState.value)
  }

  // Tap to return

  @Test
  fun tapWhileDetected_returnsToIdleImmediately() = runTest {
    val viewModel = createViewModel()
    start(viewModel)
    detect(DangerSound.FIRE_ALARM)

    viewModel.onDismissDetection()

    assertEquals(MainScreenUiState.Idle, viewModel.uiState.value)
    assertTrue(detector.isListening)
  }

  @Test
  fun tapWhileDetected_cancelsPendingTimers() = runTest {
    val viewModel = createViewModel()
    start(viewModel)
    detect(DangerSound.FIRE_ALARM)
    viewModel.onDismissDetection()

    advance(ALERT_AUTO_RETURN_MILLIS)

    assertEquals(MainScreenUiState.Idle, viewModel.uiState.value)
  }

  @Test
  fun detectionAfterTap_movesToDetectedAgain_withoutOldSounds() = runTest {
    val viewModel = createViewModel()
    start(viewModel)
    detect(DangerSound.FIRE_ALARM)
    viewModel.onDismissDetection()

    detect(DangerSound.DOORBELL)

    assertEquals(detected(DangerSound.DOORBELL), viewModel.uiState.value)
  }

  @Test
  fun tapWhileIdle_isIgnored() = runTest {
    val viewModel = createViewModel()
    start(viewModel)

    viewModel.onDismissDetection()

    assertEquals(MainScreenUiState.Idle, viewModel.uiState.value)
  }

  @Test
  fun tapWhileOff_isIgnored() = runTest {
    val viewModel = createViewModel()

    viewModel.onDismissDetection()

    assertEquals(MainScreenUiState.Off, viewModel.uiState.value)
  }

  // Multiple sounds: order, max 3, 3s removal

  @Test
  fun simultaneousSounds_areOrderedByDanger() = runTest {
    val viewModel = createViewModel()
    start(viewModel)

    detect(DangerSound.CAR_HORN, DangerSound.SIREN)

    assertEquals(detected(DangerSound.SIREN, DangerSound.CAR_HORN), viewModel.uiState.value)
  }

  @Test
  fun moreThanThreeSounds_showsThreeAndOverflowCount() = runTest {
    val viewModel = createViewModel()
    start(viewModel)

    detect(*DangerSound.entries.reversed().toTypedArray())

    assertEquals(
      detected(DangerSound.FIRE_ALARM, DangerSound.SIREN, DangerSound.BABY_CRYING, overflow = 2),
      viewModel.uiState.value,
    )
  }

  @Test
  fun exactlyThreeSounds_hasNoOverflow() = runTest {
    val viewModel = createViewModel()
    start(viewModel)

    detect(DangerSound.FIRE_ALARM, DangerSound.SIREN, DangerSound.DOORBELL)

    assertEquals(0, (viewModel.uiState.value as MainScreenUiState.Detected).overflowCount)
  }

  @Test
  fun sound_isRemovedFromList_3SecondsAfterLastDetection() = runTest {
    val viewModel = createViewModel()
    start(viewModel)
    detect(DangerSound.SIREN)
    advance(1_000)
    detect(DangerSound.CAR_HORN)

    advance(SOUND_EXPIRY_MILLIS - 1_000)

    assertEquals(detected(DangerSound.CAR_HORN), viewModel.uiState.value)
  }

  @Test
  fun sound_justBeforeExpiry_isStillListed() = runTest {
    val viewModel = createViewModel()
    start(viewModel)
    detect(DangerSound.SIREN, DangerSound.CAR_HORN)

    advance(SOUND_EXPIRY_MILLIS - 1)

    assertEquals(detected(DangerSound.SIREN, DangerSound.CAR_HORN), viewModel.uiState.value)
  }

  @Test
  fun redetectedSound_restartsItsExpiryTimer() = runTest {
    val viewModel = createViewModel()
    start(viewModel)
    detect(DangerSound.SIREN)
    advance(2_000)
    detect(DangerSound.SIREN)

    advance(2_000)

    assertEquals(detected(DangerSound.SIREN), viewModel.uiState.value)
  }

  @Test
  fun overflowCount_shrinksWhenSoundsExpire() = runTest {
    val viewModel = createViewModel()
    start(viewModel)
    detect(DangerSound.FIRE_ALARM, DangerSound.SIREN, DangerSound.BABY_CRYING, DangerSound.DOORBELL)
    assertEquals(1, (viewModel.uiState.value as MainScreenUiState.Detected).overflowCount)
    advance(2_000)
    detect(DangerSound.DOORBELL)

    advance(1_000)

    assertEquals(detected(DangerSound.DOORBELL), viewModel.uiState.value)
  }

  @Test
  fun allSoundsExpired_staysDetectedWithEmptyList_untilAutoReturn() = runTest {
    val viewModel = createViewModel()
    start(viewModel)
    detect(DangerSound.FIRE_ALARM)

    advance(SOUND_EXPIRY_MILLIS)
    assertEquals(detected(), viewModel.uiState.value)

    advance(ALERT_AUTO_RETURN_MILLIS - SOUND_EXPIRY_MILLIS)
    assertEquals(MainScreenUiState.Idle, viewModel.uiState.value)
  }

  // Microphone permission (BL-022)

  @Test
  fun startWithoutMicrophonePermission_doesNotStart_andShowsGuide() = runTest {
    permissions.microphoneGranted = false
    val viewModel = createViewModel()

    start(viewModel)

    assertEquals(MainScreenUiState.Off, viewModel.uiState.value)
    assertFalse(detector.isListening)
    assertTrue(viewModel.permissionGuideVisible.value)
  }

  @Test
  fun dismissPermissionGuide_hidesIt() = runTest {
    permissions.microphoneGranted = false
    val viewModel = createViewModel()
    start(viewModel)

    viewModel.onDismissPermissionGuide()

    assertFalse(viewModel.permissionGuideVisible.value)
    assertEquals(MainScreenUiState.Off, viewModel.uiState.value)
  }

  @Test
  fun startAfterGrantingPermission_starts_withoutGuide() = runTest {
    permissions.microphoneGranted = false
    val viewModel = createViewModel()
    start(viewModel)
    viewModel.onDismissPermissionGuide()

    permissions.microphoneGranted = true
    start(viewModel)

    assertEquals(MainScreenUiState.Idle, viewModel.uiState.value)
    assertTrue(detector.isListening)
    assertFalse(viewModel.permissionGuideVisible.value)
  }

  @Test
  fun startWithPermission_doesNotShowGuide() = runTest {
    val viewModel = createViewModel()

    start(viewModel)

    assertFalse(viewModel.permissionGuideVisible.value)
  }

  @Test
  fun notificationPermissionMissing_doesNotBlockStart() = runTest {
    permissions.notificationGranted = false
    val viewModel = createViewModel()

    start(viewModel)

    assertEquals(MainScreenUiState.Idle, viewModel.uiState.value)
  }

  // Detector ending on its own

  @Test
  fun detectorFailure_movesToOff() = runTest {
    val failingDetector = SoundDetector { flow { throw IllegalStateException("microphone lost") } }
    val viewModel = MainScreenViewModel(failingDetector, permissions)

    start(viewModel)

    assertEquals(MainScreenUiState.Off, viewModel.uiState.value)
  }

  @Test
  fun detectorCompletion_movesToOff_andCanBeRestarted() = runTest {
    var completes = true
    val detector = SoundDetector { if (completes) flow { } else flow<DangerSound> { awaitCancellation() } }
    val viewModel = MainScreenViewModel(detector, permissions)
    start(viewModel)
    assertEquals(MainScreenUiState.Off, viewModel.uiState.value)

    completes = false
    start(viewModel)

    assertEquals(MainScreenUiState.Idle, viewModel.uiState.value)
  }
}

/** Test double that lets tests emit detections and observe whether anything is listening. */
private class FakeSoundDetector : SoundDetector {
  private val events = MutableSharedFlow<DangerSound>()

  val isListening: Boolean
    get() = events.subscriptionCount.value > 0

  override fun detect(): Flow<DangerSound> = events

  suspend fun emit(sound: DangerSound) = events.emit(sound)
}
