package de.mef.lebensmittel.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import de.mef.lebensmittel.Lang
import de.mef.lebensmittel.I18n
import de.mef.lebensmittel.T
import de.mef.lebensmittel.AppViewModel
import de.mef.lebensmittel.Dates
import de.mef.lebensmittel.DictEntry
import de.mef.lebensmittel.Product
import de.mef.lebensmittel.Stores
import de.mef.lebensmittel.Txt
import de.mef.lebensmittel.formatNumber
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(vm: AppViewModel, date: LocalDate) {
    val data by vm.store.data.collectAsState()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    val q = vm.query
    // "Milch Edeka": product words + supermarket typed into the field
    val plainQ = remember(q) { Stores.splitQuery(q).first }
    val parts = remember(q) { vm.searchParts(q) }
    val german = parts.first
    val typedStore = parts.second
    val knownHits = remember(q, data) {
        if (plainQ.isBlank()) vm.store.frequent(30) else vm.store.searchKnown(plainQ, german)
    }
    val dictHits = remember(q) { if (plainQ.isBlank()) emptyList() else vm.dict.search(plainQ, 6) }
    val plainName = remember(q) { vm.plainName(plainQ) }
    val onList = data.days[date.toString()]?.size ?: 0
    val offTerm = vm.offTerm
    val alreadySearched = offTerm != null && Txt.norm(offTerm) == Txt.norm(german) &&
        (typedStore == null || typedStore == vm.offStore)

    fun runSearch(term: String, store: String? = typedStore) {
        keyboard?.hide()
        vm.searchOff(term, store)
    }

    Scaffold(
        containerColor = Color.Transparent,
        // transparent screens: text and icons must still follow the theme (light text in dark mode)
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Column {
                        Text(T.addProduct())
                        Text(
                            T.inListCount(Dates.full(date), onList),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { vm.back() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = T.back())
                    }
                },
                actions = {
                    IconButton(onClick = { context.findActivity()?.let { vm.scan(it, date) } }) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = T.scanBarcode())
                    }
                    TextButton(onClick = { vm.back() }) { Text(T.done()) }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding(),
        ) {
            OutlinedTextField(
                value = q,
                onValueChange = { vm.query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .focusRequester(focus),
                placeholder = { Text(T.searchPlaceholder()) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (q.isNotEmpty()) {
                        IconButton(onClick = { vm.query = "" }) { Icon(Icons.Filled.Close, contentDescription = T.clear()) }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search,
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                keyboardActions = KeyboardActions(onSearch = { runSearch(german) }),
            )
            if (vm.scanLoading) LinearProgressIndicator(Modifier.fillMaxWidth())

            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
                if (q.isBlank()) {
                    item { SectionHeader(T.frequent()) }
                    if (knownHits.isEmpty()) {
                        item {
                            Hint(
                                T.startTypingHint(),
                            )
                        }
                    }
                    items(knownHits, key = { "k" + it.product.key }) { k ->
                        ProductRow(
                            product = k.product,
                            showPhoto = data.showPhotos,
                            nameColor = data.nameColor,
                            extra = "${formatNumber(k.lastAmount)} ${k.lastUnit.label}",
                            onClick = { vm.chooseKnown(date, k) },
                        )
                    }
                } else {
                    // 1. add exactly what was typed
                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { vm.choosePlain(date, plainQ) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(T.addPlain(plainName), fontWeight = FontWeight.SemiBold)
                                Text(
                                    T.withoutChoosing(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        HorizontalDivider()
                    }

                    // 2. products entered before
                    if (knownHits.isNotEmpty()) {
                        item { SectionHeader(T.myProducts()) }
                        items(knownHits, key = { "k" + it.product.key }) { k ->
                            ProductRow(
                                product = k.product,
                                showPhoto = data.showPhotos,
                                nameColor = data.nameColor,
                                extra = "${formatNumber(k.lastAmount)} ${k.lastUnit.label}",
                                onClick = { vm.chooseKnown(date, k) },
                            )
                        }
                    }

                    // 3. dictionary (German / Ukrainian)
                    if (dictHits.isNotEmpty()) {
                        item { SectionHeader(T.dictionary()) }
                        items(dictHits, key = { "d" + it.de }) { e ->
                            DictRow(
                                e = e,
                                onSearch = { runSearch(e.de) },
                                onPlain = { vm.chooseDict(date, e) },
                            )
                        }
                    }

                    // 4. Open Food Facts
                    item { SectionHeader(T.dbProducts()) }
                    if (!alreadySearched) {
                        item {
                            OutlinedButton(
                                onClick = { runSearch(german) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            ) {
                                Icon(Icons.Filled.Search, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    T.searchFor(german) +
                                        ((typedStore ?: vm.offStore)?.let { T.inStore(Stores.displayName(it)) } ?: ""),
                                )
                            }
                        }
                    }
                    if (offTerm != null) {
                        if (!alreadySearched) {
                            item { Hint(T.resultsFor(offTerm)) }
                        }
                        if (vm.offLoading) {
                            item {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center,
                                ) { CircularProgressIndicator() }
                            }
                        }
                        // supermarket drop-down: only stores that appear in the found products
                        item {
                            StoreDropdown(
                                selected = vm.offStore,
                                options = vm.offStoreOptions,
                                totalCount = vm.offAllCount,
                                onSelect = { vm.selectStore(it) },
                            )
                        }
                        vm.offError?.let { err -> item { Hint(err) } }
                        vm.offEmptyMessage?.let { msg -> item { Hint(msg) } }
                        items(vm.offResults, key = { "o" + it.key }) { p: Product ->
                            ProductRow(
                                product = p,
                                showPhoto = data.showPhotos,
                                nameColor = data.nameColor,
                                extra = p.stores.takeIf { it.isNotEmpty() }
                                    ?.joinToString(", ", prefix = "🛒 ") { Stores.displayName(it) },
                                onClick = { vm.chooseOff(date, p) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DictRow(e: DictEntry, onSearch: () -> Unit, onPlain: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onSearch)
            .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(e.de, style = MaterialTheme.typography.bodyLarge)
            Text(
                (e.localName?.let { "$it · " } ?: "") + T.tapToChoose(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onPlain) { Text(T.justThis()) }
    }
}

/** "Магазин ▾" with the supermarkets found in the results (and how many products each has). */
@Composable
private fun StoreDropdown(
    selected: String?,
    options: List<Pair<String, Int>>,
    totalCount: Int,
    onSelect: (String?) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(T.storeLabel(), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(8.dp))
        Box {
            OutlinedButton(onClick = { open = true }) {
                Icon(Icons.Filled.Store, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(selected?.let { Stores.displayName(it) } ?: T.allStores())
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                DropdownMenuItem(
                    text = { Text(T.allStores() + if (totalCount > 0) " ($totalCount)" else "") },
                    onClick = {
                        open = false
                        onSelect(null)
                    },
                )
                options.forEach { (slug, count) ->
                    DropdownMenuItem(
                        text = { Text("${Stores.displayName(slug)} ($count)") },
                        trailingIcon = if (slug == selected) {
                            { Icon(Icons.Filled.Check, contentDescription = null) }
                        } else {
                            null
                        },
                        onClick = {
                            open = false
                            onSelect(slug)
                        },
                    )
                }
                if (options.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text(T.noStoreInfo()) },
                        enabled = false,
                        onClick = {},
                    )
                }
            }
        }
    }
}
