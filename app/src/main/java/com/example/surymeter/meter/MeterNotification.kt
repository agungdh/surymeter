package com.example.surymeter.meter

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.surymeter.MainActivity
import com.example.surymeter.R
import com.example.surymeter.data.Speeds
import com.example.surymeter.data.Totals
import com.example.surymeter.ui.Format

object MeterNotification {

    const val CHANNEL_ID = "meter_channel"
    const val NOTIFICATION_ID = 1
    const val ACTION_STOP = "com.example.surymeter.action.STOP"

    fun createChannel(context: Context) {
        val channel = android.app.NotificationChannel(
            CHANNEL_ID,
            "Internet usage",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows current bandwidth and usage"
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun build(context: Context, speeds: Speeds, today: Totals, totals: Totals): Notification {
        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            context,
            1,
            Intent(context, MeterService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val content = android.widget.RemoteViews(
            context.packageName,
            R.layout.notification_content
        )
        content.setTextViewText(R.id.title, context.getString(R.string.notif_title))
        content.setTextViewText(
            R.id.total_today,
            context.getString(
                R.string.notif_total_today,
                Format.bytes(today.total)
            )
        )
        content.setTextViewText(R.id.wifi_label, context.getString(R.string.net_wifi))
        content.setTextViewText(R.id.wifi_down, Format.speed(speeds.wifiRx))
        content.setTextViewText(R.id.wifi_up, Format.speed(speeds.wifiTx))
        content.setTextViewText(R.id.mobile_label, context.getString(R.string.net_mobile))
        content.setTextViewText(R.id.mobile_down, Format.speed(speeds.mobileRx))
        content.setTextViewText(R.id.mobile_up, Format.speed(speeds.mobileTx))

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_traffic)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(Format.bytes(totals.total))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setContentIntent(openIntent)
            .setCustomContentView(content)
            .addAction(
                NotificationCompat.Action.Builder(
                    0,
                    context.getString(R.string.notif_stop),
                    stopIntent
                ).build()
            )
            .build()
    }
}
