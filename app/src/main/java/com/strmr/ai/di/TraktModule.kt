package com.strmr.ai.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.strmr.ai.data.repository.TraktRepository
import com.strmr.ai.data.trakt.TraktAuthInterceptor
import com.strmr.ai.data.trakt.TraktAuthManager
import com.strmr.ai.data.trakt.TraktAuthService
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit

val traktModule = module {
    single<TraktAuthService> {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
            setLevel(HttpLoggingInterceptor.Level.BODY)
        }
        
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(TraktAuthInterceptor())
            .build()
        
        Retrofit.Builder()
            .baseUrl("https://api.trakt.tv/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TraktAuthService::class.java)
    }
    
    single<TraktAuthManager> {
        TraktAuthManager(get())
    }
    
    single<TraktRepository> {
        TraktRepository(
            traktSessionDao = get<com.strmr.ai.data.database.AppDatabase>().traktSessionDao(),
            traktAuthManager = get()
        )
    }
}