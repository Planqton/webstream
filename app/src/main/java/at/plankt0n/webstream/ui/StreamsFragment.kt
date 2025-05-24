package at.plankt0n.webstream.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.*
import at.plankt0n.webstream.R
import at.plankt0n.webstream.StreamingService
import at.plankt0n.webstream.adapter.StreamAdapter
import at.plankt0n.webstream.data.Stream
import at.plankt0n.webstream.helper.PreferencesHelper
import at.plankt0n.webstream.helper.UIHelper
import at.plankt0n.webstream.helper.ToastType
import com.bumptech.glide.Glide
import com.google.gson.Gson
import okhttp3.*
import org.json.JSONArray
import java.io.IOException
import java.net.URLEncoder

class StreamsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var addButton: Button
    private lateinit var importButton: Button
    private lateinit var saveButton: Button
    private lateinit var streamAdapter: StreamAdapter

    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { importStreamsFromUri(it) }
            ?: Toast.makeText(requireContext(), getString(R.string.no_file_selected), Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_streams, container, false)

        recyclerView = view.findViewById(R.id.streamsRecyclerView)
        addButton = view.findViewById(R.id.addStreamButton)
        importButton = view.findViewById(R.id.importFromFileButton)
        saveButton = view.findViewById(R.id.saveButton)

        val streamList = PreferencesHelper.getStreams(requireContext()).toMutableList()
        streamAdapter = StreamAdapter(streamList) { stream, position ->
            showAddItemDialog(stream, position)
        }

        recyclerView.apply {
            adapter = streamAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        val dragDropHandler = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                streamAdapter.moveItem(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

            // Highlight beim Drag starten
            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.drag_highlight))
                }
            }

            // Hintergrund zurücksetzen
            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.setBackgroundColor(Color.TRANSPARENT)
            }
        }

        val swipeToDeleteHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private val deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_recycle)
            private val background = ColorDrawable(Color.RED)

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                streamAdapter.removeItem(position)
            }

            override fun onChildDraw(
                c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                background.draw(c)

                deleteIcon?.let {
                    val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                    val iconTop = itemView.top + iconMargin
                    val iconLeft = itemView.right - iconMargin - it.intrinsicWidth
                    it.setBounds(iconLeft, iconTop, itemView.right - iconMargin, iconTop + it.intrinsicHeight)
                    it.draw(c)
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(dragDropHandler).attachToRecyclerView(recyclerView)
        ItemTouchHelper(swipeToDeleteHandler).attachToRecyclerView(recyclerView)

        addButton.setOnClickListener { showAddItemDialog(null) }
        importButton.setOnClickListener { importFileLauncher.launch("application/json") }

        saveButton.setOnClickListener {
            val updatedStreams = streamAdapter.getItems()
            PreferencesHelper.saveStreams(requireContext(), updatedStreams)
            Toast.makeText(requireContext(), getString(R.string.saved_successfully), Toast.LENGTH_SHORT).show()
            // Service-Intent senden, um refreshPlaylist() auszulösen
            val intent = Intent(requireContext(), StreamingService::class.java).apply {
                action = "at.plankt0n.webstream.action.REFRESH_PLAYLIST"
            }
            requireContext().startService(intent)
        }

        return view
    }

    private fun showAddItemDialog(stream: Stream?, position: Int? = null) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_item, null)
        val streamNameEditText = dialogView.findViewById<EditText>(R.id.editStreamName)
        val streamUrlEditText = dialogView.findViewById<EditText>(R.id.editStreamURL)
        val iconUrlEditText = dialogView.findViewById<EditText>(R.id.editIconURL)
        val iconPreview = dialogView.findViewById<ImageView>(R.id.iconPreview)
        val searchLogoButton = dialogView.findViewById<Button>(R.id.buttonSearchLogo)

        if (stream != null) {
            streamNameEditText.setText(stream.name)
            streamUrlEditText.setText(stream.url)
            iconUrlEditText.setText(stream.iconUrl)
            Glide.with(requireContext()).load(stream.iconUrl).placeholder(R.drawable.default_station).into(iconPreview)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val newStream = Stream(
                    streamNameEditText.text.toString(),
                    streamUrlEditText.text.toString(),
                    iconUrlEditText.text.toString()
                )

                val updatedList = streamAdapter.getItems().toMutableList()
                if (stream == null) {
                    updatedList.add(newStream)
                } else if (position != null) {
                    updatedList[position] = newStream
                }
                streamAdapter.updateAll(updatedList)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .apply {
                if (stream != null) {
                    setNeutralButton(getString(R.string.delete)) { _, _ ->
                        val updatedList = streamAdapter.getItems().toMutableList()
                        updatedList.removeAt(position!!)
                        streamAdapter.updateAll(updatedList)
                    }
                }
            }
            .create()

        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog.show()

        streamNameEditText.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(streamNameEditText, InputMethodManager.SHOW_IMPLICIT)

        searchLogoButton.setOnClickListener {
            showLogoSearchDialog { selectedIconUrl ->
                iconUrlEditText.setText(selectedIconUrl)
                Glide.with(requireContext()).load(selectedIconUrl).into(iconPreview)
            }
        }
    }

    private fun showLogoSearchDialog(onSelected: (String) -> Unit) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_icon_search, null)
        val searchInput = dialogView.findViewById<EditText>(R.id.editSearchInput)
        val searchButton = dialogView.findViewById<Button>(R.id.buttonStartSearch)
        val progress = dialogView.findViewById<ProgressBar>(R.id.loadingProgress)
        val gridView = dialogView.findViewById<GridView>(R.id.gridView)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.select_logo))
            .setView(dialogView)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        dialog.show()

        searchButton.setOnClickListener {
            val query = searchInput.text.toString().trim()
            if (query.isEmpty()) {
                UIHelper.showToast(requireContext(), getString(R.string.toast_input_searchtext), 10, ToastType.INFO)
                return@setOnClickListener
            }

            progress.visibility = View.VISIBLE
            gridView.visibility = View.GONE

            searchLogoUrl(requireContext(), query) { resultList ->
                requireActivity().runOnUiThread {
                    progress.visibility = View.GONE
                    if (resultList.isNotEmpty()) {
                        gridView.visibility = View.VISIBLE
                        val adapter = object : BaseAdapter() {
                            override fun getCount() = resultList.size
                            override fun getItem(position: Int) = resultList[position]
                            override fun getItemId(position: Int) = position.toLong()
                            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                                val imageView = ImageView(requireContext())
                                imageView.layoutParams = ViewGroup.LayoutParams(200, 200)
                                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                                Glide.with(requireContext()).load(resultList[position]).into(imageView)
                                return imageView
                            }
                        }
                        gridView.adapter = adapter
                        gridView.setOnItemClickListener { _, _, position, _ ->
                            onSelected(resultList[position])
                            dialog.dismiss()
                        }
                    } else {
                        UIHelper.showToast(requireContext(), getString(R.string.no_logos_found), 10, ToastType.INFO)
                    }
                }
            }
        }
    }

    private fun searchLogoUrl(context: Context, streamName: String, onResult: (List<String>) -> Unit) {
        val client = OkHttpClient()
        val url = "https://de1.api.radio-browser.info/json/stations/search?name=" + URLEncoder.encode(streamName, "UTF-8")
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = onResult(emptyList())
            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { json ->
                    try {
                        val stations = JSONArray(json)
                        val results = mutableListOf<String>()
                        for (i in 0 until stations.length()) {
                            val favicon = stations.getJSONObject(i).getString("favicon")
                            if (favicon.isNotBlank()) results.add(favicon)
                        }
                        onResult(results)
                    } catch (e: Exception) {
                        onResult(emptyList())
                    }
                } ?: onResult(emptyList())
            }
        })
    }

    private fun importStreamsFromUri(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val jsonString = inputStream?.bufferedReader().use { it?.readText() }

            if (!jsonString.isNullOrEmpty()) {
                val importedStreams = Gson().fromJson(jsonString, Array<Stream>::class.java).toList()
                val currentStreams = streamAdapter.getItems().toMutableList()

                for (imported in importedStreams) {
                    val index = currentStreams.indexOfFirst { it.name.equals(imported.name, ignoreCase = true) }
                    if (index != -1) {
                        currentStreams[index] = imported
                    } else {
                        currentStreams.add(imported)
                    }
                }

                streamAdapter.updateAll(currentStreams)
                PreferencesHelper.saveStreams(requireContext(), currentStreams)
                Toast.makeText(requireContext(), getString(R.string.import_success), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), getString(R.string.file_empty), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.import_failed) + ": ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}
