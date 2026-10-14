package com.example.earview.permission

class FakePermissionChecker(
  var microphoneGranted: Boolean = true,
  var notificationGranted: Boolean = true,
) : PermissionChecker {
  override fun isMicrophoneGranted(): Boolean = microphoneGranted

  override fun isNotificationGranted(): Boolean = notificationGranted
}
