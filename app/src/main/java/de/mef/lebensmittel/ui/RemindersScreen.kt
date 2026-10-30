package de.mef.lebensmittel.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.mef.lebensmittel.AppViewModel
import de.mef.lebensmittel.Dates
import de.mef.lebensmittel.Reminder
import de.mef.lebensmittel.ReminderScheduler
import de.mef.lebensmittel.Screen
import de.mef.lebensmittel.T
import de.mef.lebensmittel.timeText
import java.time.LocalDate

/** Tab "Нагадування": notes with a time for a day; a notification is shown at that time. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(vm: AppViewModel) {
    val data by vm.store.data.collectAsState()
    val context = LocalContext.current
    val day = vm.remindersDate
    var pickingDay by remember { mutableStateOf(false) }

    // permission state is re-read after the user answers the system dialog
    var permissionTick by remember { mutableIntStateOf(0) }
    val notificationsAllowed = remember(permissionTick) { ReminderScheduler.notificationsAllowed(context) }
    val exactAllowed = remember(permissionTick) { ReminderScheduler.exactAlarmsAllowed(context) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionTick++
    }

    fun askNotifications() {
        if (Build.VERSION.SDK_INT >= 33) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // older Android: notifications were switched off in the system settings
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            runCatching { context.startActivity(intent) }
        }
    }

    val dayReminders = remember(data, day) { data.reminders.filter { it.date == day.toString() }.sortedBy { it.time } }
    val upcoming = remember(data, day) {
        val now = System.currentTimeMillis()
        data.reminders.filter { it.enabled && !it.isPast(now) && it.date != day.toString() }
            .sortedBy { it.triggerAt() }
            .take(10)
    }

    Scaffold(
        containerColor = Color.Transparent,
        // transparent screens: text and icons must still follow the theme (light text in dark mode)
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = { MainTabs(vm, Screen.Reminders) },
        topBar = {
            TopAppBar(
                title = { Text(T.tabReminders()) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.newReminder() }) {
                Icon(Icons.Filled.Add, contentDescription = T.reminderNew())
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            if (!notificationsAllowed) {
                WarningCard(T.notificationsOff(), T.allow()) { askNotifications() }
            } else if (!exactAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                WarningCard(T.exactAlarmsOff(), T.allow()) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName))
                    runCatching { context.startActivity(intent) }
                    permissionTick++
                }
            }

            // day switcher
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.remindersDate = day.minusDays(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                }
                TextButton(onClick = { pickingDay = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        Dates.full(day) + (Dates.relative(day)?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                IconButton(onClick = { vm.remindersDate = day.plusDays(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }

            if (dayReminders.isEmpty()) {
                Text(
                    T.noReminders(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                dayReminders.forEach { r ->
                    ReminderRow(
                        r = r,
                        onClick = { vm.editingReminder = r },
                        onToggle = { vm.setReminderEnabled(r, it) },
                        onDelete = { vm.deleteReminder(r) },
                    )
                    HorizontalDivider()
                }
            }

            if (upcoming.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text(T.upcoming(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                upcoming.forEach { r ->
                    Text(
                        "${r.localDate?.let { Dates.short(it) } ?: r.date} · ${r.time} — ${r.text}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { r.localDate?.let { vm.remindersDate = it } }
                            .padding(vertical = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(96.dp))
        }
    }

    if (pickingDay) {
        DayPickerDialog(
            initial = day,
            onPick = {
                vm.remindersDate = it
                pickingDay = false
            },
            onDismiss = { pickingDay = false },
        )
    }

    vm.editingReminder?.let { r ->
        ReminderDialog(
            vm = vm,
            r = r,
            onSaved = { if (!ReminderScheduler.notificationsAllowed(context)) askNotifications() },
        )
    }
}

@Composable
private fun WarningCard(text: String, button: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(text, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onClick) { Text(button) }
        }
    }
}

@Composable
private fun ReminderRow(r: Reminder, onClick: () -> Unit, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    val past = r.isPast()
    val faded = !r.enabled || past
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            r.time,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (faded) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(72.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                r.text,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (past) TextDecoration.LineThrough else null,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (past) {
                Text(T.reminderPast(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = r.enabled, onCheckedChange = onToggle)
        IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = T.delete()) }
    }
}

/** Text, day and time of a reminder. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderDialog(vm: AppViewModel, r: Reminder, onSaved: () -> Unit) {
    val isNew = vm.store.data.collectAsState().value.reminders.none { it.id == r.id }
    var text by remember(r.id) { mutableStateOf(r.text) }
    var date by remember(r.id) { mutableStateOf(r.localDate ?: LocalDate.now()) }
    var pickingDay by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    val time = r.localTime
    val timeState = rememberTimePickerState(
        initialHour = time?.hour ?: 9,
        initialMinute = time?.minute ?: 0,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = { vm.editingReminder = null },
        title = { Text(if (isNew) T.reminderNew() else T.reminderEdit()) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        showError = false
                    },
                    label = { Text(T.reminderText()) },
                    isError = showError,
                    supportingText = if (showError) {
                        { Text(T.enterText()) }
                    } else {
                        null
                    },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(T.reminderDay(), style = MaterialTheme.typography.labelLarge)
                OutlinedButton(onClick = { pickingDay = true }) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(Dates.full(date))
                }
                Spacer(Modifier.height(12.dp))
                Text(T.reminderTime(), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                TimeInput(state = timeState)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (text.isBlank()) {
                    showError = true
                } else {
                    vm.saveReminder(
                        r.copy(
                            text = text.trim(),
                            date = date.toString(),
                            time = timeText(timeState.hour, timeState.minute),
                            enabled = true,
                        ),
                    )
                    onSaved()
                }
            }) { Text(T.save()) }
        },
        dismissButton = {
            Row {
                if (!isNew) {
                    TextButton(
                        onClick = { vm.deleteReminder(r) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text(T.delete()) }
                }
                TextButton(onClick = { vm.editingReminder = null }) { Text(T.cancel()) }
            }
        },
    )

    if (pickingDay) {
        DayPickerDialog(
            initial = date,
            onPick = {
                date = it
                pickingDay = false
            },
            onDismiss = { pickingDay = false },
        )
    }
}
