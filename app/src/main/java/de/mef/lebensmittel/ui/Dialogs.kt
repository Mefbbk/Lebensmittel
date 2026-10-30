package de.mef.lebensmittel.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.mef.lebensmittel.T
import de.mef.lebensmittel.AppViewModel
import de.mef.lebensmittel.Dates
import de.mef.lebensmittel.Kcal
import de.mef.lebensmittel.PendingAdd
import de.mef.lebensmittel.QtyUnit
import de.mef.lebensmittel.ShareCodec
import de.mef.lebensmittel.daysSorted
import de.mef.lebensmittel.formatMoney
import de.mef.lebensmittel.formatMoneyPlain
import de.mef.lebensmittel.formatNumber
import de.mef.lebensmittel.parseMoney
import de.mef.lebensmittel.parseAmount
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private fun stepFor(u: QtyUnit): Double = when (u) {
    QtyUnit.G, QtyUnit.ML -> 50.0
    QtyUnit.KG, QtyUnit.L -> 0.5
    QtyUnit.PCS, QtyUnit.PACK -> 1.0
}

private fun isSmallUnit(u: QtyUnit) = u == QtyUnit.G || u == QtyUnit.ML

/** Asks how much of the product to put on the list. Also used to edit or delete an item. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QuantityDialog(vm: AppViewModel, p: PendingAdd) {
    var amountText by remember(p) { mutableStateOf(formatNumber(p.amount)) }
    var unit by remember(p) { mutableStateOf(p.unit) }
    val amount = parseAmount(amountText)
    val kcal = amount?.let { Kcal.total(it, unit, p.product) }
    val editing = p.editItemId != null

    // Price: suggested from the last known price (scaled to the amount) until the user types one.
    var priceTouched by remember(p) { mutableStateOf(false) }
    var priceText by remember(p) { mutableStateOf("") }
    val autoPrice = if (!priceTouched && p.unitPrice != null && unit == p.unitPriceUnit && amount != null) {
        p.unitPrice * amount
    } else {
        null
    }
    val shownPrice = if (priceTouched) priceText else autoPrice?.let { formatMoneyPlain(it) } ?: ""
    val price = if (priceTouched) parseMoney(priceText) else autoPrice
    val priceInvalid = priceTouched && priceText.isNotBlank() && parseMoney(priceText) == null

    AlertDialog(
        onDismissRequest = { vm.pending = null },
        title = { Text(p.product.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                p.product.subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Kcal.per100Label(p.product)?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = {
                        val s = stepFor(unit)
                        amountText = formatNumber(maxOf(s, (amount ?: s) - s))
                    }) { Icon(Icons.Filled.Remove, contentDescription = T.less()) }
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        singleLine = true,
                        label = { Text(T.quantity()) },
                        isError = amount == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    )
                    FilledTonalIconButton(onClick = {
                        amountText = formatNumber((amount ?: 0.0) + stepFor(unit))
                    }) { Icon(Icons.Filled.Add, contentDescription = T.more()) }
                }
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QtyUnit.entries.forEach { u ->
                        FilterChip(
                            selected = unit == u,
                            onClick = {
                                if (unit != u) {
                                    val a = amount
                                    amountText = when {
                                        a == null -> if (isSmallUnit(u)) "500" else "1"
                                        isSmallUnit(u) && a < 10 -> "500"
                                        !isSmallUnit(u) && a >= 50 -> "1"
                                        else -> amountText
                                    }
                                    unit = u
                                }
                            },
                            label = { Text(u.label) },
                        )
                    }
                }
                if (kcal != null) {
                    Text(
                        "≈ ${formatNumber(kcal, 0)} ${T.kcal()}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else if (p.product.kcal100 != null && (unit == QtyUnit.PCS || unit == QtyUnit.PACK)) {
                    Text(
                        T.packWeightUnknown(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = shownPrice,
                    onValueChange = {
                        priceTouched = true
                        priceText = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(T.priceLabel()) },
                    suffix = { Text("€") },
                    isError = priceInvalid,
                    supportingText = {
                        val per = if (amount != null && price != null && amount > 0) price / amount else null
                        Text(
                            when {
                                priceInvalid -> T.enterNumber()
                                per != null && amount != 1.0 -> T.perUnit(formatMoney(per), unit.label)
                                autoPrice != null -> T.filledFromLast()
                                else -> T.canAddLater()
                            },
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = amount != null && !priceInvalid,
                onClick = { amount?.let { vm.confirmPending(it, unit, price) } },
            ) { Text(if (editing) T.save() else T.add()) }
        },
        dismissButton = {
            Row {
                if (editing) {
                    TextButton(
                        onClick = { vm.deletePending() },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text(T.delete()) }
                }
                TextButton(onClick = { vm.pending = null }) { Text(T.cancel()) }
            }
        },
    )
}

/** Shown when a shared list arrives (link, "Share" from a messenger, or clipboard). */
@Composable
fun ImportDialog(vm: AppViewModel, shared: ShareCodec.Shared) {
    val today = LocalDate.now()
    val suggested = shared.date?.takeIf { !it.isBefore(today) } ?: today
    var date by remember(shared) { mutableStateOf(suggested) }
    var picking by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { vm.pendingImport = null },
        title = { Text(T.receivedList()) },
        text = {
            Column {
                Text(
                    Dates.productsCount(shared.items.size) +
                        (shared.date?.let { T.composedFor(Dates.full(it)) } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    Modifier
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    shared.items.forEach {
                        Text(
                            "• ${it.product.name} — ${it.amountLabel}" + (it.price?.let { pr -> ", ${formatMoney(pr)}" } ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(T.addToDay(), style = MaterialTheme.typography.labelLarge)
                OutlinedButton(onClick = { picking = true }) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(Dates.full(date))
                }
            }
        },
        confirmButton = { TextButton(onClick = { vm.confirmImport(date) }) { Text(T.add()) } },
        dismissButton = { TextButton(onClick = { vm.pendingImport = null }) { Text(T.cancel()) } },
    )

    if (picking) {
        DayPickerDialog(
            initial = date,
            onPick = {
                date = it
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayPickerDialog(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                if (millis != null) onPick(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()) else onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(T.cancel()) } },
    ) {
        DatePicker(state = state)
    }
}

/** "Copy the list from last Tuesday to this day". */
@Composable
fun CopyFromDialog(vm: AppViewModel, target: LocalDate, onDismiss: () -> Unit) {
    val data by vm.store.data.collectAsState()
    val days = remember(data, target) { data.daysSorted().filter { it.first != target }.take(60) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(T.copyFromDay()) },
        text = {
            if (days.isEmpty()) {
                Text(T.noOtherLists())
            } else {
                Column(
                    Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    days.forEach { (d, items) ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    vm.copyDay(d, target)
                                    onDismiss()
                                }
                                .padding(vertical = 10.dp),
                        ) {
                            Text(
                                Dates.full(d) + (Dates.relative(d)?.let { " ($it)" } ?: ""),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                Dates.productsCount(items.size) + ": " +
                                    items.take(4).joinToString(", ") { it.product.name } +
                                    if (items.size > 4) "…" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(T.close()) } },
    )
}

/** Pick a picture from the gallery as the app background, or remove it. */
@Composable
fun BackgroundDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    val data by vm.store.data.collectAsState()
    val context = LocalContext.current
    // Android photo picker: no storage permission needed
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.setBackground(context, uri)
    }
    var dim by remember { mutableStateOf(data.backgroundDim) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(T.appBackground()) },
        text = {
            Column {
                Button(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Wallpaper, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (data.backgroundId == null) T.pickFromGallery() else T.pickOtherPicture())
                }
                if (data.backgroundId != null) {
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(onClick = { vm.removeBackground() }, modifier = Modifier.fillMaxWidth()) {
                        Text(T.removeBackground())
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(T.dimHint(), style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = dim,
                        onValueChange = {
                            dim = it
                            vm.backgroundDimPreview = it
                        },
                        onValueChangeFinished = {
                            vm.store.setBackgroundDim(dim)
                            vm.backgroundDimPreview = null
                        },
                        valueRange = 0f..0.9f,
                    )
                    Row {
                        Text(T.brighter(), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                        Text(T.calmer(), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(T.done()) } },
    )
}
