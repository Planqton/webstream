package at.plankt0n.webstream

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import at.plankt0n.webstream.data.Stream
import at.plankt0n.webstream.helper.PreferencesHelper
import at.plankt0n.webstream.helper.BroadcastActions
import androidx.media3.common.MediaMetadata
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.DefaultDataSource

class RadioService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    private var streams: List<Stream> = emptyList()
    private var currentIndex = 0
    private var isInitialized = false
    private var autoplayRequested: Boolean = false

    companion object {
        const val CHANNEL_ID = "radio_service_channel"
    }

    override fun onCreate() {
        super.onCreate()
        // Keine Logik mehr hier – nur Basisinitialisierung, falls nötig
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isInitialized) {
            initService(intent)
            isInitialized = true
        } else {
            autoplayRequested = intent?.getBooleanExtra("autoplay", false) == true
            if (autoplayRequested && !player.isPlaying) {
                player.play()
            }
        }
        return START_STICKY
    }

    private fun initService(intent: Intent?) {
        autoplayRequested = intent?.getBooleanExtra("autoplay", false) == true
        sendBroadcast(Intent(BroadcastActions.ACTION_SERVICE_STARTED))

        // Notification-Channel erstellen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Radio Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for the Radio Service"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        // Notification anzeigen
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Radio bereit")
            .setContentText("Bereit zum Abspielen")
            .setSmallIcon(R.drawable.ic_radio)
            .build()

        startForeground(1, notification)

        // Player vorbereiten
        streams = PreferencesHelper.getStreams(this)
        if (streams.isEmpty()) {
            stopSelf()
            return
        }

        val lastUrl = PreferencesHelper.getLastPlayedStreamUrl(this)
        currentIndex = streams.indexOfFirst { it.url == lastUrl }.takeIf { it >= 0 } ?: 0

        // ✅ Timeout-fähiger HTTP-Datenquellen-Factory
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(10_000)

        val mediaSourceFactory = DefaultMediaSourceFactory(
            DefaultDataSource.Factory(this, httpDataSourceFactory)
        )

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory) // ⬅️ Verwende custom factory
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ALL
                addListener(playerListener)
            }

        setupPlaylist()

        val sessionIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionIntent)
            .build()
    }

    private fun setupPlaylist() {
        if (streams.isEmpty()) return

        val mediaItems = streams.map {
            val metadata = MediaMetadata.Builder()
                .setArtist(it.name)
                .build()

            MediaItem.Builder()
                .setUri(it.url)
                .setMediaMetadata(metadata)
                .build()
        }

        player.setMediaItems(mediaItems, currentIndex, 0L)
        player.prepare()

        if (autoplayRequested) {
            player.play()
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            currentIndex = player.currentMediaItemIndex
            val currentStream = streams.getOrNull(currentIndex)
            PreferencesHelper.saveLastPlayedStreamUrl(this@RadioService, currentStream?.url ?: "")
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady) {
                player.pause()
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession.release()
        player.release()
        sendBroadcast(Intent(BroadcastActions.ACTION_SERVICE_STOPPED))
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
    }
}
