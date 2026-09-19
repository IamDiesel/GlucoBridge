package de.glucobridge.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ReadingEntity::class], version = 1, exportSchema = false)
abstract class GlucoBridgeDatabase : RoomDatabase() {
    abstract fun glucoseDao(): GlucoseDao
}
