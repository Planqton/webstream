
package at.plankt0n.webstream.ui

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import at.plankt0n.webstream.R
import at.plankt0n.webstream.data.Stream
import at.plankt0n.webstream.adapter.StreamAdapter
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

    private lateinit var streamsListView: ListView
    private lateinit var addButton: Button
    private lateinit var importButton: Button
    private lateinit var streamList: ArrayList<Stream>

    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            importStreamsFromUri(uri)
        } else {
            Toast.makeText(requireContext(), getString(R.string.no_file_selected), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = inflater.inflate(R.layout.fragment_streams, container, false)

        streamsListView = binding.findViewById(R.id.streamsListView)
        addButton = binding.findViewById(R.id.addStreamButton)
        importButton = binding.findViewById(R.id.importFromFileButton)

        streamList = ArrayList(PreferencesHelper.getStreams(requireContext()))
        val adapter = StreamAdapter(requireContext(), streamList)
        streamsListView.adapter = adapter

        addButton.setOnClickListener {
            showAddItemDialog(null)
        }

        importButton.setOnClickListener {
            importFileLauncher.launch("application/json")
        }

        streamsListView.setOnItemClickListener { _, _, position, _ ->
            showAddItemDialog(streamList[position], position)
        }

        return binding
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
            Glide.with(requireContext())
                .load(stream.iconUrl)
                .placeholder(R.drawable.default_station)
                .into(iconPreview)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val name = streamNameEditText.text.toString()
                val url = streamUrlEditText.text.toString()
                val iconUrl = iconUrlEditText.text.toString()

                val newStream = Stream(name, url, iconUrl)

                if (stream == null) {
                    streamList.add(newStream)
                } else {
                    streamList[position!!] = newStream
                }

                PreferencesHelper.saveStreams(requireContext(), streamList)
                (streamsListView.adapter as BaseAdapter).notifyDataSetChanged()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .apply {
                if (stream != null) {
                    setNeutralButton(getString(R.string.delete)) { _, _ ->
                        streamList.removeAt(position!!)
                        PreferencesHelper.saveStreams(requireContext(), streamList)
                        (streamsListView.adapter as BaseAdapter).notifyDataSetChanged()
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

    private fun importStreamsFromUri(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val jsonString = inputStream?.bufferedReader().use { it?.readText() }

            if (!jsonString.isNullOrEmpty()) {
                val importedStreams = Gson().fromJson(jsonString, Array<Stream>::class.java).toList()
                streamList.addAll(importedStreams)
                PreferencesHelper.saveStreams(requireContext(), streamList)
                (streamsListView.adapter as BaseAdapter).notifyDataSetChanged()
                Toast.makeText(requireContext(), getString(R.string.import_success), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), getString(R.string.file_empty), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.import_failed) + ": ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun searchLogoUrl(context: Context, streamName: String, onResult: (List<String>) -> Unit) {
        val client = OkHttpClient()
        val url = "https://de1.api.radio-browser.info/json/stations/search?name=" + URLEncoder.encode(streamName, "UTF-8")
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                val json = response.body?.string()
                if (json != null) {
                    try {
                        val stations = JSONArray(json)
                        val results = mutableListOf<String>()
                        for (i in 0 until stations.length()) {
                            val favicon = stations.getJSONObject(i).getString("favicon")
                            if (favicon.isNotBlank()) {
                                results.add(favicon)
                            }
                        }
                        onResult(results)
                    } catch (e: Exception) {
                        onResult(emptyList())
                    }
                } else {
                    onResult(emptyList())
                }
            }
        })
    }
}
