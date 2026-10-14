package com.kelas.backgroundservices

import android.R as AndroidR
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlin.math.abs

private lateinit var sensorManager: SensorManager
private var accelerometer: Sensor? = null

private const val NOTIFICATION_CHANNERL_ID_SERVICE_RUNNING = "tracker_service_running"
private const val NOTIFICATION_CHANNEL_ID_JUMP_DETECTED = "tracker_jump_detected"
private const val NOTIFICATION_ID_SERVICE_RUNNING = 1
private const val NOTIFICATION_ID_JUMP_DETECTED = 2
var jumps = 0

class TrackerService : Service(), SensorEventListener {

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNERL_ID_SERVICE_RUNNING,
                "Service Running",
                NotificationManager.IMPORTANCE_LOW,
            )
            val jumpChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID_JUMP_DETECTED,
                "Jump Detected",
                NotificationManager.IMPORTANCE_HIGH,
            )

            manager?.createNotificationChannel(serviceChannel)
            manager?.createNotificationChannel(jumpChannel)
        }
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        createNotificationChannels()
        Log.d("TrackerService", "Service onCreate")
    }

    private fun startJumpDetection() {
        accelerometer?.also {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL,
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("TrackerService", "Service onStartCommand")

        startForeground(
            NOTIFICATION_ID_SERVICE_RUNNING,
            buildServiceRunningNotification(),
        )

        startJumpDetection()

        return START_STICKY
    }

    private fun buildServiceRunningNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNERL_ID_SERVICE_RUNNING)
            .setContentTitle("Pelacak Aktivitas Latar")
            .setContentText("Aplikasi Tracker Services sedang berjalan...")
            .setSmallIcon(AndroidR.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val y = event.values[1]
            val jumpThreshold = 15.0

            if (abs(y) > jumpThreshold) {
                Log.d("TrackerService", "LOMPATAN Terdeteksi! Y: $y")

                sensorManager.unregisterListener(this)

                showJumpNotification()
                jumps+=1
                Log.d("TrackerService", "Jumps: $jumps")
                if (jumps >= 3){
                    stopSelf()
                }
            }
        }
    }

    private fun showJumpNotification() {
        val mainActivityIntent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_SERVICE_STOPPED_JUMP
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingMainActivityIntent = PendingIntent.getActivity(
            this, 1, mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val jumpNotification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_JUMP_DETECTED)
            .setContentTitle("Lompatan Terdeteksi")
            .setContentText("Service Berhenti. Ketuk untuk membuka aplikasi")
            .setSmallIcon(AndroidR.drawable.ic_dialog_alert)
            .setContentIntent(pendingMainActivityIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(NOTIFICATION_ID_JUMP_DETECTED, jumpNotification)
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        Log.d("TrackerService", "Service onDestroy")
    }
}
