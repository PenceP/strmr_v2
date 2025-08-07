package org.jellyfin.androidtv.data.api.service

import org.jellyfin.androidtv.data.api.model.tmdb.TmdbConfiguration
import org.jellyfin.androidtv.data.api.model.tmdb.TmdbMovie
import org.jellyfin.androidtv.data.api.model.tmdb.TmdbMovieDetails
import org.jellyfin.androidtv.data.api.model.tmdb.TmdbMovieResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {
    
    /**
     * Get TMDB configuration
     */
    @GET("configuration")
    suspend fun getConfiguration(
        @Header("Authorization") authorization: String
    ): TmdbConfiguration
    
    /**
     * Get movie details by TMDB ID
     */
    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Header("Authorization") authorization: String,
        @Query("language") language: String = "en-US"
    ): TmdbMovieDetails
    
    /**
     * Search for movies by title
     */
    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Header("Authorization") authorization: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("year") year: Int? = null
    ): TmdbMovieResponse
    
    /**
     * Get trending movies
     */
    @GET("trending/movie/day")
    suspend fun getTrendingMovies(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US"
    ): TmdbMovieResponse
    
    /**
     * Get popular movies
     */
    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US"
    ): TmdbMovieResponse
    
    companion object {
        const val BASE_URL = "https://api.themoviedb.org/3/"
        const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"
        
        // Common image sizes
        const val POSTER_SIZE_W342 = "w342"
        const val POSTER_SIZE_W500 = "w500"
        const val BACKDROP_SIZE_W780 = "w780"
        const val BACKDROP_SIZE_W1280 = "w1280"
        
        fun getPosterUrl(posterPath: String?, size: String = POSTER_SIZE_W500): String? {
            return posterPath?.let { "$IMAGE_BASE_URL$size$it" }
        }
        
        fun getBackdropUrl(backdropPath: String?, size: String = BACKDROP_SIZE_W1280): String? {
            return backdropPath?.let { "$IMAGE_BASE_URL$size$it" }
        }
    }
}