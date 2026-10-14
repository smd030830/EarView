package com.example.earview.detection

/** Vibrates the phone when a danger sound is detected (FR-02). */
interface AlertVibrator {
  /** Vibrates with the pattern of [sound], replacing any vibration in progress. */
  fun vibrate(sound: DangerSound)

  /** Stops any vibration in progress. */
  fun cancel()
}
