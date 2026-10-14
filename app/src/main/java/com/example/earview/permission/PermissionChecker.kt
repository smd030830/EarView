package com.example.earview.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** Current state of the runtime permissions the app depends on. Abstracted so it can be faked in tests. */
interface PermissionChecker {
  /** Microphone access. Detection must not start without it (FR-11). */
  fun isMicrophoneGranted(): Boolean

  /** Whether the app may post notifications (FR-09, FR-10). */
  fun isNotificationGranted(): Boolean
}

class AndroidPermissionChecker(context: Context) : PermissionChecker {
  private val appContext = context.applicationContext

  override fun isMicrophoneGranted(): Boolean = ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

  // Also reflects POST_NOTIFICATIONS on Android 13+ and the per-app switch on older versions.
  override fun isNotificationGranted(): Boolean = NotificationManagerCompat.from(appContext).areNotificationsEnabled()
}
