package de.glucobridge.app.data

import de.glucobridge.core.model.GlucoseReading
import de.glucobridge.core.model.Trend
import de.glucobridge.domain.HistoryStore

/** Room-Adapter fuer den Domain-Port HistoryStore. */
class RoomHistoryStore(private val dao: GlucoseDao) : HistoryStore {

    override suspend fun upsertAll(readings: List<GlucoseReading>) {
        if (readings.isEmpty()) return
        dao.upsertAll(readings.map { ReadingEntity(it.timestampEpochMillis, it.valueMgDl, it.trend.name) })
    }

    override suspend fun since(sinceEpochMillis: Long): List<GlucoseReading> =
        dao.since(sinceEpochMillis).map {
            GlucoseReading(
                valueMgDl = it.valueMgDl,
                trend = runCatching { Trend.valueOf(it.trend) }.getOrDefault(Trend.UNKNOWN),
                timestampEpochMillis = it.timestampEpochMillis
            )
        }

    override suspend fun clear() = dao.clear()
}
