package at.plankt0n.webstream

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
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
import at.plankt0n.webstream.helper.PreferencesHelper
import at.plankt0n.webstream.helper.ToastType
import at.plankt0n.webstream.helper.UIHelper

class StreamingService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    private var streams: List<Stream> = emptyList()
    private var currentIndex = 0


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


    @androidx.media3.common.util.UnstableApi //Disable warning for unstab
    override fun onCreate() {
        super.onCreate()
       //Foreground Service
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Streaming Service")
            .setContentText("Initialisiere...")
            .setSmallIcon(R.drawable.ic_radio)
            .build()
        startForeground(1, notification)

        // Create Notification Channel for Android O and above
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
        currentIndex = PreferencesHelper.getLastPlayedStreamIndex(this)

        // HTTP data source with 10s timeouts and support for cross-protocol redirects
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(10_000)

        // Media source factory using the custom HTTP data source
        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(DefaultDataSource.Factory(this, httpDataSourceFactory))

        // Create ExoPlayer instance
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ALL
                addListener(playerListener)
            }

        //Setup the Playlist
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
        //Load Streams from Preferences
        streams = PreferencesHelper.getStreams(this)
        if (streams.isEmpty()) {
            stopSelf()
            return
        }

        //Create Media Items for the Playlist and set exta fpr ICONURL
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

        //Set the Playlist to the Player and prepare it for Playback
        player.setMediaItems(mediaItems, currentIndex, 0L)
        player.prepare()
    }

    private fun refreshPlaylist() {
        // Prüfen, ob aktuell gespielt wird
        val wasPlaying = player.isPlaying
        if (wasPlaying) {
            player.pause()
        }

        // ReLoad Streams from Preferences
        streams = PreferencesHelper.getStreams(this)
        if (streams.isEmpty()) {
            stopSelf()
            return
        }

        // Create Media Items for the Playlist und set extra für ICONURL
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

        // Setze die Playlist und bereite sie vor
        player.setMediaItems(mediaItems, currentIndex, 0L)
        player.prepare()

        // Wiedergabe fortsetzen, falls vorher aktiv
        if (wasPlaying) {
            player.play()
        }
    }


    private val playerListener = object : Player.Listener {
        //listener for saving the last played stream Index
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val index = player.currentMediaItemIndex
            if (index >= 0) {
                currentIndex = index
                PreferencesHelper.saveLastPlayedStreamIndex(this@StreamingService, index)
                Log.d("StreamingService", "🔁 MediaItem gewechselt: Index = $index")
            }
        }
        //listener for changes in the playlist
        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
                Log.d("StreamingService", "📻 Playlist wurde aktualisiert.")
            }
        }
        //Error handling
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


        //Tracklogging Only when Setting is True
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

        //Player State Change
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
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
    }
}
