package de.mef.lebensmittel.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.mef.lebensmittel.T
import de.mef.lebensmittel.AppData
import de.mef.lebensmittel.AppViewModel
import de.mef.lebensmittel.Dates
import de.mef.lebensmittel.Kcal
import de.mef.lebensmittel.ShopItem
import de.mef.lebensmittel.formatMoney
import de.mef.lebensmittel.formatNumber
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayScreen(vm: AppViewModel, date: LocalDate) {
    val data by vm.store.data.collectAsState()
    val dayItems = data.days[date.toString()].orEmpty()
    // not bought first, bought at the bottom
    val sorted = remember(dayItems) { dayItems.filter { !it.checked } + dayItems.filter { it.checked } }
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var copyFrom by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val done = dayItems.count { it.checked }

    Scaffold(
        containerColor = Color.Transparent,
        // transparent screens: text and icons must still follow the theme (light text in dark mode)
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Column {
                        Text(Dates.full(date))
                        val sub = listOfNotNull(
                            Dates.relative(date),
                            if (dayItems.isNotEmpty()) T.boughtOf(done, dayItems.size) else null,
                        ).joinToString(" · ")
                        if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { vm.back() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = T.back())
                    }
                },
                actions = {
                    IconButton(onClick = { vm.shareDay(context, date) }, enabled = dayItems.isNotEmpty()) {
                        Icon(Icons.Filled.Share, contentDescription = T.share())
                    }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = T.menu()) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text(T.copyFromOtherDay()) },
                                leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                                onClick = {
                                    menu = false
                                    copyFrom = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(T.copyListText()) },
                                leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                                enabled = dayItems.isNotEmpty(),
                                onClick = {
                                    menu = false
                                    vm.copyDayToClipboard(context, date)
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
                                text = { Text(T.removeBought()) },
                                enabled = done > 0,
                                onClick = {
                                    menu = false
                                    vm.store.removeChecked(date)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(T.clearList()) },
                                enabled = dayItems.isNotEmpty(),
                                onClick = {
                                    menu = false
                                    confirmClear = true
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { vm.startAdding(date) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(T.add()) },
            )
        },
        bottomBar = { DaySummary(dayItems, showKcal = data.showKcal) },
    ) { padding ->
        if (dayItems.isEmpty()) {
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    T.dayEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { vm.startAdding(date) }) { Text(T.addProduct()) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { copyFrom = true }) { Text(T.copyFromOtherDay()) }
            }
        } else {
            LazyColumn(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                items(sorted, key = { it.id }) { item ->
                    ItemRow(
                        item = item,
                        settings = data,
                        onToggle = { vm.store.toggle(date, item.id) },
                        onEdit = { vm.editItem(date, item) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (copyFrom) CopyFromDialog(vm, date) { copyFrom = false }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(T.clearListQ()) },
            text = { Text(T.clearListText(Dates.full(date))) },
            confirmButton = {
                TextButton(onClick = {
                    vm.store.clearDay(date)
                    confirmClear = false
                }) { Text(T.clear()) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(T.cancel()) } },
        )
    }
}

@Composable
private fun ItemRow(item: ShopItem, settings: AppData, onToggle: () -> Unit, onEdit: () -> Unit) {
    val showPhoto = settings.showPhotos
    val a = if (item.checked) 0.5f else 1f
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = item.checked, onCheckedChange = { onToggle() })
        if (showPhoto && item.product.imageUrl != null) {
            Box(Modifier.alpha(a)) { ProductImage(item.product.imageUrl) }
            Spacer(Modifier.width(10.dp))
        }
        Column(
            Modifier
                .weight(1f)
                .alpha(a),
        ) {
            Text(
                item.product.name,
                style = MaterialTheme.typography.bodyLarge,
                color = settings.nameColor?.let { Color(it) } ?: Color.Unspecified,
                textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            item.product.subtitle?.takeIf { settings.showBrand }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            kcalLine(item)?.takeIf { settings.showKcal }?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        Column(
            Modifier
                .alpha(a)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(item.amountLabel, style = MaterialTheme.typography.titleSmall)
            item.price?.let {
                Text(formatMoney(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = T.editQtyPrice()) }
    }
}

private fun kcalLine(item: ShopItem): String? {
    val per = Kcal.per100Label(item.product) ?: return null
    val total = Kcal.total(item) ?: return per
    return "$per · ≈ ${formatNumber(total, 0)} ${T.kcal()}"
}

@Composable
private fun DaySummary(items: List<ShopItem>, showKcal: Boolean) {
    val totals = if (showKcal) items.map { Kcal.total(it) } else emptyList()
    val knownKcal = totals.filterNotNull()
    val unknownKcal = totals.count { it == null }
    // the total is only meaningful when every item on the day has a price
    val prices = if (items.isNotEmpty() && items.all { it.price != null }) items.mapNotNull { it.price } else emptyList()
    val boughtSum = items.filter { it.checked }.mapNotNull { it.price }.sum()
    if (knownKcal.isEmpty() && prices.isEmpty()) return
    Surface(tonalElevation = 3.dp) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            if (prices.isNotEmpty()) {
                Text(
                    T.sumLine(formatMoney(prices.sum())) +
                        if (boughtSum > 0) T.boughtFor(formatMoney(boughtSum)) else "",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            if (knownKcal.isNotEmpty()) {
                Text(
                    T.totalApprox("${formatNumber(knownKcal.sum(), 0)} ${T.kcal()}") +
                        if (unknownKcal > 0) T.unknownKcal(unknownKcal) else "",
                    style = if (prices.isEmpty()) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
