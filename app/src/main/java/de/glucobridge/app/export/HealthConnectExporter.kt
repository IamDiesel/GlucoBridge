package de.glucobridge.app.export

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.BloodGlucose
import de.glucobridge.core.model.GlucoseReading
import java.time.Instant
import java.time.ZoneId

/** Schreibt Glukosewerte als BloodGlucoseRecord nach Google Health Connect. */
class HealthConnectExporter(private val context: Context) {

    val permissions = setOf(HealthPermission.getWritePermission(BloodGlucoseRecord::class))

    fun available(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    private fun client(): HealthConnectClient = HealthConnectClient.getOrCreate(context)

    suspend fun hasPermission(): Boolean =
        available() && client().permissionController.getGrantedPermissions().containsAll(permissions)

    /** Schreibt die Messwerte; gibt die Anzahl geschriebener Records zurueck. */
    suspend fun write(readings: List<GlucoseReading>): Int {
        if (readings.isEmpty() || !available()) return 0
        val zone = ZoneId.systemDefault()
        val recs = readings.map { r ->
            val instant = Instant.ofEpochMilli(r.timestampEpochMillis)
            BloodGlucoseRecord(
                time = instant,
                zoneOffset = zone.rules.getOffset(instant),
                level = BloodGlucose.milligramsPerDeciliter(r.valueMgDl.toDouble()),
                specimenSource = BloodGlucoseRecord.SPECIMEN_SOURCE_INTERSTITIAL_FLUID,
                relationToMeal = BloodGlucoseRecord.RELATION_TO_MEAL_UNKNOWN,
                mealType = MealType.MEAL_TYPE_UNKNOWN,
                metadata = Metadata.manualEntry()
            )
        }
        client().insertRecords(recs)
        return recs.size
    }
}
