package org.jellyfin.androidtv.data.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.jellyfin.androidtv.data.database.entity.Show

@Dao
interface ShowDao {
    @Query("SELECT * FROM shows WHERE id = :id")
    suspend fun getShowById(id: Int): Show?
    
    @Query("SELECT * FROM shows WHERE id = :id")
    fun getShowByIdFlow(id: Int): Flow<Show?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShows(shows: List<Show>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShow(show: Show)
    
    @Query("DELETE FROM shows")
    suspend fun clearAllShows()
    
    @Query("SELECT * FROM shows WHERE mediaDataCachedAt < :timestamp")
    suspend fun getStaleShows(timestamp: Long): List<Show>
    
    @Query("DELETE FROM shows WHERE lastAccessedAt < :timestamp")
    suspend fun deleteShowsNotAccessedSince(timestamp: Long)
    
    @Query("SELECT COUNT(*) FROM shows")
    suspend fun getTotalShowCount(): Int
    
    @Query("SELECT COUNT(*) FROM shows WHERE mediaDataCachedAt < :timestamp")
    suspend fun countShowsWithStaleCachedData(timestamp: Long): Int
    
    @Query("UPDATE shows SET lastAccessedAt = :timestamp WHERE id = :showId")
    suspend fun updateLastAccessed(showId: Int, timestamp: Long)
}