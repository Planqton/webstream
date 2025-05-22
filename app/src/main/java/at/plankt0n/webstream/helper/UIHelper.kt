package at.plankt0n.webstream.helper

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import at.plankt0n.webstream.R

enum class ToastType {
    INFO,
    DEBUG,
    ERROR,
    BUFFERING,
    RECONNECTING
}

object UIHelper {
    private var currentToast: Toast? = null

    fun showToast(
        context: Context,
        message: String,
        durationSeconds: Int = 2,
        type: ToastType? = null
    ) {
        // Wenn Typ angegeben ist, prüfe ob der zugehörige Toast aktiviert ist
        if (type != null && !isToastTypeEnabled(context, type)) return

        val prefix = when (type) {
            ToastType.INFO -> context.getString(R.string.info_toast_string)
            ToastType.DEBUG -> context.getString(R.string.pref_debug_toast_enabled_title)
            ToastType.ERROR -> context.getString(R.string.error_toast_string)
            ToastType.BUFFERING -> context.getString(R.string.buffering_toast_string)
            ToastType.RECONNECTING -> context.getString(R.string.reconnecting_toast_string)
            null -> "" // kein Typ → kein Prefix
        }

        // Log-Ausgabe je nach Typ
        when (type) {
            ToastType.DEBUG -> Log.d("WebStreamToast", message)
            ToastType.INFO -> Log.i("WebStreamToast", message)
            ToastType.ERROR -> Log.e("WebStreamToast", message)
            else -> {}
        }

        // Aktuellen Toast abbrechen
        currentToast?.cancel()

        val textToShow = "$prefix ${message.trim()}"
        val toast = Toast.makeText(context.applicationContext, textToShow.trim(), Toast.LENGTH_SHORT)
        toast.show()
        currentToast = toast

        // Wiederholt anzeigen, falls duration > 2 Sekunden
        if (durationSeconds > 2) {
            val handler = Handler(Looper.getMainLooper())
            val interval = 2000L
            var shown = 1
            val total = durationSeconds / 2

            handler.postDelayed(object : Runnable {
                override fun run() {
                    if (shown < total) {
                        toast.show()
                        shown++
                        handler.postDelayed(this, interval)
                    }
                }
            }, interval)
        }
    }

    private fun isToastTypeEnabled(context: Context, type: ToastType): Boolean {
        return when (type) {
            ToastType.DEBUG -> PreferencesHelper.isDebugToastEnabled(context)
            ToastType.INFO -> PreferencesHelper.isInfoToastEnabled(context)
            ToastType.ERROR -> PreferencesHelper.isErrorToastEnabled(context)
            ToastType.BUFFERING -> PreferencesHelper.isBufferingToastEnabled(context)
            ToastType.RECONNECTING -> PreferencesHelper.isReconnectingToastEnabled(context)
        }
    }
}