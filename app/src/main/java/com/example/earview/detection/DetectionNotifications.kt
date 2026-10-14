package com.example.earview.detection

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.earview.MainActivity
import com.example.earview.R
import kotlinx.coroutines.flow.Flow

/** Shows and clears the "detection stopped unexpectedly" alert (FR-10). */
interface StoppedNotifier {
  fun showStopped()

  fun clearStopped()
}

/** Shows [StoppedNotifier.showStopped] whenever detection stops without the user asking for it. */
suspend fun Flow<StopReason>.alertOnUnexpectedStop(notifier: StoppedNotifier) {
  collect { reason -> if (reason == StopReason.Unexpected) notifier.showStopped() }
}

/** The ongoing "detecting" notification (FR-09) and the "stopped" alert (FR-10). */
class DetectionNotifications(context: Context) : StoppedNotifier {
  private val context = context.applicationContext
  private val manager = this.context.getSystemService(NotificationManager::class.java)

  init {
    manager.createNotificationChannel(
      NotificationChannel(
        CHANNEL_ONGOING,
        this.context.getString(R.string.notification_channel_ongoing),
        NotificationManager.IMPORTANCE_LOW,
      ),
    )
    manager.createNotificationChannel(
      NotificationChannel(
        CHANNEL_STOPPED,
        this.context.getString(R.string.notification_channel_stopped),
        NotificationManager.IMPORTANCE_HIGH,
      ).apply { enableVibration(true) },
    )
  }

  /** Pinned notification shown while detecting. Tapping it opens the app; the action stops detection. */
  fun buildOngoing() = NotificationCompat.Builder(context, CHANNEL_ONGOING)
    .setSmallIcon(R.drawable.ic_notification)
    .setContentTitle(context.getString(R.string.notification_ongoing_title))
    .setContentText(context.getString(R.string.notification_ongoing_text))
    .setContentIntent(openAppIntent())
    .addAction(0, context.getString(R.string.notification_action_stop), stopIntent())
    .setOngoing(true)
    .setOnlyAlertOnce(true)
    .setCategory(NotificationCompat.CATEGORY_SERVICE)
    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
    .build()

  override fun showStopped() {
    val notification =
      NotificationCompat.Builder(context, CHANNEL_STOPPED)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(context.getString(R.string.notification_stopped_title))
        .setContentText(context.getString(R.string.notification_stopped_text))
        .setContentIntent(openAppIntent())
        .setAutoCancel(true)
        .setCategory(NotificationCompat.CATEGORY_ERROR)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .build()
    manager.notify(ID_STOPPED, notification)
  }

  override fun clearStopped() {
    manager.cancel(ID_STOPPED)
  }

  private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
    context,
    REQUEST_OPEN_APP,
    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
  )

  private fun stopIntent(): PendingIntent = PendingIntent.getService(
    context,
    REQUEST_STOP,
    DetectionService.stopIntent(context),
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
  )

  companion object {
    const val ID_ONGOING = 1
    const val ID_STOPPED = 2
    private const val CHANNEL_ONGOING = "detection_ongoing"
    private const val CHANNEL_STOPPED = "detection_stopped"
    private const val REQUEST_OPEN_APP = 0
    private const val REQUEST_STOP = 1
  }
}
