package com.example.earview.permission

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** Opens the system app-info screen, where the microphone permission can be changed (FR-11). */
fun Context.openAppSettings() {
  startActivity(
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
  )
}

/** Opens the system notification settings for this app, falling back to the app-info screen. */
fun Context.openNotificationSettings() {
  val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
  try {
    startActivity(intent)
  } catch (_: ActivityNotFoundException) {
    openAppSettings()
  }
}
