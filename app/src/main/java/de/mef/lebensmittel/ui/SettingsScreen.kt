package de.mef.lebensmittel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import de.mef.lebensmittel.Lang
import de.mef.lebensmittel.T
import de.mef.lebensmittel.AppViewModel
import de.mef.lebensmittel.Screen
import de.mef.lebensmittel.parseHexColor
import de.mef.lebensmittel.toHexColor

private val swatches = listOf(
    0xFF1A1C19, // standard dark
    0xFF000000,
    0xFF2E6B3F, // green
    0xFF1565C0, // blue
    0xFF6A1B9A, // purple
    0xFFC62828, // red
    0xFFE65100, // orange
    0xFF5D4037, // brown
    0xFF00838F, // teal
    0xFFAD1457, // pink
    0xFF455A64, // grey
    0xFFFFFFFF, // white (for dark backgrounds)
).map { it.toInt() }

private fun argb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: AppViewModel) {
    val data by vm.store.data.collectAsState()
    var showBackground by remember { mutableStateOf(false) }

    // current name colour, edited live with the sliders and saved when a slider is released
    val stored = data.nameColor
    val themeText = MaterialTheme.colorScheme.onBackground.toArgb()
    val start = stored ?: themeText
    var r by remember(stored, themeText) { mutableStateOf((start shr 16) and 0xFF) }
    var g by remember(stored, themeText) { mutableStateOf((start shr 8) and 0xFF) }
    var b by remember(stored, themeText) { mutableStateOf(start and 0xFF) }
    var hexText by remember(stored, themeText) { mutableStateOf(toHexColor(start)) }
    val current = argb(r, g, b)

    fun save() {
        vm.store.setNameColor(argb(r, g, b))
    }

    Scaffold(
        containerColor = Color.Transparent,
        // transparent screens: text and icons must still follow the theme (light text in dark mode)
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = { MainTabs(vm, Screen.Settings) },
        topBar = {
            TopAppBar(
                title = { Text(T.tabSettings()) },
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
            // ---------- appearance ----------
            SettingsTitle(T.language())
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                (listOf("system" to T.languageSystem()) + Lang.entries.map { it.code to it.nativeName }).forEach { (code, label) ->
                    FilterChip(
                        selected = data.language == code,
                        onClick = { vm.store.setLanguage(code) },
                        label = { Text(label) },
                    )
                }
            }

            SettingsTitle(T.appearance())
            Text(T.theme(), style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("light" to T.themeLight(), "dark" to T.themeDark(), "system" to T.themeSystem()).forEach { (mode, label) ->
                    FilterChip(
                        selected = data.themeMode == mode,
                        onClick = { vm.store.setThemeMode(mode) },
                        label = { Text(label) },
                    )
                }
            }
            if (data.backgroundId != null) {
                Text(
                    T.bgAutoTextHint(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { showBackground = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Wallpaper, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (data.backgroundId == null) T.bgPick() else T.bgChange())
            }

            // ---------- product name colour ----------
            SettingsTitle(T.nameColor())
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Vollkornbrot", style = MaterialTheme.typography.bodyLarge, color = Color(current))
                    Text("Milch 3,5 %", style = MaterialTheme.typography.bodyLarge, color = Color(current))
                    Text("Äpfel", style = MaterialTheme.typography.bodyLarge, color = Color(current))
                }
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                swatches.forEach { c ->
                    val selected = stored != null && (stored and 0xFFFFFF) == (c and 0xFFFFFF)
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(c))
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = CircleShape,
                            )
                            .clickable { vm.store.setNameColor(c) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            ColorSlider("R", r, Color(0xFFE53935), onFinished = { save() }) { r = it }
            ColorSlider("G", g, Color(0xFF43A047), onFinished = { save() }) { g = it }
            ColorSlider("B", b, Color(0xFF1E88E5), onFinished = { save() }) { b = it }
            // sliders change the preview live; the colour is saved when the finger is lifted
            LaunchedEffect(r, g, b) {
                hexText = toHexColor(argb(r, g, b))
            }
            OutlinedTextField(
                    value = hexText,
                    onValueChange = { t ->
                        hexText = t
                        parseHexColor(t)?.let { vm.store.setNameColor(it) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(T.colorCode()) },
                    isError = parseHexColor(hexText) == null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                )
            TextButton(onClick = { vm.store.setNameColor(null) }, enabled = stored != null) {
                Text(T.defaultColor())
            }

            // ---------- tabs ----------
            SettingsTitle(T.tabsSection())
            SwitchRow(T.tabNutrition(), TabKeys.NUTRITION !in data.hiddenTabs) { vm.store.setTabHidden(TabKeys.NUTRITION, !it) }
            SwitchRow(T.tabSpending(), TabKeys.SPENDING !in data.hiddenTabs) { vm.store.setTabHidden(TabKeys.SPENDING, !it) }
            SwitchRow(T.tabReminders(), TabKeys.REMINDERS !in data.hiddenTabs) { vm.store.setTabHidden(TabKeys.REMINDERS, !it) }
            Text(
                T.tabsHint(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ---------- what to show in the list ----------
            SettingsTitle(T.whatToShow())
            SwitchRow(T.showPhotos(), data.showPhotos) { vm.store.setShowPhotos(it) }
            SwitchRow(T.showBrand(), data.showBrand) { vm.store.setShowBrand(it) }
            SwitchRow(T.showKcal(), data.showKcal) { vm.store.setShowKcal(it) }
            Text(
                T.settingsListHint(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showBackground) BackgroundDialog(vm) { showBackground = false }
}

@Composable
private fun SettingsTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ColorSlider(label: String, value: Int, tint: Color, onFinished: () -> Unit, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(20.dp), style = MaterialTheme.typography.labelLarge)
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt().coerceIn(0, 255)) },
            valueRange = 0f..255f,
            onValueChangeFinished = onFinished,
            colors = SliderDefaults.colors(thumbColor = tint, activeTrackColor = tint),
            modifier = Modifier.weight(1f),
        )
        Text(value.toString(), modifier = Modifier.width(36.dp), style = MaterialTheme.typography.bodySmall)
    }
}
