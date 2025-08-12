package com.strmr.ai.data.dao

import androidx.room.*
import com.strmr.ai.data.entity.TraktSession
import kotlinx.coroutines.flow.Flow

@Dao
interface TraktSessionDao {
    @Query("SELECT * FROM trakt_session WHERE id = 'trakt_auth'")
    suspend fun getSession(): TraktSession?
    
    @Query("SELECT * FROM trakt_session WHERE id = 'trakt_auth'")
    fun getSessionFlow(): Flow<TraktSession?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TraktSession)
    
    @Update
    suspend fun updateSession(session: TraktSession)
    
    @Query("DELETE FROM trakt_session WHERE id = 'trakt_auth'")
    suspend fun deleteSession()
    
    @Query("UPDATE trakt_session SET username = :username, user_id = :userId WHERE id = 'trakt_auth'")
    suspend fun updateUserInfo(username: String?, userId: String?)
    
    @Query("UPDATE trakt_session SET last_sync = :lastSync WHERE id = 'trakt_auth'")
    suspend fun updateLastSync(lastSync: Long)
    
    @Query("UPDATE trakt_session SET access_token = :accessToken, refresh_token = :refreshToken, expires_in = :expiresIn, created_at = :createdAt WHERE id = 'trakt_auth'")
    suspend fun updateTokens(accessToken: String, refreshToken: String, expiresIn: Int, createdAt: Long)
}