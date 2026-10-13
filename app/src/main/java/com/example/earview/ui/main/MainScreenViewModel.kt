package com.example.earview.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.earview.detection.DangerSound
import com.example.earview.detection.SoundDetector
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** A sound that is no longer detected is removed from the displayed list after this long (FR-05). */
internal const val SOUND_EXPIRY_MILLIS = 3_000L

/** The detected (red) state returns to idle (green) this long after the last detection (FR-06). */
internal const val ALERT_AUTO_RETURN_MILLIS = 10_000L

/** At most this many sounds are listed; the rest are summarized as "N more" (FR-04). */
internal const val MAX_DISPLAYED_SOUNDS = 3

class MainScreenViewModel(private val detector: SoundDetector) : ViewModel() {
  private val _uiState = MutableStateFlow<MainScreenUiState>(MainScreenUiState.Off)
  val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

  private var running = false
  private var alerting = false
  private val activeSounds = mutableSetOf<DangerSound>()
  private val expiryJobs = mutableMapOf<DangerSound, Job>()
  private var detectionJob: Job? = null
  private var autoReturnJob: Job? = null

  /** Start/stop button (FR-08). */
  fun onToggleDetection() {
    if (running) stopDetection() else startDetection()
  }

  /** Tap on the screen while in the detected state (FR-07). Ignored in other states. */
  fun onDismissDetection() {
    if (!alerting) return
    clearAlert()
    publish()
  }

  private fun startDetection() {
    running = true
    publish()
    detectionJob =
      viewModelScope.launch {
        // TODO(FR-10): notify the user when detection stops unexpectedly instead of only turning off.
        detector.detect().catch { }.collect(::onSoundDetected)
        // Reached only when the detector ended on its own (cancellation skips this line).
        stopDetection()
      }
  }

  private fun stopDetection() {
    running = false
    detectionJob?.cancel()
    detectionJob = null
    clearAlert()
    publish()
  }

  private fun onSoundDetected(sound: DangerSound) {
    if (!running) return
    alerting = true
    activeSounds += sound

    expiryJobs.remove(sound)?.cancel()
    expiryJobs[sound] =
      viewModelScope.launch {
        delay(SOUND_EXPIRY_MILLIS)
        expiryJobs.remove(sound)
        activeSounds -= sound
        publish()
      }

    autoReturnJob?.cancel()
    autoReturnJob =
      viewModelScope.launch {
        delay(ALERT_AUTO_RETURN_MILLIS)
        clearAlert()
        publish()
      }

    publish()
  }

  private fun clearAlert() {
    alerting = false
    activeSounds.clear()
    expiryJobs.values.forEach(Job::cancel)
    expiryJobs.clear()
    autoReturnJob?.cancel()
    autoReturnJob = null
  }

  private fun publish() {
    _uiState.value =
      when {
        !running -> MainScreenUiState.Off
        !alerting -> MainScreenUiState.Idle
        else -> {
          val sorted = activeSounds.sorted()
          MainScreenUiState.Detected(
            sounds = sorted.take(MAX_DISPLAYED_SOUNDS),
            overflowCount = (sorted.size - MAX_DISPLAYED_SOUNDS).coerceAtLeast(0),
          )
        }
      }
  }
}

sealed interface MainScreenUiState {
  /** Detection is not running (gray). */
  data object Off : MainScreenUiState

  /** Detection is running and nothing is detected (green). */
  data object Idle : MainScreenUiState

  /**
   * A danger sound was detected (red). [sounds] holds at most [MAX_DISPLAYED_SOUNDS] currently
   * detected sounds, most dangerous first, and [overflowCount] is how many more are not listed.
   * [sounds] can be empty while the screen is still red, once every sound has aged out of the list.
   */
  data class Detected(val sounds: List<DangerSound>, val overflowCount: Int) : MainScreenUiState
}
