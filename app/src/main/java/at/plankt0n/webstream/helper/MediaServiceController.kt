package at.plankt0n.webstream.helper

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import at.plankt0n.webstream.StreamingService
import at.plankt0n.webstream.data.Stream

class MediaServiceController(private val context: Context) {

    private var mediaController: MediaController? = null
    private var listener: Player.Listener? = null
    private val handler = Handler(Looper.getMainLooper())

    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null

    fun initializeAndConnect(
        onConnected: (MediaController) -> Unit,
        onPlaybackChanged: (Boolean) -> Unit,
        onStreamIndexChanged: (Int) -> Unit,
        onMetadataChanged: (String) -> Unit,
        onTimelineChanged: (Int) -> Unit
    ) {
        val serviceIntent = Intent(context, StreamingService::class.java)
        context.startForegroundService(serviceIntent)

        val sessionToken = SessionToken(context, ComponentName(context, StreamingService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()

        future.addListener({
            try {
                val controller = future.get()
                mediaController = controller

                logRotaryAndMediaSessionStreams(controller)

                listener = object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        onPlaybackChanged(isPlaying)
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        handler.postDelayed({
                            val index = controller.currentMediaItemIndex
                            if (index >= 0) {
                                onStreamIndexChanged(index)
                            }
                        }, 100)
                    }

                    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                        Log.d("MediaServiceController", "🔁 Timeline geändert! Grund: $reason")
                        onTimelineChanged(reason)
                    }


                    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                        val title = mediaMetadata.title?.toString()
                        title?.let {
                            onMetadataChanged(it)
                        }
                    }
                }

                controller.addListener(listener!!)
                onConnected(controller)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, context.mainExecutor)
    }


    //shows a log with all items in the media session and the rotary streams
    private fun logRotaryAndMediaSessionStreams(controller: MediaController) {
        val rotaryStreams = PreferencesHelper.getStreams(context)

        Log.d("MediaServiceController", "====== ROTARY STREAMS ======")
        rotaryStreams.forEachIndexed { index, stream ->
            Log.d("MediaServiceController", "[$index] ${stream.name}")
        }

        Log.d("MediaServiceController", "====== MEDIASESSION ITEMS ======")
        for (i in 0 until controller.mediaItemCount) {
            val mediaItem = controller.getMediaItemAt(i)
            val uri = mediaItem.localConfiguration?.uri?.toString() ?: "(keine URI)"
            Log.d("MediaServiceController", "[$i] $uri")
        }
    }

    fun disconnect() {
        listener?.let { mediaController?.removeListener(it) }
        mediaController?.release()
        abandonAudioFocus()
        mediaController = null
        listener = null
    }

    fun isPlaying(): Boolean = mediaController?.isPlaying == true

    fun playAtIndex(index: Int) {
        val controller = mediaController ?: return
        if (index in 0 until controller.mediaItemCount) {
            controller.seekToDefaultPosition(index)
            if (requestAudioFocus()) {
                controller.play()
            } else {
                Log.w("MediaServiceController", "❌ AudioFocus konnte nicht erhalten werden.")
            }
        }
    }

    fun getCurrentPlaylist(): List<Stream> {
        val controller = mediaController ?: return emptyList()

        val itemCount = controller.mediaItemCount
        val streams = mutableListOf<Stream>()

        for (i in 0 until itemCount) {
            val mediaItem = controller.getMediaItemAt(i)
            val metadata: MediaMetadata = mediaItem.mediaMetadata
            val extras = metadata.extras

            val name = metadata.title?.toString() ?: "Unnamed"
            val url = mediaItem.localConfiguration?.uri?.toString() ?: ""
            val iconUrl = extras?.getString("EXTRA_ICON_URL") ?: ""

            streams.add(Stream(name = name, url = url, iconUrl = iconUrl))
        }

        return streams
    }




    fun seekToIndex(index: Int) {
        val controller = mediaController ?: return
        if (index in 0 until controller.mediaItemCount) {
            controller.seekToDefaultPosition(index)
        }
    }

    fun pause() {
        mediaController?.pause()
        abandonAudioFocus()
    }

    fun skipToNext() {
        mediaController?.seekToNextMediaItem()
        Log.d("MediaServiceController", ">>>>>")
    }

    fun skipToPrevious() {
        mediaController?.seekToPreviousMediaItem()
        Log.d("MediaServiceController", "<<<<<<")
    }


    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
            abandonAudioFocus()
        } else {
            if (requestAudioFocus()) {
                controller.play()
            } else {
                Log.w("MediaServiceController", "❌ AudioFocus konnte nicht erhalten werden.")
            }
        }
    }

    fun getCurrentStreamIndex(): Int {
        return mediaController?.currentMediaItemIndex ?: 0
    }

    private fun requestAudioFocus(): Boolean {
        if (!PreferencesHelper.isAudiofocusEnabled(context)) return true

        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        Log.d("MediaServiceController", "🔇 AudioFocus verloren, pausieren.")
                        pause()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        // Optional: Lautstärke verringern
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        // Optional: Lautstärke zurücksetzen
                    }
                }
            }
            .build()

        val result = audioManager?.requestAudioFocus(focusRequest!!)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        focusRequest?.let {
            audioManager?.abandonAudioFocusRequest(it)
        }
    }
}
