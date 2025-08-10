package org.jellyfin.androidtv.data.api.service

import org.jellyfin.androidtv.data.api.model.trakt.TraktMovie
import org.jellyfin.androidtv.data.api.model.trakt.TraktMovieResponse
import org.jellyfin.androidtv.data.api.model.trakt.TraktShow
import org.jellyfin.androidtv.data.api.model.trakt.TraktShowResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
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
    
    /**
     * Get trending TV shows from Trakt
     */
    @GET("shows/trending")
    suspend fun getTrendingShows(
        @Header("trakt-api-version") version: String = "2",
        @Header("trakt-api-key") clientId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): List<TraktShowResponse>
    
    /**
     * Get popular TV shows from Trakt
     */
    @GET("shows/popular")
    suspend fun getPopularShows(
        @Header("trakt-api-version") version: String = "2",
        @Header("trakt-api-key") clientId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): List<TraktShow>
    
    /**
     * Get related movies from Trakt
     */
    @GET("movies/{id}/related")
    suspend fun getRelatedMovies(
        @Path("id") movieSlug: String,
        @Header("trakt-api-version") version: String = "2",
        @Header("trakt-api-key") clientId: String,
        @Query("limit") limit: Int = 10
    ): List<TraktMovie>
    
    /**
     * Get related TV shows from Trakt
     */
    @GET("shows/{id}/related")
    suspend fun getRelatedShows(
        @Path("id") showSlug: String,
        @Header("trakt-api-version") version: String = "2",
        @Header("trakt-api-key") clientId: String,
        @Query("limit") limit: Int = 10
    ): List<TraktShow>
    
    companion object {
        const val BASE_URL = "https://api.trakt.tv/"
        const val API_VERSION = "2"
    }
}