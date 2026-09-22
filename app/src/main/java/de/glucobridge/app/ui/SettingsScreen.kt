package de.glucobridge.app.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.health.connect.client.PermissionController
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.glucobridge.app.GlucoBridgeApp
import de.glucobridge.app.data.ExportSettings
import de.glucobridge.app.export.ExportManager
import de.glucobridge.core.model.GlucoseUnit
import de.glucobridge.core.model.TargetRange
import de.glucobridge.domain.DataSourceMode
import de.glucobridge.domain.PollInterval
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: GlucoseViewModel, modifier: Modifier = Modifier) {
    var tab by rememberSaveable { mutableStateOf(0) }
    Column(modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Allgemein") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Export") })
        }
        when (tab) {
            0 -> GeneralSettings(vm, Modifier.fillMaxSize())
            else -> ExportSettingsTab(Modifier.fillMaxSize())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeneralSettings(vm: GlucoseViewModel, modifier: Modifier = Modifier) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val allowed = PollInterval.allowed

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        SectionTitle("Aktualisierung")
        Text("Pollingintervall: alle ${settings.pollIntervalMin} min", style = MaterialTheme.typography.bodyLarge)
        val idx = allowed.indexOf(settings.pollIntervalMin).coerceAtLeast(0)
        Slider(
            value = idx.toFloat(),
            onValueChange = { v ->
                val i = v.roundToInt().coerceIn(0, allowed.size - 1)
                vm.updateSettings { it.copy(pollIntervalMin = allowed[i]) }
            },
            valueRange = 0f..(allowed.size - 1).toFloat(),
            steps = (allowed.size - 2).coerceAtLeast(0)
        )
        Text("1–15 min in 1er-Schritten, danach in 5er-Schritten bis 60.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(24.dp))

        SectionTitle("Zielbereich")
        Text(
            "${formatGlucose(settings.target.lowMgDl, settings.unit)} – " +
                "${formatGlucose(settings.target.highMgDl, settings.unit)} ${unitLabel(settings.unit)}",
            style = MaterialTheme.typography.bodyLarge
        )
        RangeSlider(
            value = settings.target.lowMgDl.toFloat()..settings.target.highMgDl.toFloat(),
            onValueChange = { r ->
                val low = r.start.roundToInt()
                val high = r.endInclusive.roundToInt().coerceAtLeast(low + 1)
                vm.updateSettings { it.copy(target = TargetRange(low, high)) }
            },
            valueRange = 40f..300f,
            steps = ((300 - 40) / 5) - 1
        )

        Spacer(Modifier.height(24.dp))

        SectionTitle("Einheit")
        Row {
            FilterChip(
                selected = settings.unit == GlucoseUnit.MG_DL,
                onClick = { vm.updateSettings { it.copy(unit = GlucoseUnit.MG_DL) } },
                label = { Text("mg/dL") }
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = settings.unit == GlucoseUnit.MMOL_L,
                onClick = { vm.updateSettings { it.copy(unit = GlucoseUnit.MMOL_L) } },
                label = { Text("mmol/L") }
            )
        }

        Spacer(Modifier.height(24.dp))

        SectionTitle("Datenquelle")
        Row {
            FilterChip(
                selected = settings.dataSource == DataSourceMode.ALE,
                onClick = { vm.updateSettings { it.copy(dataSource = DataSourceMode.ALE) } },
                label = { Text("ALE (Standard)") }
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = settings.dataSource == DataSourceMode.REST,
                onClick = { vm.updateSettings { it.copy(dataSource = DataSourceMode.REST) } },
                label = { Text("REST") }
            )
        }
        if (!vm.aleAvailable) {
            Spacer(Modifier.height(6.dp))
            Text(
                "ALE-Modul (IP) ist noch nicht eingebunden – aktuell wird immer REST verwendet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(24.dp))

        SectionTitle("Hintergrund")
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Aktualisierung + Benachrichtigung", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = settings.backgroundEnabled, onCheckedChange = { on -> vm.updateSettings { it.copy(backgroundEnabled = on) } })
        }
        Text(
            "Hält den Wert aktuell, auch wenn die App geschlossen ist (dauerhafte Benachrichtigung).",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        Button(onClick = { vm.logout() }, modifier = Modifier.fillMaxWidth()) { Text("Logout") }
        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportSettingsTab(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as GlucoBridgeApp).container }
    val store = remember { container.exportSettingsStore }
    val manager = remember { container.exportManager }
    val scope = rememberCoroutineScope()

    var cfg by remember { mutableStateOf(store.load()) }
    var secret by remember { mutableStateOf(store.nightscoutSecret) }
    var status by remember { mutableStateOf<String?>(null) }

    val hc = remember { container.healthConnectExporter }
    val uploader = remember { container.nightscoutUploader }
    val hcLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        val ok = granted.containsAll(hc.permissions)
        store.save(store.load().copy(healthConnectEnabled = ok))
        cfg = store.load()
        status = if (ok) "Health Connect freigegeben." else "Freigabe abgelehnt."
    }

    fun persist(next: ExportSettings) { cfg = next; store.save(next) }

    fun doExport(fmt: ExportManager.Format) {
        scope.launch {
            val rd = manager.readings(cfg.rangeHours)
            if (rd.isEmpty()) { status = "Kein Verlauf im gewählten Zeitraum."; return@launch }
            val intent = manager.shareIntent(fmt, manager.format(fmt, rd))
            context.startActivity(Intent.createChooser(intent, "Export teilen"))
            status = "${rd.size} Werte als ${fmt.name} exportiert."
        }
    }

    Column(modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
        SectionTitle("Zeitraum für Datei-Export")
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf(6, 12, 24, 72, 168).forEach { h ->
                FilterChip(
                    selected = cfg.rangeHours == h,
                    onClick = { persist(cfg.copy(rangeHours = h)) },
                    label = { Text(rangeLabel(h)) }
                )
                Spacer(Modifier.width(6.dp))
            }
        }

        Spacer(Modifier.height(24.dp))

        SectionTitle("Datei-Export (teilen)")
        Button(onClick = { doExport(ExportManager.Format.CSV) }, modifier = Modifier.fillMaxWidth()) { Text("Als CSV teilen") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { doExport(ExportManager.Format.FHIR) }, modifier = Modifier.fillMaxWidth()) { Text("Als FHIR (JSON) teilen") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { doExport(ExportManager.Format.NIGHTSCOUT) }, modifier = Modifier.fillMaxWidth()) { Text("Als Nightscout-JSON teilen") }
        status?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(24.dp))

        SectionTitle("Health Connect (Google Health)")
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Neue Werte automatisch schreiben", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = cfg.healthConnectEnabled,
                onCheckedChange = { on ->
                    if (on) {
                        if (hc.available()) hcLauncher.launch(hc.permissions)
                        else status = "Health Connect ist auf diesem Gerät nicht verfügbar."
                    } else persist(cfg.copy(healthConnectEnabled = false))
                }
            )
        }
        Text(
            "Stellt Glukosewerte anderen Android-Apps bereit. Beim Einschalten fragt Health Connect die Freigabe ab.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                scope.launch {
                    if (!hc.available()) { status = "Health Connect nicht verfügbar."; return@launch }
                    if (!hc.hasPermission()) { status = "Bitte zuerst oben freigeben."; return@launch }
                    val rd = manager.readings(cfg.rangeHours)
                    if (rd.isEmpty()) { status = "Kein Verlauf im gewählten Zeitraum."; return@launch }
                    runCatching { hc.write(rd) }
                        .onSuccess { status = "$it Werte nach Health Connect geschrieben." }
                        .onFailure { status = "HC-Fehler: ${it.message}" }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Zeitraum nach Health Connect schreiben") }

        Spacer(Modifier.height(24.dp))

        SectionTitle("Nightscout-Upload")
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Automatischer Upload", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = cfg.nightscoutEnabled, onCheckedChange = { persist(cfg.copy(nightscoutEnabled = it)) })
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = cfg.nightscoutUrl,
            onValueChange = { persist(cfg.copy(nightscoutUrl = it)) },
            label = { Text("Nightscout-URL (https://…)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = secret,
            onValueChange = { secret = it; store.nightscoutSecret = it },
            label = { Text("API-Secret") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                scope.launch {
                    if (cfg.nightscoutUrl.isBlank()) { status = "Bitte Nightscout-URL angeben."; return@launch }
                    val rd = manager.readings(cfg.rangeHours)
                    if (rd.isEmpty()) { status = "Kein Verlauf im gewählten Zeitraum."; return@launch }
                    uploader.upload(cfg.nightscoutUrl, secret, rd)
                        .onSuccess { status = "$it Werte zu Nightscout hochgeladen." }
                        .onFailure { status = "Upload-Fehler: ${it.message}" }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Zeitraum jetzt hochladen") }
        Text(
            "URL/Secret werden verschlüsselt gespeichert. Bei aktivem Schalter werden neue Werte automatisch hochgeladen.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
    }
}

private fun rangeLabel(h: Int): String = when {
    h < 24 -> "${h}h"
    h == 24 -> "24h"
    else -> "${h / 24}d"
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
}
