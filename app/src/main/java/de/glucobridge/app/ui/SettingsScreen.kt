package de.glucobridge.app.ui

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
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.glucobridge.core.model.GlucoseUnit
import de.glucobridge.core.model.TargetRange
import de.glucobridge.domain.DataSourceMode
import de.glucobridge.domain.PollInterval
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: GlucoseViewModel, modifier: Modifier = Modifier) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val allowed = PollInterval.allowed

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        // --- Aktualisierung ---
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

        // --- Zielbereich ---
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

        // --- Einheit ---
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

        // --- Datenquelle ---
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

        // --- Hintergrund ---
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

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
}
