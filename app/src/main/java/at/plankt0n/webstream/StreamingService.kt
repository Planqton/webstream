package at.plankt0n.webstream

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Timeline
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import at.plankt0n.webstream.data.Stream
import at.plankt0n.webstream.helper.IcyStreamReader
import at.plankt0n.webstream.helper.PreferencesHelper
import at.plankt0n.webstream.helper.ToastType
import at.plankt0n.webstream.helper.UIHelper

class StreamingService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    private var streams: List<Stream> = emptyList()
    private var currentIndex = 0
    private var icyStreamReader: IcyStreamReader? = null

    private var lastLoggedRawTitle: String? = null

    companion object {
        const val CHANNEL_ID = "streaming_service_channel"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            if (action == "at.plankt0n.webstream.action.REFRESH_PLAYLIST") {
                refreshPlaylist()
                Log.d("StreamingService", "🔁 refreshPlaylist() über Save-Button ausgeführt!")
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @androidx.media3.common.util.UnstableApi
    override fun onCreate() {
        super.onCreate()

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.streaming_service_notification_title))
            .setContentText(getString(R.string.streaming_service_notification_text))
            .setSmallIcon(R.drawable.ic_radio)
            .build()
        startForeground(1, notification)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.streaming_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.streaming_service_channel_description)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        currentIndex = PreferencesHelper.getLastPlayedStreamIndex(this)

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
        streams = PreferencesHelper.getStreams(this)
        if (streams.isEmpty()) {
            stopSelf()
            return
        }

        val mediaItems = streams.map {
            val extras = Bundle().apply {
                putString("EXTRA_ICON_URL", it.iconUrl)
            }

            val metadata = MediaMetadata.Builder()
                .setArtist(it.name)
                .setExtras(extras)
                .build()

            MediaItem.Builder()
                .setUri(it.url)
                .setMediaMetadata(metadata)
                .build()
        }

        player.setMediaItems(mediaItems, currentIndex, 0L)
        player.prepare()

    }

    private fun refreshPlaylist() {
        val wasPlaying = player.isPlaying
        if (wasPlaying) {
            player.pause()
        }

        streams = PreferencesHelper.getStreams(this)
        if (streams.isEmpty()) {
            stopSelf()
            return
        }

        val mediaItems = streams.map {
            val extras = Bundle().apply {
                putString("EXTRA_ICON_URL", it.iconUrl)
            }

            val metadata = MediaMetadata.Builder()
                .setArtist(it.name)
                .setExtras(extras)
                .build()

            MediaItem.Builder()
                .setUri(it.url)
                .setMediaMetadata(metadata)
                .build()
        }



        if (wasPlaying) {
            player.play()
        }

        player.setMediaItems(mediaItems, currentIndex, 0L)
        player.prepare()

        Log.d("StreamingService", getString(R.string.streaming_service_refresh_playlist_log))

    }



    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val index = player.currentMediaItemIndex
            if (index >= 0) {
                currentIndex = index
                PreferencesHelper.saveLastPlayedStreamIndex(this@StreamingService, index)
                Log.d("StreamingService", getString(R.string.streaming_service_media_item_changed_log, index))


            }


        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
                Log.d("StreamingService", getString(R.string.streaming_service_timeline_changed_log))

            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val stream = streams.getOrNull(currentIndex)
            val name = stream?.name ?: getString(R.string.streaming_service_unknown_stream)
            val errorMessage = getString(
                R.string.streaming_service_error_message,
                name,
                error.message,
                error.cause?.javaClass?.simpleName ?: "Unknown",
                error.cause?.message ?: "Unknown"
            )
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
                if (PreferencesHelper.isAutoLogEnabled(this@StreamingService)) {
                    PreferencesHelper.logTrack(this@StreamingService, rawTitle, streamName)
                }
            }
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
        icyStreamReader?.stop()
        icyStreamReader = null

        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
    }
}
