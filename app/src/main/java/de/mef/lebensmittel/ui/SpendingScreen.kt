package de.mef.lebensmittel.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.mef.lebensmittel.T
import de.mef.lebensmittel.AppViewModel
import de.mef.lebensmittel.Dates
import de.mef.lebensmittel.Screen
import de.mef.lebensmittel.Spending
import de.mef.lebensmittel.formatMoney
import java.time.LocalDate

/** "How much did I spend from X to Y". */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SpendingScreen(vm: AppViewModel) {
    val data by vm.store.data.collectAsState()
    val from = vm.spendFrom
    val to = vm.spendTo
    val onlyBought = vm.spendOnlyBought
    val report = remember(data, from, to, onlyBought) { Spending.report(data, from, to, onlyBought) }
    var picking by remember { mutableStateOf<String?>(null) } // "from" / "to"

    Scaffold(
        containerColor = Color.Transparent,
        // transparent screens: text and icons must still follow the theme (light text in dark mode)
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = { MainTabs(vm, Screen.Spending) },
        topBar = {
            TopAppBar(
                title = { Text(T.tabSpending()) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = { vm.back() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = T.back())
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // quick ranges
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Spending.Preset.entries.forEach { preset ->
                    val r = Spending.range(preset)
                    FilterChip(
                        selected = r.first == from && r.second == to,
                        onClick = {
                            vm.spendFrom = r.first
                            vm.spendTo = r.second
                        },
                        label = { Text(preset.label) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            // custom range
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { picking = "from" }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(T.fromDate("${Dates.short(from)}.${from.year % 100}"))
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { picking = "to" }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(T.toDate("${Dates.short(to)}.${to.year % 100}"))
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { vm.spendOnlyBought = !onlyBought }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    T.onlyBought(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(checked = onlyBought, onCheckedChange = { vm.spendOnlyBought = it })
            }

            // total
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "${Dates.full(report.from)} — ${Dates.full(report.to)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(formatMoney(report.total), style = MaterialTheme.typography.displaySmall)
                    Text(
                        T.spendStats(report.dayCount, formatMoney(report.perDay), report.pricedCount),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (report.unpricedCount > 0) {
                        Text(
                            T.withoutPrice(report.unpricedCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            if (report.byDay.isEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    T.noPricedPurchases(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Spacer(Modifier.height(16.dp))
                Text(T.byDay(), style = MaterialTheme.typography.titleMedium)
                val max = report.byDay.maxOf { it.second }
                report.byDay.forEach { (d, sum) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { vm.open(Screen.Day(d)) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(Dates.short(d), modifier = Modifier.width(76.dp), style = MaterialTheme.typography.bodyMedium)
                        LinearProgressIndicator(
                            // a day with only a Pfand receipt is negative: empty bar
                            progress = { if (max > 0) (sum / max).toFloat().coerceIn(0f, 1f) else 0f },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                        )
                        Text(formatMoney(sum), style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(T.topSpending(), style = MaterialTheme.typography.titleMedium)
                report.top.forEach { (name, sum) ->
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Text(
                            name,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(formatMoney(sum), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    picking?.let { which ->
        DayPickerDialog(
            initial = if (which == "from") from else to,
            onPick = { d: LocalDate ->
                if (which == "from") vm.spendFrom = d else vm.spendTo = d
                if (vm.spendFrom > vm.spendTo) {
                    val t = vm.spendFrom
                    vm.spendFrom = vm.spendTo
                    vm.spendTo = t
                }
                picking = null
            },
            onDismiss = { picking = null },
        )
    }
}
