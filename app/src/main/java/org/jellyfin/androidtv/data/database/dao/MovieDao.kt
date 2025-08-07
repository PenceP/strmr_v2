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
    @Query("SELECT * FROM movies WHERE category = :category ORDER BY popularity DESC")
    fun getMoviesByCategory(category: String): PagingSource<Int, Movie>
    
    @Query("SELECT * FROM movies WHERE category = :category ORDER BY popularity DESC LIMIT :limit")
    suspend fun getMoviesByCategorySync(category: String, limit: Int): List<Movie>
    
    @Query("SELECT * FROM movies WHERE id = :id")
    suspend fun getMovieById(id: Int): Movie?
    
    @Query("SELECT * FROM movies WHERE id = :id")
    fun getMovieByIdFlow(id: Int): Flow<Movie?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<Movie>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovie(movie: Movie)
    
    @Query("DELETE FROM movies WHERE category = :category")
    suspend fun clearMoviesByCategory(category: String)
    
    @Query("DELETE FROM movies")
    suspend fun clearAllMovies()
    
    @Query("SELECT COUNT(*) FROM movies WHERE category = :category")
    suspend fun getMovieCountByCategory(category: String): Int
    
    @Query("SELECT * FROM movies WHERE category = :category AND lastUpdated < :timestamp")
    suspend fun getStaleMovies(category: String, timestamp: Long): List<Movie>
}