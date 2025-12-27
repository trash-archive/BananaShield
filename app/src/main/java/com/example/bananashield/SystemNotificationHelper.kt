package com.example.bananashield

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object SystemNotificationHelper {

    private const val CHANNEL_ID_SCAN = "scan_results_channel"
    private const val CHANNEL_NAME_SCAN = "Scan Results"
    private const val CHANNEL_DESC_SCAN = "Notifications when a scan is finished"

    private var nextNotificationId = 1000

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_SCAN,
                CHANNEL_NAME_SCAN,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC_SCAN
                enableVibration(true)
                enableLights(true)
            }

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Shows a system notification in the status bar that opens the results page when tapped
     */
    fun showScanCompletedNotification(
        context: Context,
        diseaseName: String,
        confidence: Float,
        scanId: String
    ) {
        ensureChannel(context)

        // Intent that will open MainActivity and navigate to scan result
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "scan_result")
            putExtra("scanId", scanId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            scanId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (diseaseName.contains("Healthy", ignoreCase = true)) {
            "Scan Complete – Healthy Plant"
        } else {
            "Disease Detected – $diseaseName"
        }

        val message = "Confidence: ${(confidence * 100).toInt()}%. Tap to view full results."

        val notificationId = nextNotificationId++

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SCAN)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your app icon
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
