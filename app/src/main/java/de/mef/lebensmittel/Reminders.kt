package de.mef.lebensmittel

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.io.File

/** Plans and cancels the system alarms that show reminder notifications. */
object ReminderScheduler {
    const val CHANNEL_ID = "reminders"
    const val ACTION_REMINDER = "de.mef.lebensmittel.REMINDER"
    const val EXTRA_ID = "reminder_id"
    const val EXTRA_TEXT = "reminder_text"
    const val EXTRA_DATE = "reminder_date"
    const val EXTRA_TIME = "reminder_time"
    /** Set on the notification's tap intent: open the reminders tab on this day. */
    const val EXTRA_OPEN_REMINDERS_DATE = "open_reminders_date"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            val channel = NotificationChannel(CHANNEL_ID, T.tabReminders(), NotificationManager.IMPORTANCE_HIGH)
            nm.createNotificationChannel(channel)
        }
    }

    fun notificationsAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun exactAlarmsAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(AlarmManager::class.java) ?: return false
        return am.canScheduleExactAlarms()
    }

    private fun pendingIntent(context: Context, r: Reminder): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_ID, r.id)
            putExtra(EXTRA_TEXT, r.text)
            putExtra(EXTRA_DATE, r.date)
            putExtra(EXTRA_TIME, r.time)
        }
        return PendingIntent.getBroadcast(
            context,
            r.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Plans the notification (or removes it if the reminder is off or in the past). */
    fun schedule(context: Context, r: Reminder) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendingIntent(context, r)
        am.cancel(pi)
        val at = r.triggerAt() ?: return
        if (!r.enabled || at <= System.currentTimeMillis()) return
        try {
            if (exactAlarmsAllowed(context)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun cancel(context: Context, r: Reminder) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(context, r))
    }

    fun rescheduleAll(context: Context, reminders: List<Reminder>) {
        reminders.forEach { schedule(context, it) }
    }

    /** Shows the notification for a reminder. */
    fun show(context: Context, id: String, text: String, date: String, time: String) {
        if (!notificationsAllowed(context)) return
        ensureChannel(context)
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_REMINDERS_DATE, date)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            id.hashCode(),
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${T.tabReminders()} · $time")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id.hashCode(), notification)
        } catch (e: SecurityException) {
            // permission was taken away in the meantime
        }
    }

    /** Reads the saved data directly (used by receivers when the app is not running). */
    fun loadData(context: Context): AppData {
        val data = Store(File(context.filesDir, "data.json")).data.value
        I18n.lang = I18n.resolve(data.language)
        return data
    }
}

/** Fires at the reminder time and shows the notification. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_REMINDER) return
        val id = intent.getStringExtra(ReminderScheduler.EXTRA_ID) ?: return
        val data = ReminderScheduler.loadData(context)
        // use the saved version: it may have been edited, switched off or deleted
        val r = data.reminders.firstOrNull { it.id == id } ?: return
        if (!r.enabled) return
        ReminderScheduler.show(context, r.id, r.text, r.date, r.time)
    }
}

/** Alarms are wiped when the phone restarts or the app is updated: plan them again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val data = ReminderScheduler.loadData(context)
        ReminderScheduler.rescheduleAll(context, data.reminders)
    }
}
