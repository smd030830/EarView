package com.example.earview.detection

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Source of danger-sound detections.
 *
 * Abstracted so the UI state logic can be tested without a microphone. Implementations must
 * process audio in memory only; no audio may be stored, logged, or transmitted.
 */
fun interface SoundDetector {
  /**
   * Cold flow of detections. Collecting it starts listening; cancelling the collection stops
   * listening. Each emission means [DangerSound] was heard in the latest analysis window.
   * The flow completing or failing means detection stopped unexpectedly.
   */
  fun detect(): Flow<DangerSound>
}

/** Placeholder until the on-device YAMNet detector (BL-005, BL-006) is implemented. */
class NoOpSoundDetector : SoundDetector {
  override fun detect(): Flow<DangerSound> = flow { awaitCancellation() }
}
