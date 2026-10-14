package com.example.earview.detection

import kotlinx.coroutines.flow.Flow

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
