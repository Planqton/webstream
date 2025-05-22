package at.plankt0n.webstream.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import at.plankt0n.webstream.R
import at.plankt0n.webstream.data.Stream
import com.bumptech.glide.Glide

class StreamAdapter(context: Context, private val streams: ArrayList<Stream>) : BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int = streams.size

    override fun getItem(position: Int): Any = streams[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.list_item_stream, parent, false)

        val stream = getItem(position) as Stream

        val nameTextView: TextView = view.findViewById(R.id.streamName)
        val urlTextView: TextView = view.findViewById(R.id.streamURL)
        val imageView: ImageView = view.findViewById(R.id.streamIcon)

        nameTextView.text = stream.name
        urlTextView.text = stream.url

        // Lade das Icon, wenn verfügbar, andernfalls benutze das Standard-Icon
        if (stream.iconUrl.isNotEmpty()) {
            Glide.with(view.context)
                .load(stream.iconUrl)  // Lade das Bild von der URL
                .placeholder(R.drawable.default_icon)  // Zeige das Standardbild, wenn das Bild nicht geladen werden kann
                .error(R.drawable.default_icon)  // Zeige das Standardbild bei Fehlern
                .into(imageView)
        } else {
            // Lade das Standard-Icon, wenn keine URL angegeben ist
            imageView.setImageResource(R.drawable.default_icon)
        }

        return view
    }
}
