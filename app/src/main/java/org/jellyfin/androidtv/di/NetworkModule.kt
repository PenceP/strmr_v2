package org.jellyfin.androidtv.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.data.api.service.TmdbApiService
import org.jellyfin.androidtv.data.api.service.TraktApiService
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

val traktClient = named("traktClient")
val tmdbClient = named("tmdbClient")

val networkModule = module {
    
    // JSON serializer
    single {
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            encodeDefaults = true
        }
    }
    
    // Base OkHttp client
    single {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    // Trakt Retrofit client
    single<Retrofit>(traktClient) {
        Retrofit.Builder()
            .baseUrl(TraktApiService.BASE_URL)
            .client(get<OkHttpClient>())
            .addConverterFactory(get<Json>().asConverterFactory("application/json".toMediaType()))
            .build()
    }
    
    // TMDB Retrofit client
    single<Retrofit>(tmdbClient) {
        Retrofit.Builder()
            .baseUrl(TmdbApiService.BASE_URL)
            .client(get<OkHttpClient>())
            .addConverterFactory(get<Json>().asConverterFactory("application/json".toMediaType()))
            .build()
    }
    
    // API Services
    single<TraktApiService> { get<Retrofit>(traktClient).create(TraktApiService::class.java) }
    single<TmdbApiService> { get<Retrofit>(tmdbClient).create(TmdbApiService::class.java) }
}