package com.strmr.ai.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.strmr.ai.data.database.converter.Converters
import com.strmr.ai.data.database.dao.MovieDao
import com.strmr.ai.data.database.dao.MovieListEntryDao
import com.strmr.ai.data.database.dao.ShowDao
import com.strmr.ai.data.database.dao.ShowListEntryDao
import com.strmr.ai.data.database.dao.CastDao
import com.strmr.ai.data.database.entity.Movie
import com.strmr.ai.data.database.entity.MovieListEntry
import com.strmr.ai.data.database.entity.Show
import com.strmr.ai.data.database.entity.ShowListEntry
import com.strmr.ai.data.database.entity.CastMember
import com.strmr.ai.data.database.entity.ShowCastMember
import com.strmr.ai.data.dao.TraktSessionDao
import com.strmr.ai.data.entity.TraktSession

@Database(
    entities = [Movie::class, MovieListEntry::class, Show::class, ShowListEntry::class, CastMember::class, ShowCastMember::class, TraktSession::class],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun movieDao(): MovieDao
    abstract fun movieListEntryDao(): MovieListEntryDao
    abstract fun showDao(): ShowDao
    abstract fun showListEntryDao(): ShowListEntryDao
    abstract fun castDao(): CastDao
    abstract fun traktSessionDao(): TraktSessionDao
    
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
        
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new fields to movies table
                database.execSQL("ALTER TABLE movies ADD COLUMN runtime INTEGER")
                database.execSQL("ALTER TABLE movies ADD COLUMN certification TEXT")
                database.execSQL("ALTER TABLE movies ADD COLUMN rottenTomatoesRating INTEGER")
            }
        }
        
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create shows table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS shows (
                        id INTEGER PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        overview TEXT,
                        firstAirDate TEXT,
                        lastAirDate TEXT,
                        posterPath TEXT,
                        backdropPath TEXT,
                        voteAverage REAL NOT NULL,
                        voteCount INTEGER NOT NULL,
                        genreIds TEXT NOT NULL,
                        originalLanguage TEXT NOT NULL,
                        originalName TEXT NOT NULL,
                        popularity REAL NOT NULL,
                        adult INTEGER NOT NULL,
                        originCountry TEXT NOT NULL,
                        status TEXT,
                        numberOfEpisodes INTEGER,
                        numberOfSeasons INTEGER,
                        episodeRunTime TEXT NOT NULL,
                        networks TEXT NOT NULL,
                        traktId INTEGER,
                        traktSlug TEXT,
                        contentRating TEXT,
                        rottenTomatoesRating INTEGER,
                        mediaDataCachedAt INTEGER NOT NULL,
                        lastAccessedAt INTEGER NOT NULL
                    )
                """)
                
                // Create show_list_entries table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS show_list_entries (
                        showId INTEGER NOT NULL,
                        listType TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        listUpdatedAt INTEGER NOT NULL,
                        PRIMARY KEY(showId, listType),
                        FOREIGN KEY(showId) REFERENCES shows(id) ON DELETE CASCADE
                    )
                """)
                
                database.execSQL("CREATE INDEX index_show_list_entries_listType_position ON show_list_entries(listType, position)")
                database.execSQL("CREATE INDEX index_show_list_entries_showId ON show_list_entries(showId)")
            }
        }
        
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create cast_members table for movies
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS cast_members (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        movieId INTEGER NOT NULL,
                        tmdbCreditId TEXT NOT NULL,
                        personId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        character TEXT,
                        `order` INTEGER NOT NULL,
                        profilePath TEXT,
                        department TEXT NOT NULL,
                        job TEXT,
                        FOREIGN KEY(movieId) REFERENCES movies(id) ON DELETE CASCADE
                    )
                """)
                
                database.execSQL("CREATE INDEX index_cast_members_movieId ON cast_members(movieId)")
                
                // Create show_cast_members table for TV shows
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS show_cast_members (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        showId INTEGER NOT NULL,
                        tmdbCreditId TEXT NOT NULL,
                        personId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        character TEXT,
                        `order` INTEGER NOT NULL,
                        profilePath TEXT,
                        department TEXT NOT NULL,
                        job TEXT,
                        FOREIGN KEY(showId) REFERENCES shows(id) ON DELETE CASCADE
                    )
                """)
                
                database.execSQL("CREATE INDEX index_show_cast_members_showId ON show_cast_members(showId)")
            }
        }
        
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add collection fields to movies table
                database.execSQL("ALTER TABLE movies ADD COLUMN collectionId INTEGER")
                database.execSQL("ALTER TABLE movies ADD COLUMN collectionName TEXT")
            }
        }
        
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create trakt_session table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS trakt_session (
                        id TEXT NOT NULL PRIMARY KEY,
                        access_token TEXT NOT NULL,
                        refresh_token TEXT NOT NULL,
                        expires_in INTEGER NOT NULL,
                        scope TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        username TEXT,
                        user_id TEXT,
                        last_sync INTEGER
                    )
                """)
            }
        }
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "trakt_movies_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}