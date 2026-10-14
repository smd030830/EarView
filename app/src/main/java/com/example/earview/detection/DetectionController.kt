package com.example.earview.detection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** A sound that is no longer detected is removed from the displayed list after this long (FR-05). */
internal const val SOUND_EXPIRY_MILLIS = 3_000L

/** The detected (red) state returns to idle (green) this long after the last detection (FR-06). */
internal const val ALERT_AUTO_RETURN_MILLIS = 10_000L

/** At most this many sounds are listed; the rest are summarized as "N more" (FR-04). */
internal const val MAX_DISPLAYED_SOUNDS = 3

/** What the main screen shows. */
sealed interface DetectionState {
  /** Detection is not running (gray). */
  data object Off : DetectionState

  /** Detection is running and nothing is detected (green). */
  data object Idle : DetectionState

  /**
   * A danger sound was detected (red). [sounds] holds at most [MAX_DISPLAYED_SOUNDS] currently
   * detected sounds, most dangerous first, and [overflowCount] is how many more are not listed.
   * [sounds] can be empty while the screen is still red, once every sound has aged out of the list.
   */
  data class Detected(val sounds: List<DangerSound>, val overflowCount: Int) : DetectionState
}

/** Why detection stopped. */
enum class StopReason {
  /** The user pressed stop (in the app or in the notification). */
  User,

  /** The detector ended on its own or the system stopped the service (FR-10). */
  Unexpected,
}

/**
 * Single source of truth for detection state, shared by the UI and the foreground service.
 *
 * Exactly one instance lives in the application (see `EarViewApplication`), so detection keeps its
 * state while the UI comes and goes (BL-018). All calls must come from the thread [scope] runs on
 * (the main thread in production).
 */
class DetectionController(
  private val detector: SoundDetector,
  private val vibrator: AlertVibrator,
  private val scope: CoroutineScope,
) {
  private val _state = MutableStateFlow<DetectionState>(DetectionState.Off)
  val state: StateFlow<DetectionState> = _state.asStateFlow()

  private val _stopEvents = MutableSharedFlow<StopReason>(extraBufferCapacity = 1)

  /** Emits every time a running detection stops. */
  val stopEvents: SharedFlow<StopReason> = _stopEvents.asSharedFlow()

  var isRunning = false
    private set

  private var alerting = false
  private val activeSounds = mutableSetOf<DangerSound>()
  private val expiryJobs = mutableMapOf<DangerSound, Job>()
  private var detectionJob: Job? = null
  private var autoReturnJob: Job? = null

  /** Starts listening. Does nothing if already running. */
  fun start() {
    if (isRunning) return
    isRunning = true
    publish()
    detectionJob =
      scope.launch {
        detector.detect().catch { }.collect(::onSoundDetected)
        // Reached only when the detector ended on its own (cancellation skips this line).
        stop(StopReason.Unexpected)
      }
  }

  /** Stops listening. Does nothing if not running. */
  fun stop(reason: StopReason = StopReason.User) {
    if (!isRunning) return
    isRunning = false
    detectionJob?.cancel()
    detectionJob = null
    clearAlert()
    publish()
    _stopEvents.tryEmit(reason)
  }

  /** Tap on the screen while in the detected state (FR-07). Ignored in other states. */
  fun dismissAlert() {
    if (!alerting) return
    clearAlert()
    publish()
  }

  private fun onSoundDetected(sound: DangerSound) {
    if (!isRunning) return
    alerting = true
    // Vibrate only when the sound newly appears; a sound that keeps going vibrates once (FR-02).
    if (activeSounds.add(sound)) vibrator.vibrate(sound)

    expiryJobs.remove(sound)?.cancel()
    expiryJobs[sound] =
      scope.launch {
        delay(SOUND_EXPIRY_MILLIS)
        expiryJobs.remove(sound)
        activeSounds -= sound
        publish()
      }

    autoReturnJob?.cancel()
    autoReturnJob =
      scope.launch {
        delay(ALERT_AUTO_RETURN_MILLIS)
        clearAlert()
        publish()
      }

    publish()
  }

  private fun clearAlert() {
    vibrator.cancel()
    alerting = false
    activeSounds.clear()
    expiryJobs.values.forEach(Job::cancel)
    expiryJobs.clear()
    autoReturnJob?.cancel()
    autoReturnJob = null
  }

  private fun publish() {
    _state.value =
      when {
        !isRunning -> DetectionState.Off
        !alerting -> DetectionState.Idle
        else -> {
          val sorted = activeSounds.sorted()
          DetectionState.Detected(
            sounds = sorted.take(MAX_DISPLAYED_SOUNDS),
            overflowCount = (sorted.size - MAX_DISPLAYED_SOUNDS).coerceAtLeast(0),
          )
        }
      }
  }
}
