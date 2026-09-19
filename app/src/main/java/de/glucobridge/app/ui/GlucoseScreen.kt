package de.glucobridge.app.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.glucobridge.core.model.GlucoseReading
import de.glucobridge.core.model.GlucoseUnit
import de.glucobridge.core.model.TargetRange
import de.glucobridge.core.model.Trend
import de.glucobridge.core.model.Zone
import de.glucobridge.domain.GlucoseState
import de.glucobridge.domain.Settings
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun GlucoseScreen(vm: GlucoseViewModel, modifier: Modifier = Modifier) {
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    when (val s = state) {
        GlucoseState.LoggedOut -> CenteredForm(modifier) { LoginForm(vm, null) }
        is GlucoseState.Error -> CenteredForm(modifier) { LoginForm(vm, s.message) }
        GlucoseState.Loading -> CenteredForm(modifier) { CircularProgressIndicator() }
        is GlucoseState.Content -> ValueView(s, settings, vm, modifier)
    }
}

@Composable
private fun CenteredForm(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) { content() }
}

@Composable
private fun LoginForm(vm: GlucoseViewModel, errorMessage: String?) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Text("Anmelden", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(20.dp))
    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("E-Mail") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            autoCorrectEnabled = false
        ),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Passwort") },
        singleLine = true,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            autoCorrectEnabled = false
        ),
        trailingIcon = {
            TextButton(onClick = { passwordVisible = !passwordVisible }) {
                Text(if (passwordVisible) "Verbergen" else "Zeigen")
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
    if (errorMessage != null) {
        Spacer(Modifier.height(12.dp))
        Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
    Spacer(Modifier.height(20.dp))
    Button(
        onClick = { vm.login(email, password) },
        enabled = email.isNotBlank() && password.isNotBlank(),
        modifier = Modifier.fillMaxWidth()
    ) { Text("Login") }
    Spacer(Modifier.height(8.dp))
    Text(
        "Eigene LibreLinkUp-Zugangsdaten. Kein medizinisches Produkt.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ValueView(s: GlucoseState.Content, settings: Settings, vm: GlucoseViewModel, modifier: Modifier) {
    val zone = settings.target.zoneFor(s.reading.valueMgDl)
    val color = zoneColor(zone)

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(formatGlucose(s.reading.valueMgDl, settings.unit), fontSize = 64.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(Modifier.width(8.dp))
            Text(trendGlyph(s.reading.trend), fontSize = 44.sp, color = color)
        }
        Text("${unitLabel(settings.unit)} · ${ageLabel(s.reading.timestampEpochMillis)}", style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(20.dp))
        GlucoseChart(
            history = s.history,
            target = settings.target,
            unit = settings.unit,
            modifier = Modifier.fillMaxWidth().height(230.dp)
        )

        Spacer(Modifier.height(20.dp))
        Button(onClick = { vm.refresh() }) { Text("Aktualisieren") }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun GlucoseChart(
    history: List<GlucoseReading>,
    target: TargetRange,
    unit: GlucoseUnit,
    modifier: Modifier = Modifier
) {
    val pts = remember(history) { history.sortedBy { it.timestampEpochMillis } }

    if (pts.size < 2) {
        Box(modifier) {
            Text(
                "Noch kein Verlauf",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        return
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var cursorIndex by remember { mutableStateOf<Int?>(null) }

    // Plot-Metriken aus dem letzten Draw (fuer Gesten/Buttons: Clamping + Tap->Index)
    var plotLeftPx by remember { mutableFloatStateOf(46f) }
    var plotWpx by remember { mutableFloatStateOf(0f) }
    var contentWpx by remember { mutableFloatStateOf(0f) }
    var offPx by remember { mutableFloatStateOf(0f) }
    var minOffsetPx by remember { mutableFloatStateOf(0f) }

    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val lineColor = MaterialTheme.colorScheme.onSurface
    val axisArgb = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val boxBg = MaterialTheme.colorScheme.inverseSurface
    val boxTextArgb = MaterialTheme.colorScheme.inverseOnSurface.toArgb()

    val values = pts.map { it.valueMgDl }
    val minV = (minOf(values.min(), target.lowMgDl) - 15).toFloat()
    val maxV = (maxOf(values.max(), target.highMgDl) + 15).toFloat()
    val span = (maxV - minV).coerceAtLeast(1f)
    val t0 = pts.first().timestampEpochMillis
    val t1 = pts.last().timestampEpochMillis

    Column(modifier) {
        Canvas(
            modifier = Modifier.fillMaxWidth().weight(1f).pointerInput(pts) {
                val slop = viewConfiguration.touchSlop
                val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                val doubleTapTimeout = viewConfiguration.doubleTapTimeoutMillis

                fun idxFromX(x: Float): Int {
                    if (contentWpx <= 0f) return 0
                    val frac = ((x - plotLeftPx - offPx) / contentWpx).coerceIn(0f, 1f)
                    return (frac * (pts.size - 1)).roundToInt().coerceIn(0, pts.size - 1)
                }

                awaitEachGesture {
                    val down1 = awaitFirstDown(requireUnconsumed = false)

                    // Klassifikation der ersten Beruehrung: UP / SECOND / MOVED / (Timeout=LongPress)
                    val outcome = withTimeoutOrNull(longPressTimeout) {
                        while (true) {
                            val e = awaitPointerEvent()
                            val pressed = e.changes.filter { it.pressed }
                            if (pressed.size >= 2) return@withTimeoutOrNull "SECOND"
                            val c = e.changes.firstOrNull { it.id == down1.id }
                            if (c != null && !c.pressed) return@withTimeoutOrNull "UP"
                            if (c != null &&
                                (abs(c.position.x - down1.position.x) > slop ||
                                    abs(c.position.y - down1.position.y) > slop)
                            ) return@withTimeoutOrNull "MOVED"
                        }
                        @Suppress("UNREACHABLE_CODE") "UP"
                    }

                    when (outcome) {
                        null -> {
                            // LONG-PRESS -> Crosshair scrubben (ziehen bewegt den Cursor)
                            cursorIndex = idxFromX(down1.position.x)
                            while (true) {
                                val e = awaitPointerEvent()
                                val pressed = e.changes.filter { it.pressed }
                                if (pressed.isEmpty()) break
                                if (pressed.size >= 2) {
                                    val zoom = e.calculateZoom()
                                    if (zoom != 1f) scale = (scale * zoom).coerceIn(1f, 10f)
                                    offsetX = (offsetX + e.calculatePan().x).coerceIn(minOffsetPx, 0f)
                                    e.changes.forEach { it.consume() }
                                } else {
                                    val c = pressed.first()
                                    cursorIndex = idxFromX(c.position.x)
                                    c.consume()
                                }
                            }
                        }
                        "SECOND" -> {
                            // Zwei Finger -> Zoom + Pan
                            while (true) {
                                val e = awaitPointerEvent()
                                val pressed = e.changes.filter { it.pressed }
                                if (pressed.isEmpty()) break
                                if (pressed.size >= 2) {
                                    val zoom = e.calculateZoom()
                                    if (zoom != 1f) scale = (scale * zoom).coerceIn(1f, 10f)
                                    offsetX = (offsetX + e.calculatePan().x).coerceIn(minOffsetPx, 0f)
                                    e.changes.forEach { it.consume() }
                                } else {
                                    pressed.first().consume()
                                }
                            }
                        }
                        "MOVED" -> {
                            // Undefinierte Einzel-Wisch-Geste -> ignorieren (Seiten-Scroll darf greifen)
                        }
                        else -> {
                            // "UP": evtl. Doppeltipp-und-halten, sonst Einzeltipp
                            val second = withTimeoutOrNull(doubleTapTimeout) {
                                awaitFirstDown(requireUnconsumed = false)
                            }
                            if (second == null) {
                                // Einzeltipp -> Crosshair setzen/umschalten
                                val idx = idxFromX(down1.position.x)
                                cursorIndex = if (cursorIndex == idx) null else idx
                            } else {
                                // Doppeltipp-und-halten -> horizontal verschieben (nur X)
                                var lastX = second.position.x
                                while (true) {
                                    val e = awaitPointerEvent()
                                    val pressed = e.changes.filter { it.pressed }
                                    if (pressed.isEmpty()) break
                                    if (pressed.size >= 2) {
                                        val zoom = e.calculateZoom()
                                        if (zoom != 1f) scale = (scale * zoom).coerceIn(1f, 10f)
                                        offsetX = (offsetX + e.calculatePan().x).coerceIn(minOffsetPx, 0f)
                                        e.changes.forEach { it.consume() }
                                        lastX = pressed.first().position.x
                                    } else {
                                        val c = pressed.first()
                                        offsetX = (offsetX + (c.position.x - lastX)).coerceIn(minOffsetPx, 0f)
                                        c.consume()
                                        lastX = c.position.x
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) {
            val w = size.width
            val h = size.height
            val mLeft = 46f
            val mRight = 10f
            val mTop = 10f
            val mBottom = 26f
            val plotLeft = mLeft
            val plotTop = mTop
            val plotRight = w - mRight
            val plotBottom = h - mBottom
            val plotW = plotRight - plotLeft
            val plotH = plotBottom - plotTop

            val contentW = plotW * scale
            val minOff = (plotW - contentW).coerceAtMost(0f)
            val off = offsetX.coerceIn(minOff, 0f)

            // Metriken zuruckschreiben
            plotLeftPx = plotLeft
            plotWpx = plotW
            contentWpx = contentW
            offPx = off
            minOffsetPx = minOff

            fun sx(frac: Float): Float = plotLeft + off + frac * contentW
            fun sy(v: Float): Float = plotTop + plotH * (1f - (v - minV) / span)
            fun fracOf(i: Int): Float = if (pts.size == 1) 0f else i.toFloat() / (pts.size - 1)
            fun timeAtScreenX(px: Float): Long {
                val fracData = ((px - plotLeft - off) / contentW).coerceIn(0f, 1f)
                return t0 + (fracData * (t1 - t0)).toLong()
            }

            // Y-Grid + Labels
            val yLabelPaint = Paint().apply {
                isAntiAlias = true; color = axisArgb; textSize = 10.sp.toPx()
                textAlign = Paint.Align.RIGHT
            }
            for (tick in niceTicks(minV, maxV, 5)) {
                val y = sy(tick.toFloat())
                if (y < plotTop - 1 || y > plotBottom + 1) continue
                drawLine(gridColor, Offset(plotLeft, y), Offset(plotRight, y), 1f)
                drawContext.canvas.nativeCanvas.drawText(formatGlucose(tick, unit), plotLeft - 6f, y + 3.5f, yLabelPaint)
            }

            // Zielbereich-Band
            val bandTop = sy(target.highMgDl.toFloat())
            val bandBottom = sy(target.lowMgDl.toFloat())
            drawRect(
                color = Color(0x332E7D32),
                topLeft = Offset(plotLeft, bandTop),
                size = Size(plotW, (bandBottom - bandTop).coerceAtLeast(0f))
            )

            // X-Grid + Zeit-Labels
            val xLabelPaint = Paint().apply {
                isAntiAlias = true; color = axisArgb; textSize = 10.sp.toPx()
                textAlign = Paint.Align.CENTER
            }
            for (k in 0..3) {
                val px = plotLeft + plotW * k / 3f
                drawLine(gridColor, Offset(px, plotTop), Offset(px, plotBottom), 1f)
                drawContext.canvas.nativeCanvas.drawText(fmtTime(timeAtScreenX(px)), px, plotBottom + 18f, xLabelPaint)
            }

            // Verlaufslinie (zonen-eingefaerbt), beschnitten
            clipRect(plotLeft, plotTop, plotRight, plotBottom) {
                for (i in 1 until pts.size) {
                    drawLine(
                        color = zoneColor(target.zoneFor(values[i])).copy(alpha = 0.9f),
                        start = Offset(sx(fracOf(i - 1)), sy(values[i - 1].toFloat())),
                        end = Offset(sx(fracOf(i)), sy(values[i].toFloat())),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Achsenrahmen
            drawLine(gridColor, Offset(plotLeft, plotTop), Offset(plotLeft, plotBottom), 1.5f)
            drawLine(gridColor, Offset(plotLeft, plotBottom), Offset(plotRight, plotBottom), 1.5f)

            // Crosshair (an Datenpunkt gebunden)
            val ci = cursorIndex
            if (ci != null && ci in pts.indices) {
                val p = pts[ci]
                val pxp = sx(fracOf(ci))
                val pyp = sy(p.valueMgDl.toFloat())
                if (pxp in plotLeft..plotRight) {
                    drawLine(lineColor.copy(alpha = 0.5f), Offset(pxp, plotTop), Offset(pxp, plotBottom), 1.5f)
                    drawLine(lineColor.copy(alpha = 0.5f), Offset(plotLeft, pyp), Offset(plotRight, pyp), 1.5f)
                    drawCircle(zoneColor(target.zoneFor(p.valueMgDl)), 7f, Offset(pxp, pyp))
                    drawCircle(Color.White, 3f, Offset(pxp, pyp))

                    val line1 = "${formatGlucose(p.valueMgDl, unit)} ${unitLabel(unit)}"
                    val line2 = fmtTime(p.timestampEpochMillis)
                    val boxPaint = Paint().apply {
                        isAntiAlias = true; color = boxTextArgb; textSize = 12.sp.toPx()
                    }
                    val tw = maxOf(boxPaint.measureText(line1), boxPaint.measureText(line2))
                    val padH = 8f; val padV = 6f; val lineGap = 4f
                    val lineH = boxPaint.textSize
                    val boxW = tw + padH * 2
                    val boxH = lineH * 2 + lineGap + padV * 2
                    var boxX = pxp + 10f
                    if (boxX + boxW > plotRight) boxX = pxp - 10f - boxW
                    if (boxX < plotLeft) boxX = plotLeft + 2f
                    val boxY = plotTop + 2f
                    drawRoundRect(
                        color = boxBg.copy(alpha = 0.92f),
                        topLeft = Offset(boxX, boxY),
                        size = Size(boxW, boxH),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    drawContext.canvas.nativeCanvas.drawText(line1, boxX + padH, boxY + padV + lineH - 2f, boxPaint)
                    drawContext.canvas.nativeCanvas.drawText(line2, boxX + padH, boxY + padV + lineH * 2 + lineGap - 2f, boxPaint)
                }
            }
        }

        // Bedienleiste
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = {
                val old = scale
                val nw = (old / 1.5f).coerceIn(1f, 10f)
                val mid = plotWpx / 2f
                offsetX = (mid - (mid - offsetX) * (nw / old)).coerceIn(plotWpx - plotWpx * nw, 0f)
                scale = nw
            }) { Text("−") }
            OutlinedButton(onClick = {
                val old = scale
                val nw = (old * 1.5f).coerceIn(1f, 10f)
                val mid = plotWpx / 2f
                offsetX = (mid - (mid - offsetX) * (nw / old)).coerceIn(plotWpx - plotWpx * nw, 0f)
                scale = nw
            }) { Text("+") }
            OutlinedButton(onClick = {
                scale = 1f; offsetX = 0f; cursorIndex = null
            }) { Text("Reset") }
            Text(
                "${"%.1f".format(scale)}× · halten = Cursor · Doppeltipp+halten = verschieben",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun zoneColor(zone: Zone): Color = when (zone) {
    Zone.LOW -> Color(0xFFD32F2F)
    Zone.HIGH -> Color(0xFFF9A825)
    Zone.IN_RANGE -> Color(0xFF2E7D32)
}

private fun trendGlyph(t: Trend): String = when (t) {
    Trend.RISING_QUICK -> "↑"
    Trend.RISING -> "↗"
    Trend.STABLE -> "→"
    Trend.FALLING -> "↘"
    Trend.FALLING_QUICK -> "↓"
    Trend.UNKNOWN -> "–"
}

private val HM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private fun fmtTime(ms: Long): String =
    Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(HM)

private fun ageLabel(timestampEpochMillis: Long): String {
    val diffMin = (System.currentTimeMillis() - timestampEpochMillis) / 60_000L
    return when {
        diffMin < 1 -> "gerade eben"
        diffMin == 1L -> "vor 1 min"
        diffMin < 60 -> "vor $diffMin min"
        else -> "vor ${diffMin / 60} h ${diffMin % 60} min"
    }
}

private fun niceTicks(minV: Float, maxV: Float, count: Int): List<Int> {
    val range = (maxV - minV).coerceAtLeast(1f)
    val rawStep = range / count
    val step = listOf(10, 20, 25, 50, 100).firstOrNull { it >= rawStep } ?: 100
    val start = ceil(minV / step).toInt() * step
    val out = ArrayList<Int>()
    var v = start
    while (v <= maxV) { out.add(v); v += step }
    return out
}
