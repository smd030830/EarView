package com.example.earview.detection

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.example.earview.detectionController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps detection alive while the app is in the background (BL-017, BL-018).
 *
 * Detection state itself lives in [DetectionController]; the service only keeps the process alive,
 * shows the pinned notification, and alerts the user when detection stops unexpectedly (BL-019).
 */
class DetectionService : Service() {
  private val controller: DetectionController
    get() = detectionController

  private lateinit var notifications: DetectionNotifications
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var watching = false

  override fun onCreate() {
    super.onCreate()
    notifications = DetectionNotifications(this)
    scope.launch { controller.stopEvents.alertOnUnexpectedStop(notifications) }
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    // Must be the first thing: the system crashes the app if this is not called soon after
    // startForegroundService, even when we are about to stop again.
    if (!enterForeground()) {
      stopSelf()
      return START_NOT_STICKY
    }

    when (intent?.action) {
      ACTION_START -> {
        notifications.clearStopped()
        controller.start()
        stopWhenDetectionEnds()
      }
      ACTION_STOP -> {
        controller.stop(StopReason.User)
        leaveForeground()
      }
      // Restarted by the system after the process was killed: detection is gone, so tell the user.
      else -> {
        notifications.showStopped()
        leaveForeground()
      }
    }
    return START_STICKY
  }

  override fun onDestroy() {
    scope.cancel()
    if (controller.isRunning) {
      // The system took the service away while detection was still wanted (FR-10).
      controller.stop(StopReason.Unexpected)
      notifications.showStopped()
    }
    super.onDestroy()
  }

  private fun enterForeground(): Boolean = try {
    ServiceCompat.startForeground(
      this,
      DetectionNotifications.ID_ONGOING,
      notifications.buildOngoing(),
      ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
    )
    true
  } catch (e: IllegalStateException) {
    Log.w(TAG, "Could not enter the foreground", e)
    false
  } catch (e: SecurityException) {
    Log.w(TAG, "Could not enter the foreground", e)
    false
  }

  private fun stopWhenDetectionEnds() {
    if (watching) return
    watching = true
    scope.launch {
      controller.state.first { it == DetectionState.Off }
      watching = false
      leaveForeground()
    }
  }

  private fun leaveForeground() {
    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  companion object {
    private const val TAG = "DetectionService"
    internal const val ACTION_START = "com.example.earview.action.START_DETECTION"
    internal const val ACTION_STOP = "com.example.earview.action.STOP_DETECTION"

    fun startIntent(context: Context): Intent = Intent(context, DetectionService::class.java).setAction(ACTION_START)

    fun stopIntent(context: Context): Intent = Intent(context, DetectionService::class.java).setAction(ACTION_STOP)
  }
}
