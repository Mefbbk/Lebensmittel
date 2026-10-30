package de.mef.lebensmittel.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import de.mef.lebensmittel.T
import de.mef.lebensmittel.AppViewModel
import de.mef.lebensmittel.Kcal
import de.mef.lebensmittel.Screen
import de.mef.lebensmittel.Product

fun Context.findActivity(): Activity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
fun ProductImage(url: String?, size: Dp = 44.dp) {
    val shape = RoundedCornerShape(8.dp)
    val base = Modifier
        .size(size)
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceVariant)
    if (url == null) {
        Box(base)
    } else {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = base,
        )
    }
}

/** One product in a search result / suggestion list. */
@Composable
fun ProductRow(
    product: Product,
    showPhoto: Boolean,
    extra: String? = null,
    nameColor: Int? = null,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showPhoto) {
            ProductImage(product.imageUrl)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                product.name,
                style = MaterialTheme.typography.bodyLarge,
                color = nameColor?.let { Color(it) } ?: Color.Unspecified,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = listOfNotNull(product.subtitle, extra).joinToString(" · ")
            if (sub.isNotEmpty()) {
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Kcal.per100Label(product)?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        if (trailing != null) trailing()
    }
}

/** Keys of the tabs that can be switched off in the settings. */
object TabKeys {
    const val NUTRITION = "nutrition"
    const val SPENDING = "spending"
    const val REMINDERS = "reminders"
}

/** Bottom tabs shown on the main screens (without the ones switched off in the settings). */
@Composable
fun MainTabs(vm: AppViewModel, current: Screen) {
    val data by vm.store.data.collectAsState()
    val tabs = listOf(
        Triple(Screen.Calendar, Icons.Filled.CalendarMonth, T.tabLists()) to null,
        Triple(Screen.Nutrition, Icons.Filled.QrCodeScanner, T.tabNutrition()) to TabKeys.NUTRITION,
        Triple(Screen.Spending, Icons.Filled.Euro, T.tabSpending()) to TabKeys.SPENDING,
        Triple(Screen.Reminders, Icons.Filled.Notifications, T.tabReminders()) to TabKeys.REMINDERS,
        // Settings are opened from the ⋮ menu on the main screen
    ).filter { (_, key) -> key == null || key !in data.hiddenTabs || current.tabKey() == key }
    NavigationBar {
        tabs.forEach { (tab, _) ->
            val (screen, icon, label) = tab
            NavigationBarItem(
                selected = current == screen,
                onClick = { vm.openTab(screen) },
                icon = { Icon(icon, contentDescription = null) },
                label = { AutoSizeText(label) },
            )
        }
    }
}

private fun Screen.tabKey(): String? = when (this) {
    Screen.Nutrition -> TabKeys.NUTRITION
    Screen.Spending -> TabKeys.SPENDING
    Screen.Reminders -> TabKeys.REMINDERS
    else -> null
}

/**
 * One-line text that gets smaller until it fits (used for the bottom tab labels,
 * so long words like "Einstellungen" or "Нагадування" are not cut off).
 */
@Composable
fun AutoSizeText(text: String, maxSizeSp: Float = 11f, minSizeSp: Float = 7f) {
    var sizeSp by remember(text) { mutableStateOf(maxSizeSp) }
    var fits by remember(text) { mutableStateOf(false) }
    Text(
        text,
        fontSize = sizeSp.sp,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        // don't draw the too-big first tries, only the final size
        modifier = Modifier.drawWithContent { if (fits) drawContent() },
        onTextLayout = { result ->
            if (result.didOverflowWidth && sizeSp > minSizeSp) {
                sizeSp = (sizeSp - 0.5f).coerceAtLeast(minSizeSp)
            } else {
                fits = true
            }
        },
    )
}

/** ⚙ in the top bar of the main tabs: opens the settings (back returns to the tab). */
@Composable
fun SettingsAction(vm: AppViewModel) {
    IconButton(onClick = { vm.open(Screen.Settings) }) {
        Icon(Icons.Filled.Settings, contentDescription = T.tabSettings())
    }
}
