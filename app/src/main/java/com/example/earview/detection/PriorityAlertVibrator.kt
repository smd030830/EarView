package com.example.earview.detection

/**
 * Keeps a lower-priority detection from replacing a more urgent vibration (BL-008).
 *
 * Calls and [elapsedRealtimeMillis] must use the same monotonic clock/thread. Higher-priority
 * sounds interrupt immediately. Suppressed sounds are not queued, avoiding stale alerts later.
 */
internal class PriorityAlertVibrator(
  private val delegate: AlertVibrator,
  private val elapsedRealtimeMillis: () -> Long,
) : AlertVibrator {
  private var activeSound: DangerSound? = null
  private var startedAtMillis = 0L
  private var durationMillis = 0L

  override fun vibrate(sound: DangerSound) {
    val now = elapsedRealtimeMillis()
    val active = activeSound
    if (active != null && now - startedAtMillis < durationMillis && sound >= active) return

    delegate.vibrate(sound)
    activeSound = sound
    startedAtMillis = now
    durationMillis = sound.vibrationTimings.sum()
  }

  override fun cancel() {
    activeSound = null
    startedAtMillis = 0L
    durationMillis = 0L
    delegate.cancel()
  }
}
