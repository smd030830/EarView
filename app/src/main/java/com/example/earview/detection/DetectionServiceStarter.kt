package com.example.earview.detection

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat

/** Starts the foreground service. Abstracted so the ViewModel can be tested without Android. */
fun interface DetectionServiceStarter {
  fun start()
}

class AndroidDetectionServiceStarter(context: Context) : DetectionServiceStarter {
  private val appContext = context.applicationContext

  override fun start() {
    try {
      ContextCompat.startForegroundService(appContext, DetectionService.startIntent(appContext))
    } catch (e: IllegalStateException) {
      // Background start not allowed (Android 12+). The state stays off, so the UI does not lie.
      Log.w(TAG, "Could not start the detection service", e)
    } catch (e: SecurityException) {
      Log.w(TAG, "Could not start the detection service", e)
    }
  }

  private companion object {
    const val TAG = "DetectionServiceStarter"
  }
}
