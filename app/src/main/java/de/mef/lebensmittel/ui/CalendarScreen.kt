package de.mef.lebensmittel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.mef.lebensmittel.T
import de.mef.lebensmittel.AppViewModel
import de.mef.lebensmittel.Dates
import de.mef.lebensmittel.Screen
import de.mef.lebensmittel.ShopItem
import de.mef.lebensmittel.daysSorted
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(vm: AppViewModel) {
    val data by vm.store.data.collectAsState()
    val context = LocalContext.current
    val today = LocalDate.now()
    val month = vm.calendarMonth
    var menu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        // transparent screens: text and icons must still follow the theme (light text in dark mode)
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = { MainTabs(vm, Screen.Calendar) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(T.appTitle()) },
                actions = {
                    IconButton(onClick = { vm.openSpending() }) {
                        Icon(Icons.Filled.Euro, contentDescription = T.tabSpending())
                    }
                    IconButton(onClick = { vm.importFromClipboard(context) }) {
                        Icon(Icons.Filled.ContentPaste, contentDescription = T.pasteFromClipboard())
                    }
                    SettingsAction(vm)
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = T.menu()) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text(T.tabSettings()) },
                                leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                                onClick = {
                                    menu = false
                                    vm.open(Screen.Settings)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(T.spendingPeriod()) },
                                leadingIcon = { Icon(Icons.Filled.Euro, contentDescription = null) },
                                onClick = {
                                    menu = false
                                    vm.openSpending()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(T.pasteReceived()) },
                                onClick = {
                                    menu = false
                                    vm.importFromClipboard(context)
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { vm.open(Screen.Day(today)) },
                icon = { Icon(Icons.Filled.Today, contentDescription = null) },
                text = { Text(T.today()) },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
        ) {
            // month header
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.calendarMonth = month.minusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = T.prevMonth())
                }
                Text(
                    Dates.month(month),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { vm.calendarMonth = java.time.YearMonth.now() },
                )
                IconButton(onClick = { vm.calendarMonth = month.plusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = T.nextMonth())
                }
            }
            // weekday names
            Row(Modifier.fillMaxWidth()) {
                T.weekdaysHeader().forEach {
                    Text(
                        it,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            // days grid
            val first = month.atDay(1)
            val offset = first.dayOfWeek.value - 1
            val length = month.lengthOfMonth()
            val rows = (offset + length + 6) / 7
            for (r in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (c in 0 until 7) {
                        val dayNum = r * 7 + c - offset + 1
                        val date = if (dayNum in 1..length) month.atDay(dayNum) else null
                        DayCell(
                            date = date,
                            items = date?.let { data.days[it.toString()] },
                            today = today,
                            modifier = Modifier.weight(1f),
                            onClick = { if (date != null) vm.open(Screen.Day(date)) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(T.tabLists(), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            val lists = remember(data) { data.daysSorted() }
            if (lists.isEmpty()) {
                Text(
                    T.noListsYet(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            lists.take(20).forEach { (date, items) ->
                val done = items.count { it.checked }
                Card(
                    onClick = { vm.open(Screen.Day(date)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(
                            Dates.full(date) + (Dates.relative(date)?.let { " · $it" } ?: ""),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            T.listSummary(
                                Dates.productsCount(items.size),
                                done,
                                items.take(5).joinToString(", ") { it.product.name },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.height(96.dp))
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate?,
    items: List<ShopItem>?,
    today: LocalDate,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .aspectRatio(1f)
            .padding(2.dp),
    ) {
        if (date != null) {
        val isToday = date == today
        val has = !items.isNullOrEmpty()
        val left = items?.count { !it.checked } ?: 0
        val colors = MaterialTheme.colorScheme
        val bg = when {
            isToday -> colors.primary
            has -> colors.secondaryContainer
            else -> Color.Transparent
        }
        val fg = when {
            isToday -> colors.onPrimary
            has -> colors.onSecondaryContainer
            date.isBefore(today) -> colors.onSurface.copy(alpha = 0.45f)
            else -> colors.onSurface
        }
        Column(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(bg)
                .clickable(onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                date.dayOfMonth.toString(),
                color = fg,
                fontWeight = if (isToday || has) FontWeight.Bold else FontWeight.Normal,
            )
            if (has) {
                Text(
                    if (left == 0) "✓" else left.toString(),
                    color = fg,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        }
    }
}
