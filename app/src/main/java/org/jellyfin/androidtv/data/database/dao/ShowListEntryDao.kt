package org.jellyfin.androidtv.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.jellyfin.androidtv.data.database.entity.Show
import org.jellyfin.androidtv.data.database.entity.ShowListEntry

@Dao
interface ShowListEntryDao {
    
    @Query("""
        SELECT s.* FROM shows s
        INNER JOIN show_list_entries sle ON s.id = sle.showId
        WHERE sle.listType = :listType
        ORDER BY sle.position ASC
        LIMIT :limit
    """)
    suspend fun getShowsForList(listType: String, limit: Int): List<Show>
    
    @Query("""
        SELECT s.* FROM shows s
        INNER JOIN show_list_entries sle ON s.id = sle.showId
        WHERE sle.listType = :listType
        ORDER BY sle.position ASC
    """)
    fun getShowsForListFlow(listType: String): Flow<List<Show>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListEntries(entries: List<ShowListEntry>)
    
    @Query("DELETE FROM show_list_entries WHERE listType = :listType")
    suspend fun clearListEntries(listType: String)
    
    @Query("SELECT listUpdatedAt FROM show_list_entries WHERE listType = :listType LIMIT 1")
    suspend fun getListLastUpdated(listType: String): Long?
    
    @Query("""
        SELECT COUNT(*) FROM show_list_entries 
        WHERE listType = :listType 
        AND listUpdatedAt > :timestamp
    """)
    suspend fun countFreshListEntries(listType: String, timestamp: Long): Int
    
    @Transaction
    suspend fun replaceListEntries(listType: String, entries: List<ShowListEntry>) {
        clearListEntries(listType)
        insertListEntries(entries)
    }
}