package com.strmr.ai.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.strmr.ai.BuildConfig
import com.strmr.ai.data.api.service.TmdbApiService
import com.strmr.ai.data.api.service.TraktApiService
import com.strmr.ai.data.database.AppDatabase
import com.strmr.ai.data.database.entity.Movie
import com.strmr.ai.data.mapper.MovieMapper
import com.strmr.ai.data.repository.MovieRepository.Companion.PAGE_SIZE
import timber.log.Timber

@OptIn(ExperimentalPagingApi::class)
class MovieRemoteMediator(
    private val category: String,
    private val traktApiService: TraktApiService,
    private val tmdbApiService: TmdbApiService,
    private val database: AppDatabase
) : RemoteMediator<Int, Movie>() {
    
    private val movieDao = database.movieDao()
    private val listEntryDao = database.movieListEntryDao()
    private var currentPage = 1
    
    override suspend fun initialize(): InitializeAction {
        val cacheTimeout = System.currentTimeMillis() - CACHE_TIMEOUT_MS
        val listLastUpdated = listEntryDao.getListLastUpdated(category)
        
        return if (listLastUpdated == null || listLastUpdated < cacheTimeout) {
            // List doesn't exist or is stale, trigger a refresh
            InitializeAction.LAUNCH_INITIAL_REFRESH
        } else {
            // List is relatively fresh, skip initial refresh
            InitializeAction.SKIP_INITIAL_REFRESH
        }
    }
    
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Movie>
    ): MediatorResult {
        return try {
            val page = when (loadType) {
                LoadType.REFRESH -> {
                    currentPage = 1
                    1
                }
                LoadType.PREPEND -> {
                    // We don't support prepending in this implementation
                    return MediatorResult.Success(endOfPaginationReached = true)
                }
                LoadType.APPEND -> {
                    currentPage + 1
                }
            }
            
            Timber.d("Loading $category movies - LoadType: $loadType, Page: $page")
            
            // Fetch data from Trakt API and map to Movie entities
            val movies = when (category) {
                "trending" -> {
                    val traktMovies = traktApiService.getTrendingMovies(
                        clientId = BuildConfig.TRAKT_CLIENT_ID,
                        page = page,
                        limit = PAGE_SIZE
                    )
                    
                    traktMovies.mapNotNull { traktResponse ->
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
                                category = category,
                                releasesResponse = releasesResponse
                            )
                        } catch (e: Exception) {
                            Timber.w(e, "Failed to fetch TMDB data for movie: ${traktResponse.movie.title}")
                            // Fallback to Trakt data only
                            MovieMapper.mapTraktToMovie(traktResponse, category)
                        }
                    }
                }
                "popular" -> {
                    val traktMovies = traktApiService.getPopularMovies(
                        clientId = BuildConfig.TRAKT_CLIENT_ID,
                        page = page,
                        limit = PAGE_SIZE
                    )
                    
                    traktMovies.mapNotNull { traktMovie ->
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
                                category = category,
                                releasesResponse = releasesResponse
                            )
                        } catch (e: Exception) {
                            Timber.w(e, "Failed to fetch TMDB data for movie: ${traktMovie.title}")
                            // Fallback to Trakt data only
                            MovieMapper.mapTraktToMovie(traktMovie, category)
                        }
                    }
                }
                else -> throw IllegalArgumentException("Unknown category: $category")
            }
            
            // Store in database within transaction
            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    // Clear existing list entries for this category on refresh
                    listEntryDao.clearListEntries(category)
                    currentPage = 1
                }
                
                if (movies.isNotEmpty()) {
                    movieDao.insertMovies(movies)
                    
                    // Create list entries for the movies
                    val listEntries = movies.mapIndexed { index, movie ->
                        com.strmr.ai.data.database.entity.MovieListEntry(
                            movieId = movie.id,
                            listType = category,
                            position = (currentPage - 1) * PAGE_SIZE + index,
                            listUpdatedAt = System.currentTimeMillis()
                        )
                    }
                    listEntryDao.insertListEntries(listEntries)
                    currentPage = page
                }
            }
            
            val endOfPaginationReached = movies.isEmpty() || movies.size < PAGE_SIZE
            
            Timber.d("Loaded ${movies.size} $category movies for page $page. End of pagination: $endOfPaginationReached")
            
            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
            
        } catch (e: Exception) {
            Timber.e(e, "Error loading $category movies for page $currentPage")
            MediatorResult.Error(e)
        }
    }
    
    companion object {
        private const val CACHE_TIMEOUT_MS = 30 * 60 * 1000L // 30 minutes
    }
}