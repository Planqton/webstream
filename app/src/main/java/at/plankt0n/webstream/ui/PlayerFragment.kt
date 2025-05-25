package at.plankt0n.webstream.ui

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.media3.common.Player
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import at.plankt0n.webstream.R
import at.plankt0n.webstream.adapter.StreamCoverCardAdapter
import at.plankt0n.webstream.databinding.FragmentPlayerBinding
import at.plankt0n.webstream.helper.*
import at.plankt0n.webstream.Keys
import org.json.JSONObject
import java.net.URL
import kotlin.concurrent.thread

class PlayerFragment : Fragment() {

    private lateinit var binding: FragmentPlayerBinding
    private lateinit var snapHelper: LinearSnapHelper
    private var mediaServiceController: MediaServiceController? = null
    private var isProgrammaticScroll = false
    private val handler = Handler(Looper.getMainLooper()) // Für Delay beim Minimieren


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        binding.textTitle.text = getString(R.string.now_playing_prefix) + getString(R.string.placeholder_dash)
        binding.textArtist.text = ""

        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        binding.recyclerViewCoverFlow.layoutManager = layoutManager
        binding.recyclerViewCoverFlow.setHasFixedSize(true)
        binding.recyclerViewCoverFlow.clipToPadding = false
        binding.recyclerViewCoverFlow.clipChildren = false
        binding.recyclerViewCoverFlow.addItemDecoration(ScaleCenterItemDecoration(minScale = 0.8f))
        binding.recyclerViewCoverFlow.addItemDecoration(
            CoverFlow3DEffectDecoration(minScale = 0.85f, maxRotation = 25f)
        )

        snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(binding.recyclerViewCoverFlow)

        // OnScrollListener: Reset Override-Cover wenn gescrollt wird!
        binding.recyclerViewCoverFlow.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val adapter = binding.recyclerViewCoverFlow.adapter as? StreamCoverCardAdapter ?: return

                    // 🔥 Reset: overrideCoverUrl & Position zurücksetzen
                    adapter.overrideCoverUrl = null
                    adapter.overrideCoverPosition = null
                    adapter.notifyDataSetChanged()

                    // Weiter: Aktuelles Item dem Player mitteilen
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val snappedView = snapHelper.findSnapView(layoutManager)
                    val position = snappedView?.let { layoutManager.getPosition(it) } ?: return

