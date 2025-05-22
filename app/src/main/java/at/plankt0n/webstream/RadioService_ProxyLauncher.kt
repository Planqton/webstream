//Dient als Proxi Launcher für den RadioService da nach dem ColdStart kein start der Dienste erlaubt ist?package at.plankt0n.webstream_settings

package at.plankt0n.webstream


import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class RadioService_ProxyLauncher : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Service starten mit Extra
        val serviceIntent = Intent(this, RadioService::class.java).apply {
            putExtra("autoplay", true)
        }
        startService(serviceIntent)

        // Activity sofort beenden
        finish()
    }
}
