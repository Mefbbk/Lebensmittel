package de.mef.lebensmittel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import de.mef.lebensmittel.T
import de.mef.lebensmittel.AppViewModel
import de.mef.lebensmittel.Nutrient
import de.mef.lebensmittel.NutritionInfo
import de.mef.lebensmittel.Screen
import de.mef.lebensmittel.formatNumber

/** Tab "Склад": scan a barcode and see everything the database knows about the product. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NutritionScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val result = vm.nutritionResult

    Scaffold(
        containerColor = Color.Transparent,
        // transparent screens: text and icons must still follow the theme (light text in dark mode)
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                title = { Text(T.nutritionTitle()) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        bottomBar = { MainTabs(vm, Screen.Nutrition) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Button(
                onClick = { context.findActivity()?.let { vm.scanForNutrition(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(T.scanBarcode(), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = vm.nutritionCode,
                onValueChange = { vm.nutritionCode = it.filter { c -> c.isDigit() }.take(14) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(T.enterBarcodeManually()) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboard?.hide()
                    vm.lookupNutrition(vm.nutritionCode)
                }),
                trailingIcon = {
                    IconButton(onClick = {
                        keyboard?.hide()
                        vm.lookupNutrition(vm.nutritionCode)
                    }) { Icon(Icons.Filled.Search, contentDescription = T.find()) }
                },
            )

            if (vm.nutritionLoading) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
            vm.nutritionError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 12.dp))
            }

            if (result != null) {
                Spacer(Modifier.height(12.dp))
                NutritionCard(result, onAdd = { vm.addNutritionProductToday() })
            } else if (!vm.nutritionLoading && vm.nutritionError == null) {
                Text(
                    T.nutritionIntro(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }

            val history = vm.nutritionHistory.filter { it.product.key != result?.product?.key }
            if (history.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(T.recentlyChecked(), style = MaterialTheme.typography.titleSmall)
                history.forEach { h ->
                    Text(
                        h.product.name + (h.product.brand?.let { " · $it" } ?: ""),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.showNutrition(h) }
                            .padding(vertical = 8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NutritionCard(info: NutritionInfo, onAdd: () -> Unit) {
    val p = info.product
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (info.imageUrl != null) {
                    AsyncImage(
                        model = info.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White),
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    p.subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    p.barcode?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // scores
            if (info.nutriScore != null || info.novaGroup != null || info.ecoScore != null) {
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    info.nutriScore?.let { ScoreBadge("Nutri-Score", it.uppercase(), gradeColor(it)) }
                    info.novaGroup?.let { ScoreBadge("NOVA", it.toString(), novaColor(it)) }
                    info.ecoScore?.let { ScoreBadge("Eco-Score", it.uppercase(), gradeColor(it)) }
                }
                info.novaGroup?.let {
                    Text(
                        "NOVA $it: " + when (it) {
                            1 -> T.nova1()
                            2 -> T.nova2()
                            3 -> T.nova3()
                            else -> T.nova4()
                        },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            if (info.labels.isNotEmpty()) {
                Text(info.labels.joinToString(" · ") { T.labelName(it) }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }

            // nutrition table
            Spacer(Modifier.height(14.dp))
            if (info.nutrients.isEmpty()) {
                Text(
                    T.noNutrition(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val hasServing = info.nutrients.any { it.perServing != null }
                Row {
                    Text(T.nutritionFacts(), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text(
                        "100 " + if (info.isLiquid) T.unitMl() else T.unitG(),
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(84.dp),
                    )
                    if (hasServing) {
                        Text(
                            T.serving() + (info.servingSize?.let { "\n$it" } ?: ""),
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(84.dp),
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                info.nutrients.forEach { NutrientRow(it, hasServing) }
            }

            // ingredients, allergens, additives
            info.ingredients?.let {
                Spacer(Modifier.height(14.dp))
                Text(T.ingredients(), style = MaterialTheme.typography.titleSmall)
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            if (info.allergens.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(T.allergens(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                Text(info.allergens.joinToString(", ") { T.allergenName(it) }, style = MaterialTheme.typography.bodyMedium)
            }
            if (info.traces.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(T.mayContain(), style = MaterialTheme.typography.titleSmall)
                Text(info.traces.joinToString(", ") { T.allergenName(it) }, style = MaterialTheme.typography.bodySmall)
            }
            if (info.additives.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(T.additives(), style = MaterialTheme.typography.titleSmall)
                Text(info.additives.joinToString(", "), style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(T.addToToday())
            }
            Text(
                T.dataNote(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun NutrientRow(n: Nutrient, hasServing: Boolean) {
    val main = n.indent == 0
    Row(Modifier.padding(vertical = 3.dp)) {
        Text(
            n.label,
            style = if (main) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            fontWeight = if (main && n.key in setOf("energy-kcal", "fat", "carbohydrates", "proteins")) FontWeight.SemiBold else null,
            modifier = Modifier
                .weight(1f)
                .padding(start = if (main) 0.dp else 16.dp),
        )
        Text(
            n.per100?.let { "${fmt(it)} ${n.unitLabel}" } ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.width(84.dp),
        )
        if (hasServing) {
            Text(
                n.perServing?.let { "${fmt(it)} ${n.unitLabel}" } ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End,
                modifier = Modifier.width(84.dp),
            )
        }
    }
}

private fun fmt(v: Double): String = when {
    v >= 100 -> formatNumber(v, 0)
    v >= 10 -> formatNumber(v, 1)
    v >= 0.1 -> formatNumber(v, 2)
    else -> formatNumber(v, 3)
}

@Composable
private fun ScoreBadge(title: String, value: String, color: Color) {
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = Color.White, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.width(6.dp))
        Text(value, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

private fun gradeColor(g: String): Color = when (g) {
    "a" -> Color(0xFF038141)
    "b" -> Color(0xFF85BB2F)
    "c" -> Color(0xFFE5A800)
    "d" -> Color(0xFFEE8100)
    else -> Color(0xFFE63E11)
}

private fun novaColor(n: Int): Color = when (n) {
    1 -> Color(0xFF00AA00)
    2 -> Color(0xFFE5A800)
    3 -> Color(0xFFEE8100)
    else -> Color(0xFFE63E11)
}
