package at.plankt0n.webstream

import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import at.plankt0n.webstream.helper.PreferencesHelper
import at.plankt0n.webstream.ui.PlayerFragment
import at.plankt0n.webstream.ui.SettingsFragment
import at.plankt0n.webstream.ui.StreamsFragment
import at.plankt0n.webstream.ui.TrackLogFragment
import at.plankt0n.webstream.Keys

class MainActivity : AppCompatActivity() {

    private lateinit var playerFragment: PlayerFragment
    private lateinit var streamsFragment: StreamsFragment
    private lateinit var settingsFragment: SettingsFragment
    private lateinit var trackLogFragment: TrackLogFragment

    private var activeTag = "PLAYER"

    private lateinit var sharedPrefs: SharedPreferences
    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == Keys.PREF_AUTOPLAY_AND_CLOSE) {
                val isEnabled = prefs.getBoolean(key, false)
                findViewById<Switch>(R.id.toolbar_switch).isChecked = isEnabled
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.main_toolbar)
        setSupportActionBar(toolbar)

        val serviceSwitch = findViewById<Switch>(R.id.toolbar_switch)
        val serviceLabel = findViewById<TextView>(R.id.toolbar_switch_label)
        serviceLabel.text = getString(R.string.toolbar_autoplay_label)

        val current = PreferencesHelper.isAutoplayAndCloseEnabled(this)
        serviceSwitch.isChecked = current

        serviceSwitch.setOnCheckedChangeListener { _, isChecked ->
            PreferencesHelper.setAutoplayAndCloseEnabled(this, isChecked)
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val typedArray = theme.obtainStyledAttributes(
            intArrayOf(R.attr.bottomNavActiveColor, R.attr.bottomNavInactiveColor)
        )
        val activeColor = typedArray.getColor(0, Color.GREEN)
        val inactiveColor = typedArray.getColor(1, Color.GRAY)
        typedArray.recycle()

        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf()
        )
        val colorStateList = ColorStateList(states, intArrayOf(activeColor, inactiveColor))
        bottomNav.itemIconTintList = colorStateList
        bottomNav.itemTextColor = colorStateList

        if (savedInstanceState == null) {
            playerFragment = PlayerFragment()
            streamsFragment = StreamsFragment()
            settingsFragment = SettingsFragment()
            trackLogFragment = TrackLogFragment()

            supportFragmentManager.beginTransaction()
                .add(R.id.nav_host_fragment, playerFragment, "PLAYER")
                .add(R.id.nav_host_fragment, streamsFragment, "STREAMS").hide(streamsFragment)
                .add(R.id.nav_host_fragment, settingsFragment, "SETTINGS").hide(settingsFragment)
                .commit()

            activeTag = "PLAYER"
        } else {
            playerFragment = supportFragmentManager.findFragmentByTag("PLAYER") as PlayerFragment
            streamsFragment = supportFragmentManager.findFragmentByTag("STREAMS") as StreamsFragment
            settingsFragment = supportFragmentManager.findFragmentByTag("SETTINGS") as SettingsFragment
            trackLogFragment = supportFragmentManager.findFragmentByTag("HISTORY") as TrackLogFragment

            val activeFragment = supportFragmentManager.fragments.find { it.isVisible }
            activeTag = activeFragment?.tag ?: "PLAYER"
        }

        bottomNav.setOnItemSelectedListener { item ->
            val newTag = when (item.itemId) {
                R.id.nav_player -> "PLAYER"
                R.id.nav_streams -> "STREAMS"
                R.id.nav_settings -> "SETTINGS"
                R.id.nav_tracklog -> "TRACKLOG"
                else -> return@setOnItemSelectedListener false
            }

            if (newTag == activeTag) return@setOnItemSelectedListener true

            val transaction = supportFragmentManager.beginTransaction()
            supportFragmentManager.findFragmentByTag(activeTag)?.let { transaction.hide(it) }

            if (newTag == "TRACKLOG") {
                var logFragment = supportFragmentManager.findFragmentByTag("TRACKLOG")
                if (logFragment == null) {
                    logFragment = TrackLogFragment()
                    transaction.add(R.id.nav_host_fragment, logFragment, "TRACKLOG")
                } else {
                    transaction.show(logFragment)
                }
            } else {
                val fragment = supportFragmentManager.findFragmentByTag(newTag)
                if (fragment != null) {
                    transaction.show(fragment)
                }
                supportFragmentManager.findFragmentByTag("TRACKLOG")?.let {
                    transaction.remove(it)
                }
            }

            transaction.commit()
            activeTag = newTag
            true
        }

        // ✅ Preference-Änderungslistener registrieren
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPrefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }
}
