package at.plankt0n.webstream

object Keys {
    const val PREF_STREAMS = "streams" // List of streams

    //Settings begin
    const val PREF_AUTOSTART_ON_BOOT_ENABLED = "autostart_enabled"
    const val PREF_AUTOSTART_DELAY = "autostart_delay"
    const val PREF_OVERWRITE_LABEL = "overwrite_label_enabled"
    const val PREF_ICON_IN_LABEL = "icon_in_label_enabled"
    const val PREF_SHOW_DEBUG_TOAST = "debug_toast_enabled"
    const val PREF_SHOW_RECONNECTING_TOAST = "reconnecting_toast_enabled"
    const val PREF_SHOW_BUFFERING_TOAST = "buffering_toast_enabled"
    const val PREF_SHOW_ERROR_TOAST = "error_toast_enabled"
    const val PREF_SHOW_INFO_TOAST = "info_toast_enabled"
    const val PREF_AUDIOFOCUS_ENABLED = "audiofocus_enabled"
    const val PREF_ICON_SCALE_FACTOR = "icon_scale_factor"

    const val PREF_AUTOPLAY_AND_CLOSE = "autoplay_and_close_enabled" //Autoplay
    const val PREF_AUTOPLAY_AND_CLOSE_DELAY = "autoplay_and_clsoe_delay"
    //metaLog
    const val PREF_AUTOLOG_ENABLED = "autolog_enabled"
    const val PREF_MAX_LOG_AGE = "max_log_age"

    //Settings end
    const val KEY_TRACK_LOG = "track_log" // Track log liste

    const val PREF_LAST_PLAYED_STREAM_URL = "last_played_stream_url" //Letzte Gespielte URL
}