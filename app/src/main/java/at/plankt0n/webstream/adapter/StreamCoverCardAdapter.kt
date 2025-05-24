package at.plankt0n.webstream.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import at.plankt0n.webstream.R
import at.plankt0n.webstream.data.Stream
import at.plankt0n.webstream.helper.PreferencesHelper
import com.bumptech.glide.Glide

class StreamCoverCardAdapter(
    private val context: Context,
    private val streams: List<Stream>
) : RecyclerView.Adapter<StreamCoverCardAdapter.StreamViewHolder>() {

    private var scaleFactor: Float = PreferencesHelper.getIconScaleFactor(context)

    fun setScale(percent: Float) {
        scaleFactor = (percent / 100f).coerceIn(0.1f, 1.0f)
        notifyDataSetChanged()
    }

    inner class StreamViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.card)
        val title: TextView = view.findViewById(R.id.card_title)
        val image: ImageView = view.findViewById(R.id.card_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StreamViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card_stream, parent, false)
        return StreamViewHolder(view)
    }

    override fun onBindViewHolder(holder: StreamViewHolder, position: Int) {
        val stream = streams[position % streams.size]
        holder.title.text = stream.name

        // Höhe = scaleFactor * RecyclerView-Höhe → quadratisch
        holder.itemView.post {
            val parent = holder.itemView.parent as? RecyclerView ?: return@post
            val height = parent.height - parent.paddingTop - parent.paddingBottom
            val size = (height * scaleFactor).toInt()

            val layoutParams = holder.itemView.layoutParams
            layoutParams.width = size
            layoutParams.height = size
            holder.itemView.layoutParams = layoutParams
        }

        if (stream.iconUrl.isNullOrBlank()) {
            holder.image.setImageResource(R.drawable.default_station)
        } else {
            Glide.with(context)
                .load(stream.iconUrl)
                .placeholder(R.drawable.default_station)
                .error(R.drawable.default_station)
                .into(holder.image)
        }
    }

    override fun getItemCount(): Int = streams.size

}