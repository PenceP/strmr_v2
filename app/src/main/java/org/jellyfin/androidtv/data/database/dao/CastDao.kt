package org.jellyfin.androidtv.data.database.dao

import androidx.room.*
import org.jellyfin.androidtv.data.database.entity.CastMember
import org.jellyfin.androidtv.data.database.entity.ShowCastMember

@Dao
interface CastDao {
    // Movie cast operations
    @Query("SELECT * FROM cast_members WHERE movieId = :movieId ORDER BY `order` ASC")
    suspend fun getCastForMovie(movieId: Int): List<CastMember>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovieCast(cast: List<CastMember>)
    
    @Query("DELETE FROM cast_members WHERE movieId = :movieId")
    suspend fun deleteMovieCast(movieId: Int)
    
    @Transaction
    suspend fun replaceMovieCast(movieId: Int, cast: List<CastMember>) {
        deleteMovieCast(movieId)
        insertMovieCast(cast)
    }
    
    // Show cast operations
    @Query("SELECT * FROM show_cast_members WHERE showId = :showId ORDER BY `order` ASC")
    suspend fun getCastForShow(showId: Int): List<ShowCastMember>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShowCast(cast: List<ShowCastMember>)
    
    @Query("DELETE FROM show_cast_members WHERE showId = :showId")
    suspend fun deleteShowCast(showId: Int)
    
    @Transaction
    suspend fun replaceShowCast(showId: Int, cast: List<ShowCastMember>) {
        deleteShowCast(showId)
        insertShowCast(cast)
    }
}