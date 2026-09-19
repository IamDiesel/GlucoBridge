package de.glucobridge.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import de.glucobridge.core.model.GlucoseUnit
import de.glucobridge.core.model.TargetRange
import de.glucobridge.core.model.Trend
import de.glucobridge.core.model.Zone

private const val PATH = "/glucose"

class GlucoseData(
    val value: Int, val trend: String, val ts: Long,
    val targetLow: Int, val targetHigh: Int, val unit: String
)

class MainActivity : ComponentActivity(), DataClient.OnDataChangedListener {

    private var glucose by mutableStateOf<GlucoseData?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WearApp(glucose) }
    }

    override fun onResume() {
        super.onResume()
        val dc = Wearable.getDataClient(this)
        dc.addListener(this)
        dc.dataItems.addOnSuccessListener { buffer ->
            for (item in buffer) {
                if (item.uri.path == PATH) glucose = parse(DataMapItem.fromDataItem(item).dataMap)
            }
            buffer.release()
        }
    }

    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(this)
    }

    override fun onDataChanged(events: DataEventBuffer) {
        for (e in events) {
            if (e.type == DataEvent.TYPE_CHANGED && e.dataItem.uri.path == PATH) {
                glucose = parse(DataMapItem.fromDataItem(e.dataItem).dataMap)
            }
        }
    }

    private fun parse(m: DataMap): GlucoseData = GlucoseData(
        value = m.getInt("value"),
        trend = m.getString("trend") ?: "UNKNOWN",
        ts = m.getLong("ts"),
        targetLow = m.getInt("targetLow", 70),
        targetHigh = m.getInt("targetHigh", 180),
        unit = m.getString("unit") ?: "MG_DL"
    )
}

@Composable
private fun WearApp(data: GlucoseData?) {
    MaterialTheme {
        Scaffold(timeText = { TimeText() }) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (data == null) {
                    Text("Warte auf Daten…", color = Color.White, fontSize = 15.sp)
                } else {
                    val zone = TargetRange(data.targetLow, data.targetHigh).zoneFor(data.value)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${formatVal(data.value, data.unit)} ${arrow(data.trend)}",
                            color = zoneColor(zone),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${unitLabel(data.unit)} · ${age(data.ts)}",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatVal(mgdl: Int, unit: String): String =
    if (unit == GlucoseUnit.MMOL_L.name) String.format("%.1f", mgdl / 18.0182) else mgdl.toString()

private fun unitLabel(unit: String): String =
    if (unit == GlucoseUnit.MMOL_L.name) "mmol/L" else "mg/dL"

private fun arrow(trend: String): String = when (runCatching { Trend.valueOf(trend) }.getOrDefault(Trend.UNKNOWN)) {
    Trend.RISING_QUICK -> "↑"
    Trend.RISING -> "↗"
    Trend.STABLE -> "→"
    Trend.FALLING -> "↘"
    Trend.FALLING_QUICK -> "↓"
    Trend.UNKNOWN -> ""
}

private fun zoneColor(zone: Zone): Color = when (zone) {
    Zone.LOW -> Color(0xFFEF5350)
    Zone.HIGH -> Color(0xFFFFCA28)
    Zone.IN_RANGE -> Color(0xFF66BB6A)
}

private fun age(ts: Long): String {
    val m = (System.currentTimeMillis() - ts) / 60_000L
    return when {
        m < 1 -> "gerade eben"
        m == 1L -> "vor 1 min"
        m < 60 -> "vor $m min"
        else -> "vor ${m / 60} h"
    }
}
