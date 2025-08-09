package org.jellyfin.androidtv.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import org.jellyfin.androidtv.data.database.converter.Converters
import org.jellyfin.androidtv.data.database.dao.MovieDao
import org.jellyfin.androidtv.data.database.dao.MovieListEntryDao
import org.jellyfin.androidtv.data.database.entity.Movie
import org.jellyfin.androidtv.data.database.entity.MovieListEntry

@Database(
    entities = [Movie::class, MovieListEntry::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun movieDao(): MovieDao
    abstract fun movieListEntryDao(): MovieListEntryDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new movie_list_entries table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS movie_list_entries (
                        movieId INTEGER NOT NULL,
                        listType TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        listUpdatedAt INTEGER NOT NULL,
                        PRIMARY KEY(movieId, listType),
                        FOREIGN KEY(movieId) REFERENCES movies(id) ON DELETE CASCADE
                    )
                """)
                
                database.execSQL("CREATE INDEX index_movie_list_entries_listType_position ON movie_list_entries(listType, position)")
                database.execSQL("CREATE INDEX index_movie_list_entries_movieId ON movie_list_entries(movieId)")
                
                // Rename lastUpdated to mediaDataCachedAt and add lastAccessedAt
                database.execSQL("ALTER TABLE movies ADD COLUMN mediaDataCachedAt INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
                database.execSQL("ALTER TABLE movies ADD COLUMN lastAccessedAt INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
                
                // Migrate existing category data to movie_list_entries
                database.execSQL("""
                    INSERT INTO movie_list_entries (movieId, listType, position, listUpdatedAt)
                    SELECT id, category, 
                           ROW_NUMBER() OVER (PARTITION BY category ORDER BY popularity DESC) - 1,
                           lastUpdated
                    FROM movies
                    WHERE category IS NOT NULL
                """)
                
                // Drop the category column (create new table without it and copy data)
                database.execSQL("""
                    CREATE TABLE movies_new (
                        id INTEGER PRIMARY KEY NOT NULL,
                        title TEXT NOT NULL,
                        overview TEXT,
                        releaseDate TEXT,
                        posterPath TEXT,
                        backdropPath TEXT,
                        voteAverage REAL NOT NULL,
                        voteCount INTEGER NOT NULL,
                        genreIds TEXT NOT NULL,
                        originalLanguage TEXT NOT NULL,
                        originalTitle TEXT NOT NULL,
                        popularity REAL NOT NULL,
                        video INTEGER NOT NULL,
                        adult INTEGER NOT NULL,
                        traktId INTEGER,
                        traktSlug TEXT,
                        mediaDataCachedAt INTEGER NOT NULL,
                        lastAccessedAt INTEGER NOT NULL
                    )
                """)
                
                database.execSQL("""
                    INSERT INTO movies_new 
                    SELECT id, title, overview, releaseDate, posterPath, backdropPath,
                           voteAverage, voteCount, genreIds, originalLanguage, originalTitle,
                           popularity, video, adult, traktId, traktSlug,
                           mediaDataCachedAt, lastAccessedAt
                    FROM movies
                """)
                
                database.execSQL("DROP TABLE movies")
                database.execSQL("ALTER TABLE movies_new RENAME TO movies")
            }
        }
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "trakt_movies_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}