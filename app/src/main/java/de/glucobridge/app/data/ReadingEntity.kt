package de.glucobridge.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Persistierter Messwert. Zeitstempel = natuerlicher Schluessel -> automatische Dedup. */
@Entity(tableName = "readings")
data class ReadingEntity(
    @PrimaryKey val timestampEpochMillis: Long,
    val valueMgDl: Int,
    val trend: String
)
