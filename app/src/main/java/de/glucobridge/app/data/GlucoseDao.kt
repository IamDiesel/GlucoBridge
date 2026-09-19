package de.glucobridge.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface GlucoseDao {
    @Upsert
    suspend fun upsertAll(rows: List<ReadingEntity>)

    @Query("SELECT * FROM readings WHERE timestampEpochMillis >= :since ORDER BY timestampEpochMillis ASC")
    suspend fun since(since: Long): List<ReadingEntity>

    @Query("DELETE FROM readings")
    suspend fun clear()
}
