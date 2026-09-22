package de.glucobridge.wear

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt

/**
 * Schlanke, glanceable Verlaufskurve fuer die Uhr: Zielbereichs-Band + Linie + letzter Punkt.
 * Tippen/Ziehen setzt einen Cursor (onCursor liefert den Index; null = keiner).
 * `values` chronologisch (alt -> neu); X-Achse index-basiert.
 */
@Composable
fun GlucoseSparkline(
    values: IntArray,
    targetLow: Int,
    targetHigh: Int,
    lineColor: Color,
    cursorIndex: Int?,
    onCursor: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (values.size < 2) return
    Canvas(
        modifier = modifier.pointerInput(values.size) {
            awaitEachGesture {
                fun idx(x: Float): Int {
                    val wpx = size.width.toFloat()
                    if (wpx <= 0f) return 0
                    return (x / wpx * (values.size - 1)).roundToInt().coerceIn(0, values.size - 1)
                }
                val down = awaitFirstDown()
                onCursor(idx(down.position.x))
                while (true) {
                    val ev = awaitPointerEvent()
                    val c = ev.changes.firstOrNull { it.pressed } ?: break
                    onCursor(idx(c.position.x))
                }
            }
        }
    ) {
        val w = size.width
        val h = size.height
        val pad = 3f
        var lo = minOf(values.min(), targetLow)
        var hi = maxOf(values.max(), targetHigh)
        if (hi - lo < 20) { val mid = (hi + lo) / 2; lo = mid - 10; hi = mid + 10 }
        val range = (hi - lo).toFloat().coerceAtLeast(1f)
        fun y(v: Int): Float = pad + (h - 2 * pad) * (1f - (v - lo) / range)
        fun x(i: Int): Float = pad + (w - 2 * pad) * (i.toFloat() / (values.size - 1))

        val bandTop = y(targetHigh)
        val bandBottom = y(targetLow)
        drawRect(
            color = Color(0x3366BB6A),
            topLeft = Offset(pad, bandTop),
            size = Size(w - 2 * pad, (bandBottom - bandTop).coerceAtLeast(0f))
        )
        val path = Path().apply {
            moveTo(x(0), y(values[0]))
            for (i in 1 until values.size) lineTo(x(i), y(values[i]))
        }
        drawPath(path, color = lineColor, style = Stroke(width = 3f))
        val li = values.size - 1
        drawCircle(lineColor, radius = 4f, center = Offset(x(li), y(values[li])))

        if (cursorIndex != null && cursorIndex in values.indices) {
            val cx = x(cursorIndex)
            drawLine(Color(0xFFB0BEC5), Offset(cx, pad), Offset(cx, h - pad), strokeWidth = 1.5f)
            drawCircle(Color.White, radius = 4.5f, center = Offset(cx, y(values[cursorIndex])))
        }
    }
}
