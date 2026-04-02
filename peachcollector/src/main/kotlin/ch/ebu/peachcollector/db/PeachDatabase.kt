package ch.ebu.peachcollector.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ch.ebu.peachcollector.Event
import ch.ebu.peachcollector.EventStatus
import ch.ebu.peachcollector.PeachCollector

/**
 * Room database for PeachCollector event storage.
 */
@Database(
    entities = [Event::class, EventStatus::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PeachDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao

    companion object {
        private const val DATABASE_NAME = "peach_collector_database"

        @Volatile
        private var INSTANCE: PeachDatabase? = null

        fun getInstance(context: Context): PeachDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): PeachDatabase {
            val builder = Room.databaseBuilder(
                context.applicationContext,
                PeachDatabase::class.java,
                DATABASE_NAME
            )
            builder.fallbackToDestructiveMigration()
            if (PeachCollector.isUnitTesting) {
                builder.allowMainThreadQueries()
            }
            return builder.build()
        }

        /**
         * Reset the singleton instance (used in testing).
         */
        internal fun resetInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
