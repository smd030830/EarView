package com.example.earview.detection

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Vibrates through the system vibrator, doing nothing on devices without one.
 *
 * Vibrations are tagged as alarms: since Android 10 a vibration from a background app is dropped
 * unless it is an alarm, ringtone or notification, and detection runs while the app is in the background.
 */
class AndroidAlertVibrator(context: Context) : AlertVibrator {
  private val vibrator: Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

  private val alarmAttributes =
    AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_ALARM)
      .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
      .build()

  override fun vibrate(sound: DangerSound) {
    val vibrator = vibrator?.takeIf { it.hasVibrator() } ?: return
    val effect = VibrationEffect.createWaveform(sound.vibrationTimings, NO_REPEAT)
    @Suppress("DEPRECATION")
    vibrator.vibrate(effect, alarmAttributes)
  }

  override fun cancel() {
    vibrator?.cancel()
  }

  private companion object {
    const val NO_REPEAT = -1
  }
}
