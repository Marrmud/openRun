package org.openrun.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RunEntity::class, TrackPointEntity::class, PhaseResultEntity::class, DoneSessionEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class OpenRunDatabase : RoomDatabase() {
    abstract fun runs(): RunDao
    abstract fun tracks(): TrackDao
    abstract fun done(): DoneDao

    companion object {
        fun open(context: Context): OpenRunDatabase =
            Room.databaseBuilder(context.applicationContext, OpenRunDatabase::class.java, "openrun.db").build()
    }
}
