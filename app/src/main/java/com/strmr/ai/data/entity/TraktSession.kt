package com.strmr.ai.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trakt_session")
data class TraktSession(
    @PrimaryKey
    val id: String = "trakt_auth", // Single session per app
    @ColumnInfo(name = "access_token")
    val accessToken: String,
    @ColumnInfo(name = "refresh_token")
    val refreshToken: String,
    @ColumnInfo(name = "expires_in")
    val expiresIn: Int,
    @ColumnInfo(name = "scope")
    val scope: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "username")
    val username: String? = null,
    @ColumnInfo(name = "user_id")
    val userId: String? = null,
    @ColumnInfo(name = "last_sync")
    val lastSync: Long? = null
) {
    fun isExpired(): Boolean {
        val currentTime = System.currentTimeMillis() / 1000
        return (createdAt + expiresIn) <= currentTime
    }
    
    fun isExpiringSoon(): Boolean {
        val currentTime = System.currentTimeMillis() / 1000
        val timeLeft = (createdAt + expiresIn) - currentTime
        // Refresh if less than 1 hour remaining
        return timeLeft <= 3600
    }
}