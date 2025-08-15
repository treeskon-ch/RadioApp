package com.aom.radioapp.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class RadioActionService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var streamUrl: String? = null
    private val channelId = "RadioServiceChannel"
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob()) //aom22/5/25
    private var isPreparing = false

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val newUrl = intent?.getStringExtra("STREAM_URL")

        if (newUrl.isNullOrEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (streamUrl == newUrl && mediaPlayer?.isPlaying == true) {
            return START_STICKY
        }

        streamUrl = newUrl

        createNotificationChannel()
        startForeground(1, buildNotification("Streaming..."))

        playStreamSafely(newUrl)

        return START_STICKY
    }

    //ออม แก้ไขโค้ด 22/5/68 เปลี่ยนการใข้ Thread { MediaPlayer setup}.start() เป็น  serviceScope.launch แทน
    private fun playStreamSafely(url: String) {
        if (isPreparing) {
            return
        }
        isPreparing = true

        serviceScope.launch {
            try {
                mediaPlayer?.apply {
                    stop()
                    reset()
                    release()
                }

                mediaPlayer = MediaPlayer().apply {
                    setDataSource(url)
                    setOnPreparedListener {
                        it.start()
                        isPreparing = false
                    }
                    setOnErrorListener { _, _, _ ->
                        isPreparing = false
                        stopSelf()
                        true
                    }
                    prepareAsync()
                }
            } catch (e: Exception) {
                isPreparing = false
                stopSelf()
            }
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Radio Streaming")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Radio Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        serviceScope.cancel()//aom22/5/68
    }

    override fun onBind(intent: Intent?): IBinder? = null
}


