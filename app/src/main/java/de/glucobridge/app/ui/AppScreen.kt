package de.glucobridge.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import de.glucobridge.app.service.GlucoseMonitorService
import de.glucobridge.domain.GlucoseState

/** Wurzel-Screen: Hauptansicht + Einstellungen, umgeschaltet ueber die TopAppBar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(vm: GlucoseViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showSettings by rememberSaveable { mutableStateOf(false) }

    // Nur ein ECHTER Logout fuehrt zurueck zum Login. Transiente Loading/Error-Zustaende
    // (z. B. waehrend eines Abrufs) lassen die Settings-Seite unberuehrt.
    val loggedOut = state is GlucoseState.LoggedOut
    BackgroundServiceController(vm)
    LaunchedEffect(loggedOut) { if (loggedOut) showSettings = false }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (!loggedOut) {
                TopAppBar(
                    title = { Text(if (showSettings) "Einstellungen" else "GlucoBridge") },
                    navigationIcon = {
                        if (showSettings) IconButton(onClick = { showSettings = false }) { Text("←", fontSize = 20.sp) }
                    },
                    actions = {
                        if (!showSettings) IconButton(onClick = { showSettings = true }) { Text("⚙", fontSize = 20.sp) }
                    }
                )
            }
        }
    ) { padding ->
        if (showSettings && !loggedOut) {
            SettingsScreen(vm, Modifier.padding(padding))
        } else {
            GlucoseScreen(vm, Modifier.padding(padding))
        }
    }
}

@Composable
private fun BackgroundServiceController(vm: GlucoseViewModel) {
    val ctx = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    val wanted = settings.backgroundEnabled && state !is GlucoseState.LoggedOut

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) startMonitor(ctx) }

    LaunchedEffect(wanted) {
        if (wanted) {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                startMonitor(ctx)
            }
        } else {
            stopMonitor(ctx)
        }
    }
}

private fun startMonitor(ctx: Context) {
    ContextCompat.startForegroundService(ctx, Intent(ctx, GlucoseMonitorService::class.java))
}

private fun stopMonitor(ctx: Context) {
    ctx.stopService(Intent(ctx, GlucoseMonitorService::class.java))
}
