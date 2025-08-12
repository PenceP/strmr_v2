package com.strmr.ai.data.trakt

import android.util.Log
import com.strmr.ai.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

class TraktAuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        Log.d("TraktAuthInterceptor", "🔧 Processing request to: ${originalRequest.url}")
        Log.d("TraktAuthInterceptor", "📋 BuildConfig.TRAKT_CLIENT_ID: ${BuildConfig.TRAKT_CLIENT_ID}")
        Log.d("TraktAuthInterceptor", "📋 BuildConfig.TRAKT_CLIENT_SECRET length: ${BuildConfig.TRAKT_CLIENT_SECRET.length}")
        
        val request = originalRequest.newBuilder()
            .addHeader("trakt-api-version", "2")
            .addHeader("trakt-api-key", BuildConfig.TRAKT_CLIENT_ID)
            .build()
        
        Log.d("TraktAuthInterceptor", "🔧 Added headers:")
        Log.d("TraktAuthInterceptor", "  - trakt-api-version: 2")
        Log.d("TraktAuthInterceptor", "  - trakt-api-key: ${BuildConfig.TRAKT_CLIENT_ID}")
        
        val response = chain.proceed(request)
        
        Log.d("TraktAuthInterceptor", "📡 Response code: ${response.code}")
        Log.d("TraktAuthInterceptor", "📡 Response message: ${response.message}")
        
        if (!response.isSuccessful) {
            Log.e("TraktAuthInterceptor", "❌ Request failed with ${response.code}: ${response.message}")
            response.body?.let { body ->
                val bodyString = body.string()
                Log.e("TraktAuthInterceptor", "❌ Response body: $bodyString")
                // Need to recreate response body since we consumed it
                val newBody = okhttp3.ResponseBody.create(body.contentType(), bodyString)
                return response.newBuilder().body(newBody).build()
            }
        }
        
        return response
    }
}