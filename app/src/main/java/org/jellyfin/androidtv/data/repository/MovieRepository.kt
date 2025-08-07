package org.jellyfin.androidtv.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.data.api.service.TmdbApiService
import org.jellyfin.androidtv.data.api.service.TraktApiService
import org.jellyfin.androidtv.data.database.AppDatabase
import org.jellyfin.androidtv.data.database.entity.Movie
import org.jellyfin.androidtv.data.mapper.MovieMapper
import org.jellyfin.androidtv.data.paging.MovieRemoteMediator
import timber.log.Timber

class MovieRepository(
    private val traktApiService: TraktApiService,
    private val tmdbApiService: TmdbApiService,
    private val database: AppDatabase
) {
    private val movieDao = database.movieDao()
    
    companion object {
        const val PAGE_SIZE = 20
        const val PREFETCH_DISTANCE = 8
        const val CATEGORY_TRENDING = "trending"
        const val CATEGORY_POPULAR = "popular"
        private const val CACHE_TIMEOUT_MS = 30 * 60 * 1000L // 30 minutes
    }
    
    /**
     * Get trending movies with paging support
     */
    @OptIn(ExperimentalPagingApi::class)
    fun getTrendingMovies(): Flow<PagingData<Movie>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = false
            ),
            remoteMediator = MovieRemoteMediator(
                category = CATEGORY_TRENDING,
                traktApiService = traktApiService,
                tmdbApiService = tmdbApiService,
                database = database
            ),
            pagingSourceFactory = { movieDao.getMoviesByCategory(CATEGORY_TRENDING) }
        ).flow
    }
    
    /**
     * Get popular movies with paging support
     */
    @OptIn(ExperimentalPagingApi::class)
    fun getPopularMovies(): Flow<PagingData<Movie>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = false
            ),
            remoteMediator = MovieRemoteMediator(
                category = CATEGORY_POPULAR,
                traktApiService = traktApiService,
                tmdbApiService = tmdbApiService,
                database = database
            ),
            pagingSourceFactory = { movieDao.getMoviesByCategory(CATEGORY_POPULAR) }
        ).flow
    }
    
    /**
     * Get a specific movie by ID
     */
    suspend fun getMovieById(id: Int): Movie? {
        return movieDao.getMovieById(id)
    }
    
    /**
     * Get movies by category synchronously (for initial UI load)
     */
    suspend fun getMoviesByCategorySync(category: String, limit: Int): List<Movie> {
        return movieDao.getMoviesByCategorySync(category, limit)
    }
    
    /**
     * Get movie by ID as Flow for reactive updates
     */
    fun getMovieByIdFlow(id: Int): Flow<Movie?> {
        return movieDao.getMovieByIdFlow(id)
    }
    
    /**
     * Refresh movies for a category (force network fetch)
     */
    suspend fun refreshMovies(category: String): Flow<Result<Unit>> = flow {
        emit(Result.success(Unit))
        try {
            when (category) {
                CATEGORY_TRENDING -> {
                    fetchAndStoreTrendingMovies(page = 1, clearExisting = true)
                }
                CATEGORY_POPULAR -> {
                    fetchAndStorePopularMovies(page = 1, clearExisting = true)
                }
                else -> {
                    emit(Result.failure(IllegalArgumentException("Unknown category: $category")))
                    return@flow
                }
            }
            emit(Result.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh movies for category: $category")
            emit(Result.failure(e))
        }
    }
    
    /**
     * Check if cached data is stale
     */
    private suspend fun isCacheStale(category: String): Boolean {
        val staleTimestamp = System.currentTimeMillis() - CACHE_TIMEOUT_MS
        val staleMovies = movieDao.getStaleMovies(category, staleTimestamp)
        val totalMovies = movieDao.getMovieCountByCategory(category)
        
        // Consider cache stale if more than 50% of movies are stale or if no movies exist
        return totalMovies == 0 || staleMovies.size > totalMovies * 0.5
    }
    
    /**
     * Fetch and store trending movies from APIs
     */
    private suspend fun fetchAndStoreTrendingMovies(page: Int, clearExisting: Boolean = false) {
        if (clearExisting) {
            movieDao.clearMoviesByCategory(CATEGORY_TRENDING)
        }
        
        val traktMovies = traktApiService.getTrendingMovies(
            clientId = BuildConfig.TRAKT_CLIENT_ID,
            page = page,
            limit = PAGE_SIZE
        )
        
        val movies = traktMovies.mapNotNull { traktResponse ->
            try {
                val tmdbId = traktResponse.movie.ids.tmdb
                val tmdbMovie = tmdbId?.let { 
                    tmdbApiService.getMovieDetails(
                        movieId = it,
                        authorization = "Bearer ${BuildConfig.TMDB_ACCESS_TOKEN}"
                    )
                }
                
                MovieMapper.mapTraktAndTmdbToMovie(
                    traktResponse = traktResponse,
                    tmdbMovieDetails = tmdbMovie,
                    category = CATEGORY_TRENDING
                )
            } catch (e: Exception) {
                Timber.w(e, "Failed to fetch TMDB data for movie: ${traktResponse.movie.title}")
                // Fallback to Trakt data only
                MovieMapper.mapTraktToMovie(traktResponse, CATEGORY_TRENDING)
            }
        }
        
        movieDao.insertMovies(movies)
    }
    
    /**
     * Fetch and store popular movies from APIs
     */
    private suspend fun fetchAndStorePopularMovies(page: Int, clearExisting: Boolean = false) {
        if (clearExisting) {
            movieDao.clearMoviesByCategory(CATEGORY_POPULAR)
        }
        
        val traktMovies = traktApiService.getPopularMovies(
            clientId = BuildConfig.TRAKT_CLIENT_ID,
            page = page,
            limit = PAGE_SIZE
        )
        
        val movies = traktMovies.mapNotNull { traktMovie ->
            try {
                val tmdbId = traktMovie.ids.tmdb
                val tmdbMovie = tmdbId?.let { 
                    tmdbApiService.getMovieDetails(
                        movieId = it,
                        authorization = "Bearer ${BuildConfig.TMDB_ACCESS_TOKEN}"
                    )
                }
                
                MovieMapper.mapTraktAndTmdbToMovie(
                    traktMovie = traktMovie,
                    tmdbMovieDetails = tmdbMovie,
                    category = CATEGORY_POPULAR
                )
            } catch (e: Exception) {
                Timber.w(e, "Failed to fetch TMDB data for movie: ${traktMovie.title}")
                // Fallback to Trakt data only
                MovieMapper.mapTraktToMovie(traktMovie, CATEGORY_POPULAR)
            }
        }
        
        movieDao.insertMovies(movies)
    }
}