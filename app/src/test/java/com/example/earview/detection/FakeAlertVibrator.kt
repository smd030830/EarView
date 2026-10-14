package com.example.earview.detection

/** Records vibrations instead of touching hardware. */
class FakeAlertVibrator : AlertVibrator {
  val vibrations = mutableListOf<DangerSound>()
  var cancelCount = 0
    private set

  override fun vibrate(sound: DangerSound) {
    vibrations += sound
  }

  override fun cancel() {
    cancelCount++
  }
}
