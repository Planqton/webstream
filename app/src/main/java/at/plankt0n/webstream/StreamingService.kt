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
import androidx.media3.common.PlaybackException
import androidx.media3.common.MediaMetadata
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import at.plankt0n.webstream.data.Stream
import at.plankt0n.webstream.helper.PreferencesHelper
import at.plankt0n.webstream.helper.ToastType
import at.plankt0n.webstream.helper.UIHelper

class StreamingService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    private var streams: List<Stream> = emptyList()
    private var currentIndex = 0
    private var forcedIndex: Int = -1

    private var lastLoggedRawTitle: String? = null

    companion object {
        const val CHANNEL_ID = "streaming_service_channel"
    }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Streaming Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for the Streaming Service"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Streaming Service")
            .setContentText("Initialisiere...")
            .setSmallIcon(R.drawable.ic_radio)
            .build()

        startForeground(1, notification)

        streams = PreferencesHelper.getStreams(this)
        if (streams.isEmpty()) {
            stopSelf()
            return
        }

        val lastUrl = PreferencesHelper.getLastPlayedStreamUrl(this)
        currentIndex = if (forcedIndex in streams.indices) forcedIndex else {
            streams.indexOfFirst { it.url == lastUrl }.takeIf { it >= 0 } ?: 0
        }

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(10_000)

        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(DefaultDataSource.Factory(this, httpDataSourceFactory))

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
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
    }

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            currentIndex = player.currentMediaItemIndex
            val currentStream = streams.getOrNull(currentIndex)
            PreferencesHelper.saveLastPlayedStreamUrl(this@StreamingService, currentStream?.url ?: "")
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady) {
                player.pause()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val stream = streams.getOrNull(currentIndex)
            val name = stream?.name ?: "Unbekannter Stream"

            val errorMessage = "Fehler beim Abspielen von $name: ${error.message}\n${error.cause?.javaClass?.simpleName}: ${error.cause?.message}"

            UIHelper.showToast(
                this@StreamingService,
                errorMessage,
                type = ToastType.ERROR
            )

            player.pause()
        }

        override fun onMediaMetadataChanged(metadata: MediaMetadata) {
            val rawTitle = metadata.title?.toString()?.trim().orEmpty()
            val streamName = streams.getOrNull(currentIndex)?.name ?: "?"

            if (rawTitle.isNotEmpty() && rawTitle != lastLoggedRawTitle) {
                lastLoggedRawTitle = rawTitle
                PreferencesHelper.logTrack(this@StreamingService, rawTitle, streamName)
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
    }
}
