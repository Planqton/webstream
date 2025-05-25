package at.plankt0n.webstream.helper

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import at.plankt0n.webstream.Keys
import at.plankt0n.webstream.R
import at.plankt0n.webstream.data.Stream
import at.plankt0n.webstream.data.TrackLogEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Helper object to manage SharedPreferences-related logic for the app.
 * Provides convenience functions to read and write common app settings.
 */
object PreferencesHelper {

    fun isAutostartOnBootEnabled(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(Keys.PREF_AUTOSTART_ON_BOOT_ENABLED, false)
    }

    fun getAutostartDelay(context: Context): Int {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(Keys.PREF_AUTOSTART_DELAY, 0)
    }

    fun getAutoplayandCloseDelay(context: Context): Int {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(Keys.PREF_AUTOPLAY_AND_CLOSE_DELAY, 10)
    }

    fun isAutoLogEnabled(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(Keys.PREF_AUTOLOG_ENABLED, true)
    }

    fun isAutoPlayEnabled(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(Keys.PREF_AUTOPLAY, true)
    }

    fun getIconScaleFactor(context: Context): Float {
        val percent = PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(Keys.PREF_ICON_SCALE_FACTOR, 100)
        return (percent.coerceIn(10, 100)) / 100f
    }

    fun isOverwriteLabelEnabled(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(Keys.PREF_OVERWRITE_LABEL, false)
    }

    fun isAudiofocusEnabled(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(Keys.PREF_AUDIOFOCUS_ENABLED, true)
    }

    fun isDebugToastEnabled(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(Keys.PREF_SHOW_DEBUG_TOAST, false)
    }

    fun isBufferingToastEnabled(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(Keys.PREF_SHOW_BUFFERING_TOAST, false)
    }

    fun isReconnectingToastEnabled(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(Keys.PREF_SHOW_RECONNECTING_TOAST, false)
    }

    fun isErrorToastEnabled(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(Keys.PREF_SHOW_ERROR_TOAST, true)
    }

    fun useLastFMMediaInfo(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(Keys.PREF_USE_LAST_FM_MEDIA_INFO, true)
    }

    fun getLastFMApiKey(context: Context): String? {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Keys.Pref_LAST_FM_API_KEY, null)
    }

    fun isInfoToastEnabled(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(Keys.PREF_SHOW_INFO_TOAST, true)
    }

    fun getStreams(context: Context): List<Stream> {
        val json = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Keys.PREF_STREAMS, null)

        return if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<Stream>>() {}.type
            Gson().fromJson(json, type)
        } else {
            emptyList()
        }
    }

    fun saveStreams(context: Context, streams: List<Stream>) {
        val json = Gson().toJson(streams)
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit {
            putString(Keys.PREF_STREAMS, json)
        }
    }

    fun saveLastPlayedStreamUrl(context: Context, url: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit {
            putString(Keys.PREF_LAST_PLAYED_STREAM_URL, url)
        }
    }

    fun getLastPlayedStreamUrl(context: Context): String? {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(Keys.PREF_LAST_PLAYED_STREAM_URL, null)
    }

    fun saveLastPlayedStreamIndex(context: Context, index: Int) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit {
            putInt(Keys.PREF_LAST_PLAYED_STREAM_INDEX, index)
        }
    }

    fun getLastPlayedStreamIndex(context: Context): Int {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(Keys.PREF_LAST_PLAYED_STREAM_INDEX, 0)
    }

    fun isAutoplayAndCloseEnabled(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(Keys.PREF_AUTOPLAY_AND_CLOSE, false)
    }

    fun setAutoplayAndCloseEnabled(context: Context, enabled: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit {
            putBoolean(Keys.PREF_AUTOPLAY_AND_CLOSE, enabled)
        }
    }

    /**
     * Logs a played track (with timestamp, title, and stream name) into a persistent list.
     * Cleans up logs older than [maxDays] and ensures max size is not exceeded.
     */
    fun logTrack(context: Context, rawTitle: String, streamName: String = "?") {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val gson = Gson()

        // Load existing logs
        val logsJson = prefs.getString(Keys.KEY_TRACK_LOG, "[]")
        val type = object : TypeToken<MutableList<TrackLogEntry>>() {}.type
        val logs: MutableList<TrackLogEntry> = gson.fromJson(logsJson, type) ?: mutableListOf()

        // Remove old entries
        val maxDays = prefs.getInt(Keys.PREF_MAX_LOG_AGE, 10)
        if (maxDays > 0) {
            val cutoff = System.currentTimeMillis() - (maxDays * 24L * 60 * 60 * 1000)
            logs.removeAll { it.timestamp < cutoff }
        }

        // Add the new entry
        logs.add(TrackLogEntry(System.currentTimeMillis(), rawTitle, streamName))

        // Save updated logs
        prefs.edit {
            putString(Keys.KEY_TRACK_LOG, gson.toJson(logs))
        }

        // Enforce maximum log entries limit
        val maxEntries = 8000
        if (logs.size > maxEntries) {
            logs.subList(0, logs.size - maxEntries).clear()
        }

        Log.d("StreamService", context.getString(R.string.log_saved_message, rawTitle))

    }

    fun getTrackLogs(context: Context): List<TrackLogEntry> {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val gson = Gson()
        val logsJson = prefs.getString(Keys.KEY_TRACK_LOG, "[]")
        val type = object : TypeToken<List<TrackLogEntry>>() {}.type
        return gson.fromJson(logsJson, type) ?: emptyList()
    }

    fun clearTrackLogs(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit {
            remove(Keys.KEY_TRACK_LOG)
        }
    }
}
