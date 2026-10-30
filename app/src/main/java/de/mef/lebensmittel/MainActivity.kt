package de.mef.lebensmittel

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import de.mef.lebensmittel.ui.AppRoot
import de.mef.lebensmittel.ui.LebensmittelTheme

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        I18n.lang = I18n.resolve(vm.store.data.value.language)
        // reminders: make sure the notification channel exists and all alarms are planned
        ReminderScheduler.ensureChannel(this)
        ReminderScheduler.rescheduleAll(this, vm.store.data.value.reminders)
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            val data by vm.store.data.collectAsState()
            val lang = I18n.resolve(data.language)
            I18n.lang = lang
            // redraw everything when the language changes; Arabic is laid out right-to-left
            key(lang) {
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (lang.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                ) {
                    LebensmittelTheme(mode = data.themeMode, backgroundDark = data.backgroundIsDark) {
                        AppRoot(vm)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /** lebensmittel://import?d=… links, or text shared from a messenger ("Поділитися" → Lebensmittel). */
    private fun handleIntent(intent: Intent?) {
        // tapped on a reminder notification
        intent?.getStringExtra(ReminderScheduler.EXTRA_OPEN_REMINDERS_DATE)?.let { d ->
            vm.openReminders(runCatching { java.time.LocalDate.parse(d) }.getOrNull())
            return
        }
        val text = when (intent?.action) {
            Intent.ACTION_VIEW -> intent.dataString
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        } ?: return
        vm.receiveText(text)
    }
}
