package com.aom.radioapp.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.aom.radioapp.R

class RadioService : Service() {

    private lateinit var player: ExoPlayer
    private val CHANNEL_ID = "RadioServiceChannel"
    private val NOTIFICATION_ID = 1
    override fun onBind(intent: Intent): IBinder? {
        return null
        // ใช้สำหรับ Bound Service (หากไม่ใช้ return null)
    }

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e("ExoPlayer", "Playback error: ${error.message}")
                Toast.makeText(this@RadioService, "เล่นสตรีมไม่ได้", Toast.LENGTH_SHORT).show()
                player.stop()
            }
        })
        // ตั้งค่าให้ Service ทำงานตลอดเวลา
        startForeground(NOTIFICATION_ID, createNotification("กำลังเล่น..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "STOP" -> player.stop()
            "PLAY" -> {
                val stationUrl = intent.getStringExtra("STATION_URL")
                val stationName = intent.getStringExtra("STATION_NAME")
                val stationLogo = intent.getStringExtra("STATION_LOGO")
                val stationFavorite = intent.getBooleanExtra("STATION_FAVORITE",false)
                val stationIsplay = intent.getBooleanExtra("STATION_ISPLAY",false)
                if (stationUrl != null && stationName != null) {
                    playRadio(stationUrl, stationName)
                    if (stationLogo != null) {
                        broadcastUpdate(stationName, stationLogo, stationFavorite, stationIsplay)
                    }
                }
                //player.play()
            }
        }
        return START_STICKY
        // เรียกใช้เมื่อมีการ start Service ผ่าน startService(Intent)
    }

    private fun broadcastUpdate(name: String, logo: String, isFav: Boolean, isPlaying: Boolean) {
        val intent = Intent("RADIO_UPDATE")
        intent.putExtra("station_name", name)
        intent.putExtra("station_logo", logo)
        intent.putExtra("station_fav", isFav)
        intent.putExtra("is_playing", isPlaying)
        sendBroadcast(intent)
    }



    override fun onDestroy() {
        super.onDestroy()
        // เรียกใช้เมื่อ Service ถูกทำลาย
    }
    @SuppressLint("UnsafeOptInUsageError")
    private fun playRadio(url: String, stationName: String) {
        try {
            val mediaItem = MediaItem.fromUri(url)
            if (url.endsWith(".m3u8")) {
                HlsMediaSource.Factory(DefaultHttpDataSource.Factory()).createMediaSource(mediaItem)
            } else {
                ProgressiveMediaSource.Factory(DefaultHttpDataSource.Factory()).createMediaSource(mediaItem)
            }
            player.stop()
            player.clearMediaItems()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()

            // อัปเดต Notification
            updateNotification(stationName)
        }catch (e:Exception){
            Log.e("RadioService", "Error playing stream", e)
            Toast.makeText(this, "ไม่สามารถเล่นสถานีนี้ได้", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createNotification(stationName: String): Notification {
        createNotificationChannel()

        val stopIntent = Intent(this, RadioService::class.java).apply { action = "STOP" }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("กำลังเล่น: $stationName")
            .setSmallIcon(R.drawable.icon_radio)
            .setContentIntent(stopPendingIntent)
            .addAction(R.drawable.ic_stop, "หยุด", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("NotificationPermission")
    private fun updateNotification(stationName: String) {
        val notification = createNotification(stationName)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Radio Service", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

}