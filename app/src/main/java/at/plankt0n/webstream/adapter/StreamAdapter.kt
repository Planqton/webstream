package at.plankt0n.webstream.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import at.plankt0n.webstream.R
import at.plankt0n.webstream.data.Stream
import com.bumptech.glide.Glide

class StreamAdapter(
    private val streams: MutableList<Stream>,
    private val onItemClick: (Stream, Int) -> Unit
) : RecyclerView.Adapter<StreamAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rowNumber: TextView = view.findViewById(R.id.rowNumber)
        val icon: ImageView = view.findViewById(R.id.streamIcon)
        val name: TextView = view.findViewById(R.id.streamName)
        val url: TextView = view.findViewById(R.id.streamURL)

        init {
            view.setOnClickListener {
                onItemClick(streams[adapterPosition], adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_stream, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = streams.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val stream = streams[position]
        holder.rowNumber.text = (position + 1).toString() // 1-basiert
        holder.name.text = stream.name
        holder.url.text = stream.url
        Glide.with(holder.icon.context)
            .load(stream.iconUrl)
            .placeholder(R.drawable.default_icon)
            .into(holder.icon)
    }

    fun removeItem(position: Int) {
        streams.removeAt(position)
        notifyItemRemoved(position)
    }

    fun moveItem(from: Int, to: Int) {
        val stream = streams.removeAt(from)
        streams.add(to, stream)
        notifyItemMoved(from, to)
    }

    fun updateAll(newStreams: List<Stream>) {
        streams.clear()
        streams.addAll(newStreams)
        notifyDataSetChanged()
    }

    fun getItems(): List<Stream> = streams
}
