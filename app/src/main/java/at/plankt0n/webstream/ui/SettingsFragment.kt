package at.plankt0n.webstream.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.preference.*
import at.plankt0n.webstream.R
import at.plankt0n.webstream.Keys
import at.plankt0n.webstream.helper.ToastType
import at.plankt0n.webstream.helper.UIHelper

class SettingsFragment : PreferenceFragmentCompat() {

    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == Keys.PREF_AUTOPLAY_AND_CLOSE) {
                findPreference<SwitchPreferenceCompat>(key)?.isChecked =
                    sharedPreferences.getBoolean(key, false)
            }

            if (key == Keys.PREF_AUTOPLAY_AND_CLOSE_DELAY) {
                findPreference<SeekBarPreference>(key)?.value =
                    sharedPreferences.getInt(key, 10)
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)

        //Categories

        val categoryAutostart = PreferenceCategory(context).apply {
            title = getString(R.string.category_autostart)
        }
        val categoryBehaviour = PreferenceCategory(context).apply {
            title = getString(R.string.category_behaviour)
        }
        val categoryDisplay = PreferenceCategory(context).apply {
            title = getString(R.string.category_display)
        }
        val categoryDevelopment = PreferenceCategory(context).apply {
            title = getString(R.string.category_development)
        }
        val categoryLogMetaInfo = PreferenceCategory(context).apply {
            title = getString(R.string.category_metainfologging)
        }

        //Settings

        val preferenceAudioFocusEnabled = SwitchPreferenceCompat(context).apply { //Audiofocus
            key = Keys.PREF_AUDIOFOCUS_ENABLED
            title = getString(R.string.pref_audiofocus_enabled_title)
            summary = getString(R.string.pref_audiofocus_description)
            setDefaultValue(false)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_autostart)
        }

        val preferenceLastFMMediaInfo = SwitchPreferenceCompat(context).apply { //Audiofocus
            key = Keys.PREF_USE_LAST_FM_MEDIA_INFO
            title = getString(R.string.pref_last_fm_media_info_title)
            summary = getString(R.string.pref_last_fm_media_info_description)
            setDefaultValue(false)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_mediainfo)
        }

        val preferenceLastFMApiKey = EditTextPreference(context).apply {
            key = Keys.Pref_LAST_FM_API_KEY
            title = getString(R.string.pref_last_fm_api_key_title)
            summary = getString(R.string.pref_last_fm_api_key_summary)
            dialogTitle = getString(R.string.pref_last_fm_api_key_dialogtitle)
            setDefaultValue("")
            icon = ContextCompat.getDrawable(context, R.drawable.ic_key) // optional, falls vorhanden
        }

        val preferenceAutoPlayandCloseEnabled = SwitchPreferenceCompat(context).apply { //Autoplay and Close Player
            key = Keys.PREF_AUTOPLAY_AND_CLOSE
            title = getString(R.string.pref_autoplay_and_close_title)
            summary = getString(R.string.pref_autoplay_and_close_description)
            setDefaultValue(false)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_autostart)
        }

        val preferenceAutoPlayEnabled = SwitchPreferenceCompat(context).apply { //Autoplay and Close Player
            key = Keys.PREF_AUTOPLAY
            title = getString(R.string.pref_autoplay_itle)
            summary = getString(R.string.pref_autoplay_description)
            setDefaultValue(false)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_autostart)
        }

        val preferenceAutoPlayandCloseDelay = SeekBarPreference(context).apply { //If Autoplay and Close Player is enabled, this is the delay before closing the player
            key = Keys.PREF_AUTOPLAY_AND_CLOSE_DELAY
            title = getString(R.string.pref_autoplay_and_close_delay_title)
            summary = getString(R.string.pref_autoplay_and_close_delay_summary)
            min = 0
            max = 30
            showSeekBarValue = true
            setDefaultValue(10)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_delay)
        }

        val preferenceAutostartOnBootEnable = SwitchPreferenceCompat(context).apply { //Autostart on boot
            key = Keys.PREF_AUTOSTART_ON_BOOT_ENABLED
            title = getString(R.string.pref_autostart_enabled_title)
            summary = getString(R.string.pref_autostart_description)
            setDefaultValue(false)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_mediainfo)

            setOnPreferenceChangeListener { _, newValue ->
                if (newValue == true) {
                    checkAndRequestBatteryOptimizationException()
                }
                true
            }
        }

        val preferenceAutostartDelay = SeekBarPreference(context).apply { //Autostart after Boot delay
            key = Keys.PREF_AUTOSTART_DELAY
            title = getString(R.string.pref_autostart_delay_title)
            summary = getString(R.string.pref_autostart_delay_summary)
            min = 0
            max = 600
            showSeekBarValue = true
            setDefaultValue(0)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_delay)
        }

        val preferenceIconScaleFactor = SeekBarPreference(context).apply { //Icon scaling
            key = Keys.PREF_ICON_SCALE_FACTOR
            title = getString(R.string.pref_icon_scale_factor_title)
            summary = getString(R.string.pref_icon_scale_factor_summary)
            min = 0
            max = 100
            showSeekBarValue = true
            setDefaultValue(100)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_scale)
        }

        val preferenceMaxLogAge = SeekBarPreference(context).apply { //Log retention
            key = Keys.PREF_MAX_LOG_AGE
            title = getString(R.string.pref_max_log_age_title)
            summary = getString(R.string.pref_max_log_age_summary)
            min = 0
            max = 30
            showSeekBarValue = true
            setDefaultValue(10)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_tab_history)
        }

        val preferenceAutoLogEnabled = SwitchPreferenceCompat(context).apply { //Auto logging
            key = Keys.PREF_AUTOLOG_ENABLED
            title = getString(R.string.pref_auto_log_enabled_title)
            summary = getString(R.string.pref_auto_log_enabled_description)
            setDefaultValue(true)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_tab_history)
        }

        val preferenceOverwriteLabel = SwitchPreferenceCompat(context).apply { //Overwrite metadata
            key = Keys.PREF_OVERWRITE_LABEL
            title = getString(R.string.pref_overwrite_label_title)
            summary = getString(R.string.pref_overwrites_description)
            setDefaultValue(false)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_overwrite)
        }

        val preferenceShowDebugToasts = SwitchPreferenceCompat(context).apply { //Debug Toasts
            key = Keys.PREF_SHOW_DEBUG_TOAST
            title = getString(R.string.pref_debug_toast_enabled_title)
            summary = getString(R.string.pref_debug_toast_description)
            setDefaultValue(true)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_toast)
        }

        val preferenceShowReconnectingToast = SwitchPreferenceCompat(context).apply { //Reconnecting Toasts
            key = Keys.PREF_SHOW_RECONNECTING_TOAST
            title = getString(R.string.pref_reconnecting_toast_enabled_title)
            summary = getString(R.string.pref_reconnecting_toast_description)
            setDefaultValue(true)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_toast)
        }

        val preferenceShowBufferingToast = SwitchPreferenceCompat(context).apply { //Buffering Toasts
            key = Keys.PREF_SHOW_BUFFERING_TOAST
            title = getString(R.string.pref_buffering_toast_enabled_title)
            summary = getString(R.string.pref_buffering_toast_description)
            setDefaultValue(true)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_toast)
        }

        val preferenceShowErrorToast = SwitchPreferenceCompat(context).apply { //Error Toasts
            key = Keys.PREF_SHOW_ERROR_TOAST
            title = getString(R.string.pref_error_toast_enabled_title)
            summary = getString(R.string.pref_error_toast_description)
            setDefaultValue(true)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_toast)
        }

        val preferenceShowInfoToast = SwitchPreferenceCompat(context).apply { //Info Toasts
            key = Keys.PREF_SHOW_INFO_TOAST
            title = getString(R.string.pref_info_toast_enabled_title)
            summary = getString(R.string.pref_info_toast_description)
            setDefaultValue(true)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_toast)
        }

        //add settings to screen

        screen.addPreference(categoryBehaviour) //categoryBehaviour
        // screen.addPreference(preferenceAudioFocusEnabled)
        screen.addPreference(categoryAutostart) //categoryAutostart
        screen.addPreference(preferenceAudioFocusEnabled)
        //screen.addPreference(preferenceAutostartOnBootEnable)
        //screen.addPreference(preferenceAutostartDelay)
        screen.addPreference(preferenceAutoPlayEnabled)
        screen.addPreference(preferenceAutoPlayandCloseEnabled)
        screen.addPreference(preferenceAutoPlayandCloseDelay)

        screen.addPreference(categoryLogMetaInfo) //categoryLogMetaInfo
        screen.addPreference(preferenceAutoLogEnabled)
        screen.addPreference(preferenceMaxLogAge)

        screen.addPreference(categoryDisplay)  //categoryDisplay


        //Class for LAST.FM is working but not implemented in StreamingService.kt yet because there is no option in media3 to update Metainfo on the fly
        //so it only uses this for the ai not for the Mediasession
        screen.addPreference(preferenceLastFMMediaInfo)
        screen.addPreference(preferenceLastFMApiKey)

        screen.addPreference(preferenceIconScaleFactor)
        screen.addPreference(preferenceShowInfoToast)
        screen.addPreference(preferenceShowErrorToast)
        //screen.addPreference(preferenceShowReconnectingToast)
        //screen.addPreference(preferenceShowBufferingToast)
        screen.addPreference(categoryDevelopment) //categoryDevelopment
        screen.addPreference(preferenceShowDebugToasts)

        preferenceScreen = screen

        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    private fun checkAndRequestBatteryOptimizationException() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = requireContext().packageName

            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }
}
