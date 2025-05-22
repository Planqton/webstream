package at.plankt0n.webstream.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import at.plankt0n.webstream.R
import at.plankt0n.webstream.adapter.StreamCoverCardAdapter
import at.plankt0n.webstream.databinding.FragmentPlayerBinding
import at.plankt0n.webstream.helper.CoverFlow3DEffectDecoration
import at.plankt0n.webstream.helper.MediaServiceController
import at.plankt0n.webstream.helper.PreferencesHelper
import at.plankt0n.webstream.helper.ScaleCenterItemDecoration

class PlayerFragment : Fragment() {

    private lateinit var binding: FragmentPlayerBinding
    private lateinit var snapHelper: LinearSnapHelper
    private var mediaServiceController: MediaServiceController? = null
    private var isProgrammaticScroll = false
    private var lastLoggedRawTitle: String? = null // ✅ Titel-Duplikate vermeiden

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        val streams = PreferencesHelper.getStreams(context)

        binding.textTitle.text = getString(R.string.now_playing_prefix) + "–"
        binding.textArtist.text = ""

        val adapter = StreamCoverCardAdapter(context, streams)
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        binding.recyclerViewCoverFlow.layoutManager = layoutManager
        binding.recyclerViewCoverFlow.adapter = adapter
        binding.recyclerViewCoverFlow.setHasFixedSize(true)
        binding.recyclerViewCoverFlow.clipToPadding = false
        binding.recyclerViewCoverFlow.clipChildren = false
        binding.recyclerViewCoverFlow.addItemDecoration(ScaleCenterItemDecoration(minScale = 0.8f))
        binding.recyclerViewCoverFlow.addItemDecoration(
            CoverFlow3DEffectDecoration(minScale = 0.85f, maxRotation = 25f)
        )

        snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(binding.recyclerViewCoverFlow)

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

        binding.recyclerViewCoverFlow.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val snappedView = snapHelper.findSnapView(layoutManager)
                    val position = snappedView?.let { layoutManager.getPosition(it) } ?: return

                    if (isProgrammaticScroll) {
                        isProgrammaticScroll = false
                    } else {
                        mediaServiceController?.seekToIndex(position)
                    }
                }
            }
        })

        mediaServiceController = MediaServiceController(context)
        mediaServiceController?.initializeAndConnect(
            onConnected = { controller ->
                val currentIndex = controller.currentMediaItemIndex
                if (currentIndex >= 0) {
                    isProgrammaticScroll = true
                    binding.recyclerViewCoverFlow.post {
                        binding.recyclerViewCoverFlow.smoothScrollToPosition(currentIndex)
                    }
                }

                val delaySeconds = PreferencesHelper.getAutoplayandCloseDelay(requireContext())
                val delayMillis = delaySeconds * 1000L

                if (PreferencesHelper.isAutoplayAndCloseEnabled(requireContext())) {
                    controller.play()

                    binding.recyclerViewCoverFlow.postDelayed({
                        if (PreferencesHelper.isAutoplayAndCloseEnabled(requireContext())) {
                            requireActivity().moveTaskToBack(true)
                        }
                    }, delayMillis)
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
            val rawTitleText = binding.textTitle.text?.toString()?.removePrefix(getString(R.string.title_prefix))?.trim()
            val rawArtistText = binding.textArtist.text?.toString()?.removePrefix(getString(R.string.artist_prefix))?.trim()

            val rawTitle = if (!rawArtistText.isNullOrEmpty()) {
                "$rawArtistText - $rawTitleText"
            } else {
                rawTitleText ?: "Unknown Title"
            }

            val currentIndex = mediaServiceController?.getCurrentStreamIndex() ?: 0
            val currentStream = PreferencesHelper.getStreams(requireContext()).getOrNull(currentIndex)
            val streamName = currentStream?.name ?: "Unknown Stream"

            PreferencesHelper.logTrack(requireContext(), rawTitle, "$streamName (Manual log)")
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaServiceController?.disconnect()
        mediaServiceController = null
    }

}
