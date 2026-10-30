package de.mef.lebensmittel.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import de.mef.lebensmittel.AppViewModel
import de.mef.lebensmittel.Screen

@Composable
fun AppRoot(vm: AppViewModel) {
    val snackbar = remember { SnackbarHostState() }
    val msg = vm.message
    LaunchedEffect(msg?.id) {
        if (msg != null) {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(msg.text, withDismissAction = true, duration = SnackbarDuration.Short)
        }
    }

    BackHandler(enabled = vm.screen != Screen.Calendar) { vm.back() }

    val data by vm.store.data.collectAsState()
    val bgColor = MaterialTheme.colorScheme.background

    Box(
        Modifier
            .fillMaxSize()
            .background(bgColor),
    ) {
        // user's picture from the gallery, covered with a see-through layer so text stays readable
        data.backgroundId?.let { id ->
            AsyncImage(
                model = vm.backgroundFile(id),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(bgColor.copy(alpha = vm.backgroundDimPreview ?: data.backgroundDim)),
            )
        }
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        when (val s = vm.screen) {
            Screen.Calendar -> CalendarScreen(vm)
            is Screen.Day -> DayScreen(vm, s.date)
            is Screen.Add -> AddScreen(vm, s.date)
            Screen.Spending -> SpendingScreen(vm)
            Screen.Settings -> SettingsScreen(vm)
            Screen.Nutrition -> NutritionScreen(vm)
            Screen.Reminders -> RemindersScreen(vm)
        }
        }
        SnackbarHost(
            snackbar,
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 96.dp),
        )
    }

    vm.pending?.let { QuantityDialog(vm, it) }
    vm.pendingImport?.let { ImportDialog(vm, it) }
}
