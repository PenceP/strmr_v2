package com.strmr.ai.data.repository

import android.util.Log
import com.strmr.ai.data.dao.TraktSessionDao
import com.strmr.ai.data.entity.TraktSession
import com.strmr.ai.data.trakt.DeviceCodeResponse
import com.strmr.ai.data.trakt.TraktAuthManager
import com.strmr.ai.data.trakt.TokenResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TraktRepository(
    private val traktSessionDao: TraktSessionDao,
    private val traktAuthManager: TraktAuthManager,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private var pollingJob: Job? = null
    
    fun getSessionFlow(): Flow<TraktSession?> = traktSessionDao.getSessionFlow()
    
    suspend fun getSession(): TraktSession? = traktSessionDao.getSession()
    
    suspend fun isLoggedIn(): Boolean = getSession() != null
    
    suspend fun startDeviceAuth(): DeviceCodeResponse {
        return traktAuthManager.startDeviceAuth()
    }
    
    suspend fun startPollingForAuth(deviceCode: String, interval: Int): Result<TokenResponse> {
        return try {
            val tokenResponse = traktAuthManager.pollForToken(deviceCode, interval)
            if (tokenResponse != null) {
                saveSession(tokenResponse)
                Result.success(tokenResponse)
            } else {
                Result.failure(Exception("Authentication timed out or was cancelled"))
            }
        } catch (e: Exception) {
            Log.e("TraktRepository", "Error during polling", e)
            Result.failure(e)
        }
    }
    
    fun startPollingInBackground(deviceCode: String, interval: Int, onResult: (Result<TokenResponse>) -> Unit) {
        pollingJob?.cancel()
        pollingJob = coroutineScope.launch {
            val result = startPollingForAuth(deviceCode, interval)
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }
    
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }
    
    private suspend fun saveSession(tokenResponse: TokenResponse) {
        Log.d("TraktRepository", "💾 Saving Trakt session...")
        
        // Fetch user info
        val userInfo = traktAuthManager.getUserInfo(tokenResponse.access_token)
        
        val session = TraktSession(
            accessToken = tokenResponse.access_token,
            refreshToken = tokenResponse.refresh_token,
            expiresIn = tokenResponse.expires_in,
            scope = tokenResponse.scope,
            createdAt = tokenResponse.created_at,
            username = userInfo?.username,
            userId = userInfo?.ids?.slug
        )
        traktSessionDao.insertSession(session)
        Log.d("TraktRepository", "✅ Trakt session saved successfully with user: ${userInfo?.username}")
    }
    
    suspend fun refreshTokenIfNeeded(): Boolean {
        val session = getSession() ?: return false
        
        if (!session.isExpiringSoon()) {
            return true // Token is still valid
        }
        
        Log.d("TraktRepository", "🔄 Refreshing Trakt token...")
        return try {
            val tokenResponse = traktAuthManager.refreshAccessToken(session.refreshToken)
            if (tokenResponse != null) {
                traktSessionDao.updateTokens(
                    accessToken = tokenResponse.access_token,
                    refreshToken = tokenResponse.refresh_token,
                    expiresIn = tokenResponse.expires_in,
                    createdAt = tokenResponse.created_at
                )
                Log.d("TraktRepository", "✅ Token refreshed successfully")
                true
            } else {
                Log.w("TraktRepository", "❌ Failed to refresh token - need to re-authenticate")
                // Token refresh failed, clear the session
                logout()
                false
            }
        } catch (e: Exception) {
            Log.e("TraktRepository", "❌ Error refreshing token", e)
            logout()
            false
        }
    }
    
    suspend fun updateUserInfo(username: String, userId: String) {
        traktSessionDao.updateUserInfo(username, userId)
    }
    
    suspend fun updateLastSync() {
        traktSessionDao.updateLastSync(System.currentTimeMillis())
    }
    
    suspend fun logout() {
        stopPolling()
        traktSessionDao.deleteSession()
        Log.d("TraktRepository", "🚪 Logged out from Trakt")
    }
    
    suspend fun getValidAccessToken(): String? {
        if (!refreshTokenIfNeeded()) {
            return null
        }
        return getSession()?.accessToken
    }
}