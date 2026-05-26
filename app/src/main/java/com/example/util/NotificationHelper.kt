package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {
    private const val CHANNEL_ID = "weather_alerts_channel"
    private const val CHANNEL_NAME = "Severe Weather Radar Warnings"
    private const val CHANNEL_DESC = "Push notification alerts for severe meteorological radar warnings in favorite locations."

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showWeatherAlertNotification(
        context: Context,
        locationName: String,
        severity: String,
        message: String,
        notificationId: Int = (1000..9999).random()
    ) {
        // Create an Intent that opens MainActivity when clicked
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning) // Using dynamic system warnings drawable
            .setContentTitle("🚨 STORM WARNING: $locationName")
            .setContentText("$severity - $message")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$severity\n\n$message\n\nActive core monitoring enabled via StormOps Telemetry.")
            )

        try {
            val manager = NotificationManagerCompat.from(context)
            // Note: Standard system permission check is handled gracefully on Android 13+
            manager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Log or handle missing permissions gracefully
        }
    }
}
