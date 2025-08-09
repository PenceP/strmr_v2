package org.jellyfin.androidtv.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.data.api.service.TmdbApiService
import org.jellyfin.androidtv.data.api.service.TraktApiService
import org.jellyfin.androidtv.data.database.AppDatabase
import org.jellyfin.androidtv.data.database.entity.Movie
import org.jellyfin.androidtv.data.database.entity.MovieListEntry
import org.jellyfin.androidtv.data.mapper.MovieMapper
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Improved repository that separates list ordering from media caching
 * - List orders refresh every 24 hours
 * - Movie media data caches for 30 days
 * - Images are handled by Coil's cache separately
 */
class ImprovedMovieRepository(
    private val traktApiService: TraktApiService,
    private val tmdbApiService: TmdbApiService,
    private val database: AppDatabase
) {
    private val movieDao = database.movieDao()
    private val listEntryDao = database.movieListEntryDao()
    
    companion object {
        const val PAGE_SIZE = 20
        const val LIST_TYPE_TRENDING = "trending"
        const val LIST_TYPE_POPULAR = "popular"
        
        // Cache durations
        private val LIST_CACHE_DURATION = TimeUnit.HOURS.toMillis(24) // 24 hours for list order
        private val MEDIA_CACHE_DURATION = TimeUnit.DAYS.toMillis(30) // 30 days for movie data
    }
    
    /**
     * Get trending movies, refreshing list order if needed
     */
    suspend fun getTrendingMovies(forceRefresh: Boolean = false): Flow<List<Movie>> = flow {
        // Check if list needs refresh (24hr timeout)
        if (forceRefresh || isListStale(LIST_TYPE_TRENDING)) {
            refreshListOrder(LIST_TYPE_TRENDING)
        }
        
        // Emit movies from database
        listEntryDao.getMoviesForListFlow(LIST_TYPE_TRENDING).collect { movies ->
            emit(movies)
        }
    }
    
    /**
     * Get popular movies, refreshing list order if needed
     */
    suspend fun getPopularMovies(forceRefresh: Boolean = false): Flow<List<Movie>> = flow {
        // Check if list needs refresh (24hr timeout)
        if (forceRefresh || isListStale(LIST_TYPE_POPULAR)) {
            refreshListOrder(LIST_TYPE_POPULAR)
        }
        
        // Emit movies from database
        listEntryDao.getMoviesForListFlow(LIST_TYPE_POPULAR).collect { movies ->
            emit(movies)
        }
    }
    
    /**
     * Check if a list's order is stale (older than 24 hours)
     */
    private suspend fun isListStale(listType: String): Boolean {
        val lastUpdated = listEntryDao.getListLastUpdated(listType) ?: 0
        val staleThreshold = System.currentTimeMillis() - LIST_CACHE_DURATION
        return lastUpdated < staleThreshold
    }
    
    /**
     * Check if movie media data is stale (older than 30 days)
     */
    private fun isMovieDataStale(movie: Movie?): Boolean {
        if (movie == null) return true
        val staleThreshold = System.currentTimeMillis() - MEDIA_CACHE_DURATION
        return movie.mediaDataCachedAt < staleThreshold
    }
    
    /**
     * Refresh only the list order without re-fetching all movie data
     */
    private suspend fun refreshListOrder(listType: String) {
        try {
            when (listType) {
                LIST_TYPE_TRENDING -> refreshTrendingList()
                LIST_TYPE_POPULAR -> refreshPopularList()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh list order for: $listType")
        }
    }
    
    private suspend fun refreshTrendingList() {
        val traktMovies = traktApiService.getTrendingMovies(
            clientId = BuildConfig.TRAKT_CLIENT_ID,
            page = 1,
            limit = PAGE_SIZE * 2 // Get more to have a good list
        )
        
        val currentTime = System.currentTimeMillis()
        val newListEntries = mutableListOf<MovieListEntry>()
        
        traktMovies.forEachIndexed { index, traktResponse ->
            val movieId = traktResponse.movie.ids.tmdb 
            if (movieId == null) return@forEachIndexed
            
            // Check if we have this movie cached
            var existingMovie = movieDao.getMovieById(movieId)
            
            // If movie doesn't exist or data is stale (30 days), fetch fresh data
            if (isMovieDataStale(existingMovie)) {
                try {
                    val tmdbMovie = tmdbApiService.getMovieDetails(
                        movieId = movieId,
                        authorization = "Bearer ${BuildConfig.TMDB_ACCESS_TOKEN}"
                    )
                    
                    // Fetch releases for certification data
                    val releasesResponse = try {
                        tmdbApiService.getMovieReleases(
                            movieId = movieId,
                            authorization = "Bearer ${BuildConfig.TMDB_ACCESS_TOKEN}"
                        )
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to fetch releases for movie ID: $movieId")
                        null
                    }
                    
                    val movie = MovieMapper.mapTraktAndTmdbToMovie(
                        traktResponse = traktResponse,
                        tmdbMovieDetails = tmdbMovie,
                        category = "", // Not using category anymore
                        releasesResponse = releasesResponse
                    ).copy(
                        mediaDataCachedAt = currentTime,
                        lastAccessedAt = currentTime
                    )
                    
                    movieDao.insertMovie(movie)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to fetch movie data for ID: $movieId")
                    // If we can't fetch but have old data, keep using it
                    if (existingMovie == null) return@forEachIndexed
                }
            } else {
                // Update last accessed time for LRU tracking
                existingMovie?.let {
                    movieDao.insertMovie(it.copy(lastAccessedAt = currentTime))
                }
            }
            
            // Add to list entries with new position
            newListEntries.add(
                MovieListEntry(
                    movieId = movieId,
                    listType = LIST_TYPE_TRENDING,
                    position = index,
                    listUpdatedAt = currentTime
                )
            )
        }
        
        // Replace all list entries atomically
        listEntryDao.replaceListEntries(LIST_TYPE_TRENDING, newListEntries)
    }
    
    private suspend fun refreshPopularList() {
        val traktMovies = traktApiService.getPopularMovies(
            clientId = BuildConfig.TRAKT_CLIENT_ID,
            page = 1,
            limit = PAGE_SIZE * 2
        )
        
        val currentTime = System.currentTimeMillis()
        val newListEntries = mutableListOf<MovieListEntry>()
        
        traktMovies.forEachIndexed { index, traktMovie ->
            val movieId = traktMovie.ids.tmdb 
            if (movieId == null) return@forEachIndexed
            
            // Check if we have this movie cached
            var existingMovie = movieDao.getMovieById(movieId)
            
            // If movie doesn't exist or data is stale (30 days), fetch fresh data
            if (isMovieDataStale(existingMovie)) {
                try {
                    val tmdbMovie = tmdbApiService.getMovieDetails(
                        movieId = movieId,
                        authorization = "Bearer ${BuildConfig.TMDB_ACCESS_TOKEN}"
                    )
                    
                    // Fetch releases for certification data
                    val releasesResponse = try {
                        tmdbApiService.getMovieReleases(
                            movieId = movieId,
                            authorization = "Bearer ${BuildConfig.TMDB_ACCESS_TOKEN}"
                        )
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to fetch releases for movie ID: $movieId")
                        null
                    }
                    
                    val movie = MovieMapper.mapTraktAndTmdbToMovie(
                        traktMovie = traktMovie,
                        tmdbMovieDetails = tmdbMovie,
                        category = "", // Not using category anymore
                        releasesResponse = releasesResponse
                    ).copy(
                        mediaDataCachedAt = currentTime,
                        lastAccessedAt = currentTime
                    )
                    
                    movieDao.insertMovie(movie)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to fetch movie data for ID: $movieId")
                    if (existingMovie == null) return@forEachIndexed
                }
            } else {
                // Update last accessed time
                existingMovie?.let {
                    movieDao.insertMovie(it.copy(lastAccessedAt = currentTime))
                }
            }
            
            // Add to list entries with new position
            newListEntries.add(
                MovieListEntry(
                    movieId = movieId,
                    listType = LIST_TYPE_POPULAR,
                    position = index,
                    listUpdatedAt = currentTime
                )
            )
        }
        
        // Replace all list entries atomically
        listEntryDao.replaceListEntries(LIST_TYPE_POPULAR, newListEntries)
    }
    
    /**
     * Clean up old cached movies that haven't been accessed in 60 days
     * This can be called periodically to manage storage
     */
    suspend fun cleanupOldCache() {
        val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(60)
        movieDao.deleteMoviesNotAccessedSince(cutoffTime)
    }
    
    /**
     * Get cache statistics
     */
    suspend fun getCacheStats(): CacheStats {
        val totalMovies = movieDao.getTotalMovieCount()
        val staleMediaThreshold = System.currentTimeMillis() - MEDIA_CACHE_DURATION
        val staleMovies = movieDao.countMoviesWithStaleCachedData(staleMediaThreshold)
        
        val trendingLastUpdated = listEntryDao.getListLastUpdated(LIST_TYPE_TRENDING)
        val popularLastUpdated = listEntryDao.getListLastUpdated(LIST_TYPE_POPULAR)
        
        return CacheStats(
            totalCachedMovies = totalMovies,
            staleMovies = staleMovies,
            trendingListAge = trendingLastUpdated?.let { System.currentTimeMillis() - it } ?: 0,
            popularListAge = popularLastUpdated?.let { System.currentTimeMillis() - it } ?: 0
        )
    }
    
    data class CacheStats(
        val totalCachedMovies: Int,
        val staleMovies: Int,
        val trendingListAge: Long,
        val popularListAge: Long
    )
}