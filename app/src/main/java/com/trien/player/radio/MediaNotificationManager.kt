package com.trien.player.radio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
//import androidx.media.app.NotificationCompat;

import com.trien.player.MainActivity
import com.trien.player.R

class MediaNotificationManager(private val service: RadioService) {
    private val PRIMARY_CHANNEL = "PRIMARY_CHANNEL_ID"
    private val PRIMARY_CHANNEL_NAME = "PRIMARY"

    private val notificationContent: String
    private val notificationTitle: String

    private val resources: Resources

    private val notificationManager: NotificationManagerCompat

    init {
        this.resources = service.resources

        notificationContent = resources.getString(R.string.radio_name)
        notificationTitle = resources.getString(R.string.notification_title)

        notificationManager = NotificationManagerCompat.from(service)
    }

    fun startNotification(playbackStatus: String) {

        val largeIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)

        var icon = android.R.drawable.ic_media_pause
        val playbackAction = Intent(service, RadioService::class.java)
        playbackAction.action = RadioService.ACTION_PAUSE
        var action = PendingIntent.getService(service, 1, playbackAction, PendingIntent.FLAG_UPDATE_CURRENT)

        if (playbackStatus == PlaybackStatus.PAUSED) {

            icon = android.R.drawable.ic_media_play
            playbackAction.action = RadioService.ACTION_PLAY
            action = PendingIntent.getService(service, 2, playbackAction, PendingIntent.FLAG_UPDATE_CURRENT)

        }

        val stopIntent = Intent(service, RadioService::class.java)
        stopIntent.action = RadioService.ACTION_STOP
        val stopAction = PendingIntent.getService(service, 3, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val intent = Intent(service, MainActivity::class.java)
        intent.action = Intent.ACTION_MAIN
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val pendingIntent = PendingIntent.getActivity(service, 0, intent, PendingIntent.FLAG_NO_CREATE)

        notificationManager.cancel(NOTIFICATION_ID)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(PRIMARY_CHANNEL, PRIMARY_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
            channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            manager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(service, PRIMARY_CHANNEL)
                .setAutoCancel(false)
                .setShowWhen(false)
                // .setWhen(System.currentTimeMillis())
                .setContentTitle(notificationTitle)
                .setContentText(notificationContent)
                .setLargeIcon(largeIcon)
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                .addAction(icon, "pause", action)
                .addAction(R.drawable.ic_stop_white, "stop", stopAction)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(service.mediaSession!!.sessionToken)
                        .setShowActionsInCompactView(0, 1)
                        //In Android 5.0 (API level 21) and later you can swipe away a notification
                        // to stop the player once the service is no longer running in the foreground.
                        // You can't do this in earlier versions. To allow users to remove the
                        // notification and stop playback before Android 5.0 (API level 21), you can
                        // add a cancel button in the upper-right corner of the notification by calling
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(stopAction))

        service.startForeground(NOTIFICATION_ID, builder.build())

    }

    fun cancelNotification() {

        service.stopForeground(true)

    }

    companion object {

        val NOTIFICATION_ID = 555
    }

}
