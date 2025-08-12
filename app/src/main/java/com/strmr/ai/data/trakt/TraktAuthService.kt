package com.strmr.ai.data.trakt

import android.util.Log
import com.strmr.ai.BuildConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.serialization.Serializable
import retrofit2.http.*

interface TraktAuthService {
    @POST("oauth/device/code")
    @Headers("Content-Type: application/json")
    suspend fun getDeviceCode(
        @Body request: DeviceCodeRequest
    ): DeviceCodeResponse

    @POST("oauth/device/token")
    @Headers("Content-Type: application/json")
    suspend fun getAccessToken(
        @Body request: TokenRequest
    ): TokenResponse

    @POST("oauth/token")
    @Headers("Content-Type: application/json")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): TokenResponse

    @GET("users/me")
    suspend fun getUserInfo(
        @Header("Authorization") authorization: String
    ): TraktUser
}

@Serializable
data class DeviceCodeRequest(
    val client_id: String,
    val response_type: String = "device_code"
)

@Serializable
data class DeviceCodeResponse(
    val device_code: String,
    val user_code: String,
    val verification_url: String,
    val expires_in: Int,
    val interval: Int
)

@Serializable
data class TokenRequest(
    val code: String,
    val client_id: String,
    val client_secret: String,
    val grant_type: String = "device_code"
)

@Serializable
data class RefreshTokenRequest(
    val refresh_token: String,
    val client_id: String,
    val client_secret: String,
    val grant_type: String = "refresh_token"
)

@Serializable
data class TokenResponse(
    val access_token: String,
    val refresh_token: String,
    val expires_in: Int,
    val scope: String,
    val created_at: Long
)

@Serializable
data class TraktUser(
    val username: String,
    val private: Boolean,
    val name: String? = null,
    val description: String? = null,
    val vip: Boolean = false,
    val vip_ep: Boolean = false,
    val ids: TraktUserIds? = null
)

@Serializable
data class TraktUserIds(
    val slug: String
)

class TraktAuthManager(
    private val authService: TraktAuthService
) {
    suspend fun startDeviceAuth(): DeviceCodeResponse {
        Log.d("TraktAuthManager", "🚀 Starting device authentication...")
        val request = DeviceCodeRequest(client_id = BuildConfig.TRAKT_CLIENT_ID)
        Log.d("TraktAuthManager", "📋 Request data: $request")
        
        try {
            val response = authService.getDeviceCode(request)
            Log.d("TraktAuthManager", "✅ Device code response received: $response")
            return response
        } catch (e: Exception) {
            Log.e("TraktAuthManager", "❌ Failed to get device code", e)
            throw e
        }
    }

    suspend fun pollForToken(deviceCode: String, interval: Int): TokenResponse? {
        var attempts = 0
        val maxAttempts = 60 // 5 minutes with 5-second intervals
        
        Log.d("TraktAuthManager", "🔄 Starting polling for device code: $deviceCode")
        
        while (attempts < maxAttempts && currentCoroutineContext().isActive) {
            try {
                Log.d("TraktAuthManager", "📡 Polling attempt ${attempts + 1}/$maxAttempts")
                val tokenResponse = authService.getAccessToken(
                    TokenRequest(
                        code = deviceCode,
                        client_id = BuildConfig.TRAKT_CLIENT_ID,
                        client_secret = BuildConfig.TRAKT_CLIENT_SECRET
                    )
                )
                Log.d("TraktAuthManager", "✅ Authorization successful!")
                return tokenResponse
            } catch (e: Exception) {
                if (!currentCoroutineContext().isActive) {
                    Log.d("TraktAuthManager", "🛑 Polling cancelled")
                    break
                }
                Log.d("TraktAuthManager", "⏳ Waiting for user authorization... (attempt ${attempts + 1})")
                attempts++
                delay((interval * 1000).toLong()) // Convert to milliseconds
            }
        }
        
        if (!currentCoroutineContext().isActive) {
            Log.d("TraktAuthManager", "🛑 Polling was cancelled")
        } else {
            Log.w("TraktAuthManager", "❌ Authorization timed out after $maxAttempts attempts")
        }
        return null
    }

    suspend fun refreshAccessToken(refreshToken: String): TokenResponse? {
        return try {
            authService.refreshToken(
                RefreshTokenRequest(
                    refresh_token = refreshToken,
                    client_id = BuildConfig.TRAKT_CLIENT_ID,
                    client_secret = BuildConfig.TRAKT_CLIENT_SECRET
                )
            )
        } catch (e: Exception) {
            Log.e("TraktAuthManager", "❌ Failed to refresh token", e)
            null
        }
    }

    suspend fun getUserInfo(accessToken: String): TraktUser? {
        return try {
            Log.d("TraktAuthManager", "👤 Fetching user info...")
            val user = authService.getUserInfo("Bearer $accessToken")
            Log.d("TraktAuthManager", "✅ User info received: ${user.username}")
            user
        } catch (e: Exception) {
            Log.e("TraktAuthManager", "❌ Failed to get user info", e)
            null
        }
    }
}