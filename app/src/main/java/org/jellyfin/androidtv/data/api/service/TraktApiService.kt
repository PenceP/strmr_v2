package org.jellyfin.androidtv.data.api.service

import org.jellyfin.androidtv.data.api.model.trakt.TraktMovie
import org.jellyfin.androidtv.data.api.model.trakt.TraktMovieResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface TraktApiService {
    
    /**
     * Get trending movies from Trakt
     */
    @GET("movies/trending")
    suspend fun getTrendingMovies(
        @Header("trakt-api-version") version: String = "2",
        @Header("trakt-api-key") clientId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): List<TraktMovieResponse>
    
    /**
     * Get popular movies from Trakt
     */
    @GET("movies/popular")
    suspend fun getPopularMovies(
        @Header("trakt-api-version") version: String = "2",
        @Header("trakt-api-key") clientId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): List<TraktMovie>
    
    companion object {
        const val BASE_URL = "https://api.trakt.tv/"
        const val API_VERSION = "2"
    }
}