package at.plankt0n.webstream.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import at.plankt0n.webstream.R
import at.plankt0n.webstream.adapter.TrackLogAdapter
import at.plankt0n.webstream.data.TrackLogEntry
import at.plankt0n.webstream.helper.PreferencesHelper
import java.text.SimpleDateFormat
import java.util.*

class TrackLogFragment : Fragment() {

    private lateinit var adapter: TrackLogAdapter
    private lateinit var fullList: List<TrackLogEntry>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_track_log, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = requireContext()
        val recycler = view.findViewById<RecyclerView>(R.id.trackLogRecycler)
        val filter = view.findViewById<EditText>(R.id.filterTime)
        val clearButton = view.findViewById<Button>(R.id.buttonClearHistory)
        clearButton.setOnClickListener {
            PreferencesHelper.clearTrackLogs(context)
            fullList = emptyList()
            adapter.update(fullList)
        }

        fullList = PreferencesHelper.getTrackLogs(context).reversed()
        adapter = TrackLogAdapter(fullList)
        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = adapter

        filter.addTextChangedListener {
            val input = it.toString().trim()
            if (input.isEmpty()) {
                adapter.update(fullList)
            } else {
                val filtered = fullList.filter { entry ->
                    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        .format(Date(entry.timestamp))
                    val lowerInput = input.lowercase()

                    time.lowercase().contains(lowerInput) ||
                            entry.rawTitle.lowercase().contains(lowerInput) ||
                            entry.streamName.lowercase().contains(lowerInput)
                }

                adapter.update(filtered)
            }
        }
    }
}
