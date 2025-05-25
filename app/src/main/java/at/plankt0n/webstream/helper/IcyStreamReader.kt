package at.plankt0n.webstream.helper

import android.content.Context
import android.net.Uri
import android.util.Log
import at.plankt0n.webstream.helper.PreferencesHelper
import org.json.JSONObject
import java.io.InputStream
import java.net.URL
import java.net.URLConnection
import kotlin.concurrent.thread


//Class is working but not implemented in StreamingService.kt yet because there is no option in media3 to update Metainfo on the fly
class IcyStreamReader(
    private val context: Context,
    private val streamUrl: String,
    private val onMetadataReceived: (title: String?, artworkUrl: String?) -> Unit
) {
    private var running = false

    fun start() {
        Log.d("ICY", "🔄 Starte IcyStreamReader für URL: $streamUrl")
        thread {
            try {
                val conn = URL(streamUrl).openConnection()
                conn.setRequestProperty("Icy-MetaData", "1")
                conn.connect()

                val metaInt = conn.getHeaderFieldInt("icy-metaint", -1)
                if (metaInt == -1) {
                    Log.d("ICY", "Keine ICY-Metadaten gefunden.")
                    return@thread
                }

                val input = conn.getInputStream()
                val buffer = ByteArray(1024)
                var bytesRead = 0
                var byteCount = 0

                while (running && input.read(buffer).also { bytesRead = it } != -1) {
                    byteCount += bytesRead
                    Log.d("ICY", "Gelesene Bytes: $bytesRead | Total: $byteCount")

                    if (byteCount >= metaInt) {
                        val metaLen = input.read() * 16
                        if (metaLen > 0) {
                            val metaData = ByteArray(metaLen)
                            input.read(metaData)
                            val metaString = String(metaData, charset("UTF-8"))
                            Log.d("ICY", "Metadaten-Block: $metaString")
                            val title = "StreamTitle='(.*?)';".toRegex().find(metaString)?.groupValues?.get(1)
                            title?.let {
                                Log.d("ICY", "Neuer Titel: $it")
                                handleNewIcyTitle(it)
                            }
                        }
                        byteCount = 0
                    }
                }
                input.close()

            } catch (e: Exception) {
                Log.e("ICY", "Fehler: ${e.message}")
            }
        }.also {
            running = true
        }
    }

    fun stop() {
        running = false
    }

    private fun handleNewIcyTitle(icyTitle: String) {
        Log.d("ICY", "Verarbeite Titel: $icyTitle")

        if (!PreferencesHelper.useLastFMMediaInfo(context)) {
            onMetadataReceived(icyTitle, null)
            return
        }

        val parts = icyTitle.split(" - ").map { it.trim() }
        val apiKey = PreferencesHelper.getLastFMApiKey(context)

        Log.d("ICY", "API-Key gefunden: ${!apiKey.isNullOrEmpty()} | Teile: $parts")

        if (parts.size >= 2 && !apiKey.isNullOrEmpty()) {
            val (artist, trackTitle) = if (parts[0].length <= parts[1].length) {
                parts[0] to parts[1]
            } else {
                parts[1] to parts[0]
            }

            Log.d("ICY", "Rufe LastFM auf mit: Artist='$artist' | Title='$trackTitle'")

            fetchAlbumArtFromLastFm(apiKey, artist, trackTitle) { artworkUrl ->
                onMetadataReceived(icyTitle, artworkUrl)
            }
        } else {
            Log.w("ICY", "Kein Last.fm API-Key oder unvollständige Teile. Sende nur Titel.")
            onMetadataReceived(icyTitle, null)
        }
    }


    private fun fetchAlbumArtFromLastFm(
        apiKey: String,
        artist: String,
        title: String,
        onResult: (String?) -> Unit
    ) {
        Log.d("LastFM", "Suche Cover für: Artist='$artist' | Title='$title'")

        val url =
            "http://ws.audioscrobbler.com/2.0/?method=track.getInfo&api_key=$apiKey&artist=${Uri.encode(artist)}&track=${Uri.encode(title)}&format=json"

        thread {
            try {
                val response = URL(url).readText()
                val json = JSONObject(response)

                val images = json
                    .getJSONObject("track")
                    .getJSONObject("album")
                    .getJSONArray("image")
                val largestImage = images.getJSONObject(images.length() - 1).getString("#text")

                if (largestImage.isNotEmpty()) {
                    Log.d("LastFM", "✅ Cover gefunden: $largestImage")
                } else {
                    Log.d("LastFM", "❌ Kein Cover gefunden.")
                }

                onResult(largestImage)
            } catch (e: Exception) {
                Log.e("LastFM", "❌ Fehler bei der Anfrage: ${e.message}")
                onResult(null)
            }
        }
    }

}
