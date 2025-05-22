package at.plankt0n.webstream.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import at.plankt0n.webstream.R
import at.plankt0n.webstream.data.TrackLogEntry
import java.text.SimpleDateFormat
import java.util.*

class TrackLogAdapter(private var logs: List<TrackLogEntry>) :
    RecyclerView.Adapter<TrackLogAdapter.LogViewHolder>() {

    class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textTimestamp: TextView = view.findViewById(R.id.textTimestamp)
        val textTrack: TextView = view.findViewById(R.id.textTrack)
        val textStreamName: TextView = view.findViewById(R.id.textStreamName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_track_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val entry = logs[position]
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(entry.timestamp))
        holder.textTimestamp.text = time
        holder.textTrack.text = entry.rawTitle
        holder.textStreamName.text = entry.streamName
    }

    override fun getItemCount(): Int = logs.size

    fun update(filteredLogs: List<TrackLogEntry>) {
        logs = filteredLogs
        notifyDataSetChanged()
    }
}
