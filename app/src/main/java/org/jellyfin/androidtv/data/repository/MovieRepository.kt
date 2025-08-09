package org.jellyfin.androidtv.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
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
            pagingSourceFactory = { 
                // Create a custom paging source that uses list entries
                object : PagingSource<Int, Movie>() {
                    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> {
                        return try {
                            val movies = database.movieListEntryDao().getMoviesForList(
                                CATEGORY_TRENDING, 
                                params.loadSize
                            )
                            LoadResult.Page(
                                data = movies,
                                prevKey = null,
                                nextKey = if (movies.size < params.loadSize) null else movies.size
                            )
                        } catch (e: Exception) {
                            LoadResult.Error(e)
                        }
                    }
                    
                    override fun getRefreshKey(state: PagingState<Int, Movie>): Int? = null
                }
            }
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
            pagingSourceFactory = { 
                // Create a custom paging source that uses list entries
                object : PagingSource<Int, Movie>() {
                    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> {
                        return try {
                            val movies = database.movieListEntryDao().getMoviesForList(
                                CATEGORY_POPULAR, 
                                params.loadSize
                            )
                            LoadResult.Page(
                                data = movies,
                                prevKey = null,
                                nextKey = if (movies.size < params.loadSize) null else movies.size
                            )
                        } catch (e: Exception) {
                            LoadResult.Error(e)
                        }
                    }
                    
                    override fun getRefreshKey(state: PagingState<Int, Movie>): Int? = null
                }
            }
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
     * Now uses the new list entry system
     */
    suspend fun getMoviesByCategorySync(category: String, limit: Int): List<Movie> {
        val listEntryDao = database.movieListEntryDao()
        return listEntryDao.getMoviesForList(category, limit)
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
     * Check if cached list data is stale
     */
    private suspend fun isCacheStale(category: String): Boolean {
        val listEntryDao = database.movieListEntryDao()
        val staleTimestamp = System.currentTimeMillis() - CACHE_TIMEOUT_MS
        val lastUpdated = listEntryDao.getListLastUpdated(category)
        
        // Consider cache stale if list hasn't been updated recently
        return lastUpdated == null || lastUpdated < staleTimestamp
    }
    
    /**
     * Fetch and store trending movies from APIs
     */
    private suspend fun fetchAndStoreTrendingMovies(page: Int, clearExisting: Boolean = false) {
        val listEntryDao = database.movieListEntryDao()
        if (clearExisting) {
            listEntryDao.clearListEntries(CATEGORY_TRENDING)
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
                
                // Fetch releases for certification data
                val releasesResponse = tmdbId?.let {
                    try {
                        tmdbApiService.getMovieReleases(
                            movieId = it,
                            authorization = "Bearer ${BuildConfig.TMDB_ACCESS_TOKEN}"
                        )
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to fetch releases for movie ID: $it")
                        null
                    }
                }
                
                MovieMapper.mapTraktAndTmdbToMovie(
                    traktResponse = traktResponse,
                    tmdbMovieDetails = tmdbMovie,
                    category = "", // Category no longer stored in Movie entity
                    releasesResponse = releasesResponse
                ).copy(
                    mediaDataCachedAt = System.currentTimeMillis(),
                    lastAccessedAt = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Timber.w(e, "Failed to fetch TMDB data for movie: ${traktResponse.movie.title}")
                // Fallback to Trakt data only
                MovieMapper.mapTraktToMovie(traktResponse, "").copy(
                    mediaDataCachedAt = System.currentTimeMillis(),
                    lastAccessedAt = System.currentTimeMillis()
                )
            }
        }
        
        movieDao.insertMovies(movies)
        
        // Create list entries
        val listEntries = movies.mapIndexed { index, movie ->
            org.jellyfin.androidtv.data.database.entity.MovieListEntry(
                movieId = movie.id,
                listType = CATEGORY_TRENDING,
                position = (page - 1) * PAGE_SIZE + index,
                listUpdatedAt = System.currentTimeMillis()
            )
        }
        listEntryDao.insertListEntries(listEntries)
    }
    
    /**
     * Fetch and store popular movies from APIs
     */
    private suspend fun fetchAndStorePopularMovies(page: Int, clearExisting: Boolean = false) {
        val listEntryDao = database.movieListEntryDao()
        if (clearExisting) {
            listEntryDao.clearListEntries(CATEGORY_POPULAR)
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
                
                // Fetch releases for certification data
                val releasesResponse = tmdbId?.let {
                    try {
                        tmdbApiService.getMovieReleases(
                            movieId = it,
                            authorization = "Bearer ${BuildConfig.TMDB_ACCESS_TOKEN}"
                        )
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to fetch releases for movie ID: $it")
                        null
                    }
                }
                
                MovieMapper.mapTraktAndTmdbToMovie(
                    traktMovie = traktMovie,
                    tmdbMovieDetails = tmdbMovie,
                    category = "", // Category no longer stored in Movie entity
                    releasesResponse = releasesResponse
                ).copy(
                    mediaDataCachedAt = System.currentTimeMillis(),
                    lastAccessedAt = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Timber.w(e, "Failed to fetch TMDB data for movie: ${traktMovie.title}")
                // Fallback to Trakt data only
                MovieMapper.mapTraktToMovie(traktMovie, "").copy(
                    mediaDataCachedAt = System.currentTimeMillis(),
                    lastAccessedAt = System.currentTimeMillis()
                )
            }
        }
        
        movieDao.insertMovies(movies)
        
        // Create list entries
        val listEntries = movies.mapIndexed { index, movie ->
            org.jellyfin.androidtv.data.database.entity.MovieListEntry(
                movieId = movie.id,
                listType = CATEGORY_POPULAR,
                position = (page - 1) * PAGE_SIZE + index,
                listUpdatedAt = System.currentTimeMillis()
            )
        }
        listEntryDao.insertListEntries(listEntries)
    }
}