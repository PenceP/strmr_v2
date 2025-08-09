package org.jellyfin.androidtv.data.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.jellyfin.androidtv.data.database.entity.Movie

@Dao
interface MovieDao {
    @Query("SELECT * FROM movies WHERE id = :id")
    suspend fun getMovieById(id: Int): Movie?
    
    @Query("SELECT * FROM movies WHERE id = :id")
    fun getMovieByIdFlow(id: Int): Flow<Movie?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<Movie>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovie(movie: Movie)
    
    @Query("DELETE FROM movies")
    suspend fun clearAllMovies()
    
    @Query("SELECT * FROM movies WHERE mediaDataCachedAt < :timestamp")
    suspend fun getStaleMovies(timestamp: Long): List<Movie>
    
    @Query("DELETE FROM movies WHERE lastAccessedAt < :timestamp")
    suspend fun deleteMoviesNotAccessedSince(timestamp: Long)
    
    @Query("SELECT COUNT(*) FROM movies")
    suspend fun getTotalMovieCount(): Int
    
    @Query("SELECT COUNT(*) FROM movies WHERE mediaDataCachedAt < :timestamp")
    suspend fun countMoviesWithStaleCachedData(timestamp: Long): Int
    
    @Query("UPDATE movies SET lastAccessedAt = :timestamp WHERE id = :movieId")
    suspend fun updateLastAccessed(movieId: Int, timestamp: Long)
}