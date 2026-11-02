package com.example.earview.detection

/** Vibrates the phone when a danger sound is detected (FR-02). */
interface AlertVibrator {
  /** Requests [sound]'s pattern; a priority-aware driver may suppress less urgent requests. */
  fun vibrate(sound: DangerSound)

  /** Stops any vibration in progress. */
  fun cancel()
}