                    if (!isProgrammaticScroll) {
                        mediaServiceController?.seekToIndex(position)
                    } else {
                        isProgrammaticScroll = false
                    }
                }
            }
        })

        // MediaServiceController initialisieren und verbinden
        mediaServiceController = MediaServiceController(context)
        mediaServiceController?.initializeAndConnect(
            onConnected = { controller ->
                val displayStreams = mediaServiceController?.getCurrentPlaylist().orEmpty()
                val adapter = StreamCoverCardAdapter(context, displayStreams)
                binding.recyclerViewCoverFlow.adapter = adapter

                // Padding für zentriertes Item
                binding.recyclerViewCoverFlow.post {
                    val viewHolder = binding.recyclerViewCoverFlow.findViewHolderForAdapterPosition(0)
                    val itemWidth = viewHolder?.itemView?.width
                        ?: binding.recyclerViewCoverFlow.getChildAt(0)?.width
                        ?: 0

                    if (itemWidth > 0) {
                        val recyclerWidth = binding.recyclerViewCoverFlow.width
                        val sidePadding = (recyclerWidth - itemWidth) / 2
                        binding.recyclerViewCoverFlow.setPadding(sidePadding, 0, sidePadding, 0)
                        binding.recyclerViewCoverFlow.clipToPadding = false
                    }
                }

                // Aktuellen Index zentrieren
                val currentIndex = controller.currentMediaItemIndex
                if (currentIndex >= 0) {
                    isProgrammaticScroll = true
                    binding.recyclerViewCoverFlow.post {
                        binding.recyclerViewCoverFlow.smoothScrollToPosition(currentIndex)
                    }
                }


                if (PreferencesHelper.isAutoplayAndCloseEnabled(context)) {
                    mediaServiceController?.playAtIndex(currentIndex)
                    val delayMillis = PreferencesHelper.getAutoplayandCloseDelay(context) * 1000L
                    handler.postDelayed({
                        if (PreferencesHelper.isAutoplayAndCloseEnabled(context)) {
                            requireActivity().moveTaskToBack(true)
                        }
                    }, delayMillis)
                } else if (PreferencesHelper.isAutoPlayEnabled(context)) {
                    mediaServiceController?.playAtIndex(currentIndex)
                }


            },
            onPlaybackChanged = { isPlaying ->
                binding.buttonPlayPause.setImageResource(
                    if (isPlaying) R.drawable.ic_button_pause else R.drawable.ic_button_play
                )
            },
            onStreamIndexChanged = { index ->
                isProgrammaticScroll = true
                binding.recyclerViewCoverFlow.post {
                    binding.recyclerViewCoverFlow.smoothScrollToPosition(index)
                }
            },
            onTimelineChanged = { reason ->
                if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
                    val updatedStreams = mediaServiceController?.getCurrentPlaylist().orEmpty()
                    val currentIndex = mediaServiceController?.getCurrentStreamIndex() ?: 0

                    val newAdapter = StreamCoverCardAdapter(requireContext(), updatedStreams)
                    binding.recyclerViewCoverFlow.adapter = newAdapter

                    binding.recyclerViewCoverFlow.post {
                        val viewHolder = binding.recyclerViewCoverFlow.findViewHolderForAdapterPosition(0)
                            ?: binding.recyclerViewCoverFlow.getChildAt(0)?.let {
                                binding.recyclerViewCoverFlow.getChildViewHolder(it)
                            }

                        val itemWidth = viewHolder?.itemView?.width ?: 0
                        if (itemWidth > 0) {
                            val recyclerWidth = binding.recyclerViewCoverFlow.width
                            val sidePadding = (recyclerWidth - itemWidth) / 2
                            binding.recyclerViewCoverFlow.setPadding(sidePadding, 0, sidePadding, 0)
                            binding.recyclerViewCoverFlow.clipToPadding = false
                        }

                        isProgrammaticScroll = true
                        binding.recyclerViewCoverFlow.post {
                            binding.recyclerViewCoverFlow.scrollToPosition(currentIndex)
                            val layoutManager = binding.recyclerViewCoverFlow.layoutManager as LinearLayoutManager
                            val viewToSnap = snapHelper.findSnapView(layoutManager)
                            if (viewToSnap != null) {
                                val snapDistance = snapHelper.calculateDistanceToFinalSnap(layoutManager, viewToSnap)
                                if (snapDistance != null) {
                                    binding.recyclerViewCoverFlow.scrollBy(snapDistance[0], snapDistance[1])
                                }
                            }
                        }
                    }
                }
            },
            onMetadataChanged = { rawTitle ->
                val nowPlayingPrefix = getString(R.string.now_playing_prefix)
                val titlePrefix = getString(R.string.title_prefix)
                val artistPrefix = getString(R.string.artist_prefix)

                if (rawTitle.contains(" - ")) {
                    val parts = rawTitle.split(" - ", limit = 2)
                    val artist = parts[0].trim()
                    val title = parts[1].trim()

                    binding.textArtist.text = "$artistPrefix $artist"
                    binding.textTitle.text = "$titlePrefix $title"
                } else {
                    binding.textArtist.text = ""
                    binding.textTitle.text = "$nowPlayingPrefix ${rawTitle.trim()}"
                }
                // 💥 Direkt nach Aktualisierung: Cover abfragen
                if (useLastFMMediaInfo(requireContext())) {
                    fetchLastFmCover()
                }
            }
        )

        binding.buttonPlayPause.setOnClickListener {
            mediaServiceController?.togglePlayPause()
        }
        binding.buttonNext.setOnClickListener {
            mediaServiceController?.skipToNext()
        }
        binding.buttonPrev.setOnClickListener {
            mediaServiceController?.skipToPrevious()
        }
        binding.buttonManualLog.setOnClickListener {
            val rawTitleText = binding.textTitle.text?.toString()
                ?.removePrefix(getString(R.string.title_prefix))
                ?.trim()
            val rawArtistText = binding.textArtist.text?.toString()
                ?.removePrefix(getString(R.string.artist_prefix))
                ?.trim()

            val rawTitle = if (!rawArtistText.isNullOrEmpty()) {
                "$rawArtistText - $rawTitleText"
            } else {
                rawTitleText ?: getString(R.string.unknown_title)
            }

            val currentIndex = mediaServiceController?.getCurrentStreamIndex() ?: 0
            val currentStream = PreferencesHelper.getStreams(requireContext()).getOrNull(currentIndex)
            val streamName = currentStream?.name ?: getString(R.string.unknown_stream)

            PreferencesHelper.logTrack(requireContext(), rawTitle, "$streamName ${getString(R.string.manual_log_suffix)}")
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaServiceController?.disconnect()
        mediaServiceController = null
    }

    private fun useLastFMMediaInfo(context: android.content.Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(Keys.PREF_USE_LAST_FM_MEDIA_INFO, true)
    }

    private fun fetchLastFmCover() {
        val artist = binding.textArtist.text?.removePrefix(getString(R.string.artist_prefix))?.trim()
        val title = binding.textTitle.text?.removePrefix(getString(R.string.title_prefix))?.trim()
        val apiKey = PreferencesHelper.getLastFMApiKey(requireContext())

        if (artist.isNullOrEmpty() || title.isNullOrEmpty() || apiKey.isNullOrEmpty()) {
            Log.w("LastFM", "⚠️ Keine gültigen Daten für Last.fm Cover-Abfrage.")
            return
        }

        Log.d("LastFM", "🎯 Suche Cover für Artist='$artist', Title='$title'")

        val url =
            "http://ws.audioscrobbler.com/2.0/?method=track.getInfo&api_key=$apiKey&artist=${Uri.encode(artist.toString())}&track=${Uri.encode(title.toString())}&format=json"

        thread {
            try {
                val response = URL(url).readText()
                val json = JSONObject(response)

                val images = json
                    .getJSONObject("track")
                    .getJSONObject("album")
                    .getJSONArray("image")

                // 🔥 Suche erstes nicht-leeres Cover-Bild (von groß nach klein)
                var coverUrl: String? = null
                for (i in images.length() - 1 downTo 0) {
                    val imgUrl = images.getJSONObject(i).getString("#text")
                    if (!imgUrl.isNullOrBlank()) {
                        coverUrl = imgUrl
                        break
                    }
                }

                if (!coverUrl.isNullOrBlank()) {
                    Log.d("LastFM", "✅ Bestes verfügbares Cover gefunden: $coverUrl")
                    requireActivity().runOnUiThread {
                        val adapter = binding.recyclerViewCoverFlow.adapter as? StreamCoverCardAdapter
                        adapter?.updateOverrideCover(coverUrl, binding.recyclerViewCoverFlow, snapHelper)
                    }
                } else {
                    Log.d("LastFM", "❌ Kein Cover gefunden oder leer – nichts ändern.")
                }

            } catch (e: Exception) {
                Log.e("LastFM", "❌ Fehler bei Last.fm Anfrage: ${e.message}")
            }
        }
    }

}
