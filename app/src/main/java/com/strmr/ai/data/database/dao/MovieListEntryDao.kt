package com.strmr.ai.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import com.strmr.ai.data.database.entity.Movie
import com.strmr.ai.data.database.entity.MovieListEntry

@Dao
interface MovieListEntryDao {
    
    @Query("""
        SELECT m.* FROM movies m
        INNER JOIN movie_list_entries mle ON m.id = mle.movieId
        WHERE mle.listType = :listType
        ORDER BY mle.position ASC
        LIMIT :limit
    """)
    suspend fun getMoviesForList(listType: String, limit: Int): List<Movie>
    
    @Query("""
        SELECT m.* FROM movies m
        INNER JOIN movie_list_entries mle ON m.id = mle.movieId
        WHERE mle.listType = :listType
        ORDER BY mle.position ASC
    """)
    fun getMoviesForListFlow(listType: String): Flow<List<Movie>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListEntries(entries: List<MovieListEntry>)
    
    @Query("DELETE FROM movie_list_entries WHERE listType = :listType")
    suspend fun clearListEntries(listType: String)
    
    @Query("SELECT listUpdatedAt FROM movie_list_entries WHERE listType = :listType LIMIT 1")
    suspend fun getListLastUpdated(listType: String): Long?
    
    @Query("""
        SELECT COUNT(*) FROM movie_list_entries 
        WHERE listType = :listType 
        AND listUpdatedAt > :timestamp
    """)
    suspend fun countFreshListEntries(listType: String, timestamp: Long): Int
    
    @Transaction
    suspend fun replaceListEntries(listType: String, entries: List<MovieListEntry>) {
        clearListEntries(listType)
        insertListEntries(entries)
    }
}