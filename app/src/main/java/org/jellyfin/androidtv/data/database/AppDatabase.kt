package org.jellyfin.androidtv.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import org.jellyfin.androidtv.data.database.converter.Converters
import org.jellyfin.androidtv.data.database.dao.MovieDao
import org.jellyfin.androidtv.data.database.entity.Movie

@Database(
    entities = [Movie::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun movieDao(): MovieDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "trakt_movies_database"
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}