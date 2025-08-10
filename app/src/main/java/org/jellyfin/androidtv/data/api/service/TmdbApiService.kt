package org.jellyfin.androidtv.data.api.service

import org.jellyfin.androidtv.data.api.model.tmdb.TmdbConfiguration
import org.jellyfin.androidtv.data.api.model.tmdb.TmdbMovie
import org.jellyfin.androidtv.data.api.model.tmdb.TmdbMovieDetails
import org.jellyfin.androidtv.data.api.model.tmdb.TmdbMovieResponse
import org.jellyfin.androidtv.data.api.model.tmdb.TmdbMovieReleasesResponse
import org.jellyfin.androidtv.data.api.model.tmdb.TmdbShow
import org.jellyfin.androidtv.data.api.model.tmdb.TmdbShowDetails
import org.jellyfin.androidtv.data.api.model.tmdb.TmdbShowResponse
import org.jellyfin.androidtv.data.api.model.tmdb.TmdbCreditsResponse
import org.jellyfin.androidtv.data.api.model.tmdb.TmdbCollectionDetails
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
    
    /**
     * Get movie release dates and certifications
     */
    @GET("movie/{movie_id}/release_dates")
    suspend fun getMovieReleases(
        @Path("movie_id") movieId: Int,
        @Header("Authorization") authorization: String
    ): TmdbMovieReleasesResponse
    
    /**
     * Get TV show details by TMDB ID
     */
    @GET("tv/{tv_id}")
    suspend fun getShowDetails(
        @Path("tv_id") showId: Int,
        @Header("Authorization") authorization: String,
        @Query("language") language: String = "en-US",
        @Query("append_to_response") appendToResponse: String? = null
    ): TmdbShowDetails
    
    /**
     * Get trending TV shows
     */
    @GET("trending/tv/day")
    suspend fun getTrendingShows(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US"
    ): TmdbShowResponse
    
    /**
     * Get popular TV shows
     */
    @GET("tv/popular")
    suspend fun getPopularShows(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US"
    ): TmdbShowResponse
    
    /**
     * Get movie credits (cast and crew)
     */
    @GET("movie/{movie_id}/credits")
    suspend fun getMovieCredits(
        @Path("movie_id") movieId: Int,
        @Header("Authorization") authorization: String,
        @Query("language") language: String = "en-US"
    ): TmdbCreditsResponse
    
    /**
     * Get TV show credits (cast and crew)
     */
    @GET("tv/{tv_id}/credits")
    suspend fun getShowCredits(
        @Path("tv_id") showId: Int,
        @Header("Authorization") authorization: String,
        @Query("language") language: String = "en-US"
    ): TmdbCreditsResponse
    
    /**
     * Get collection details
     */
    @GET("collection/{collection_id}")
    suspend fun getCollectionDetails(
        @Path("collection_id") collectionId: Int,
        @Header("Authorization") authorization: String,
        @Query("language") language: String = "en-US"
    ): TmdbCollectionDetails
    
    /**
     * Get similar movies
     */
    @GET("movie/{movie_id}/similar")
    suspend fun getSimilarMovies(
        @Path("movie_id") movieId: Int,
        @Header("Authorization") authorization: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TmdbMovieResponse
    
    /**
     * Get similar TV shows
     */
    @GET("tv/{tv_id}/similar")
    suspend fun getSimilarShows(
        @Path("tv_id") showId: Int,
        @Header("Authorization") authorization: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TmdbShowResponse
    
    companion object {
        const val BASE_URL = "https://api.themoviedb.org/3/"
        const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"
        
        // Common image sizes
        const val POSTER_SIZE_W342 = "w342"
        const val POSTER_SIZE_W500 = "w500"
        const val BACKDROP_SIZE_W780 = "w780"
        const val BACKDROP_SIZE_W1280 = "w1280"
        const val PROFILE_SIZE_W185 = "w185"
        const val PROFILE_SIZE_W342 = "w342"
        
        fun getPosterUrl(posterPath: String?, size: String = POSTER_SIZE_W500): String? {
            return posterPath?.let { "$IMAGE_BASE_URL$size$it" }
        }
        
        fun getBackdropUrl(backdropPath: String?, size: String = BACKDROP_SIZE_W1280): String? {
            return backdropPath?.let { "$IMAGE_BASE_URL$size$it" }
        }
        
        fun getProfileUrl(profilePath: String?, size: String = PROFILE_SIZE_W185): String? {
            return profilePath?.let { "$IMAGE_BASE_URL$size$it" }
        }
    }
}