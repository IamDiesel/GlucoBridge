package de.glucobridge.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import de.glucobridge.app.GlucoBridgeApp
import de.glucobridge.app.MainActivity
import de.glucobridge.app.R
import de.glucobridge.app.ui.formatGlucose
import de.glucobridge.app.ui.unitLabel
import de.glucobridge.core.model.GlucoseReading
import de.glucobridge.core.model.Trend
import de.glucobridge.core.model.Zone
import de.glucobridge.domain.GlucoseState
import de.glucobridge.domain.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Hält das Polling am Leben und zeigt den aktuellen Wert in einer dauerhaften Benachrichtigung. */
class GlucoseMonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startInForeground(buildNotification(null, null))
        val repo = (application as GlucoBridgeApp).container.glucoseRepository
        scope.launch {
            combine(repo.state, repo.settings) { st, se -> st to se }.collect { (st, se) ->
                val reading = (st as? GlucoseState.Content)?.reading
                runCatching {
                    NotificationManagerCompat.from(this@GlucoseMonitorService)
                        .notify(NOTIF_ID, buildNotification(reading, se))
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun startInForeground(n: Notification) {
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    private fun createChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "Glukose-Monitor", NotificationManager.IMPORTANCE_LOW)
        ch.setShowBadge(false)
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun buildNotification(reading: GlucoseReading?, settings: Settings?): Notification {
        val title: String
        val text: String
        val color: Int
        if (reading != null && settings != null) {
            val unit = settings.unit
            val zone = settings.target.zoneFor(reading.valueMgDl)
            title = "${formatGlucose(reading.valueMgDl, unit)} ${unitLabel(unit)} ${arrow(reading.trend)}"
            text = "${zoneText(zone)} · ${age(reading.timestampEpochMillis)}"
            color = zoneColor(zone)
        } else {
            title = "GlucoBridge"
            text = "Aktualisiere…"
            color = 0xFF2E7D32.toInt()
        }
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_glucose)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setColor(color)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun arrow(t: Trend): String = when (t) {
        Trend.RISING_QUICK -> "↑"
        Trend.RISING -> "↗"
        Trend.STABLE -> "→"
        Trend.FALLING -> "↘"
        Trend.FALLING_QUICK -> "↓"
        Trend.UNKNOWN -> ""
    }

    private fun zoneText(z: Zone): String = when (z) {
        Zone.LOW -> "Unter Zielbereich"
        Zone.HIGH -> "Über Zielbereich"
        Zone.IN_RANGE -> "Im Zielbereich"
    }

    private fun zoneColor(z: Zone): Int = when (z) {
        Zone.LOW -> 0xFFD32F2F.toInt()
        Zone.HIGH -> 0xFFF9A825.toInt()
        Zone.IN_RANGE -> 0xFF2E7D32.toInt()
    }

    private fun age(ts: Long): String {
        val m = (System.currentTimeMillis() - ts) / 60_000L
        return when {
            m < 1 -> "gerade eben"
            m == 1L -> "vor 1 min"
            m < 60 -> "vor $m min"
            else -> "vor ${m / 60} h ${m % 60} min"
        }
    }

    companion object {
        private const val CHANNEL_ID = "glucose_monitor"
        private const val NOTIF_ID = 1001
    }
}
