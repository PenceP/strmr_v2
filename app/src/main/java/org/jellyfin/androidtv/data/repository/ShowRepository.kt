package org.jellyfin.androidtv.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.data.api.service.TmdbApiService
import org.jellyfin.androidtv.data.api.service.TraktApiService
import org.jellyfin.androidtv.data.database.AppDatabase
import org.jellyfin.androidtv.data.database.entity.Show
import org.jellyfin.androidtv.data.database.entity.ShowListEntry
import org.jellyfin.androidtv.data.database.entity.ShowCastMember
import org.jellyfin.androidtv.data.mapper.ShowMapper
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Repository that manages TV show data from Trakt and TMDB
 * - List orders refresh every 24 hours
 * - Show media data caches for 30 days
 * - Images are handled by Coil's cache separately
 */
class ShowRepository(
    private val traktApiService: TraktApiService,
    private val tmdbApiService: TmdbApiService,
    private val database: AppDatabase
) {
    private val showDao = database.showDao()
    private val listEntryDao = database.showListEntryDao()
    private val castDao = database.castDao()
    
    companion object {
        const val PAGE_SIZE = 20
        const val LIST_TYPE_TRENDING = "trending"
        const val LIST_TYPE_POPULAR = "popular"
        
        // Cache durations
        private val LIST_CACHE_DURATION = TimeUnit.HOURS.toMillis(24) // 24 hours for list order
        private val MEDIA_CACHE_DURATION = TimeUnit.DAYS.toMillis(30) // 30 days for show data
    }
    
    /**
     * Get trending shows, refreshing list order if needed
     */
    fun getTrendingShows(forceRefresh: Boolean = false): Flow<List<Show>> = flow {
        // Check if list needs refresh (24hr timeout)
        if (forceRefresh || isListStale(LIST_TYPE_TRENDING)) {
            refreshListOrder(LIST_TYPE_TRENDING)
        }
        
        // Emit shows from database
        listEntryDao.getShowsForListFlow(LIST_TYPE_TRENDING).collect { shows ->
            emit(shows)
        }
    }
    
    /**
     * Get popular shows, refreshing list order if needed
     */
    fun getPopularShows(forceRefresh: Boolean = false): Flow<List<Show>> = flow {
        // Check if list needs refresh (24hr timeout)
        if (forceRefresh || isListStale(LIST_TYPE_POPULAR)) {
            refreshListOrder(LIST_TYPE_POPULAR)
        }
        
        // Emit shows from database
        listEntryDao.getShowsForListFlow(LIST_TYPE_POPULAR).collect { shows ->
            emit(shows)
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
     * Check if show media data is stale (older than 30 days)
     */
    private fun isShowDataStale(show: Show?): Boolean {
        if (show == null) return true
        val staleThreshold = System.currentTimeMillis() - MEDIA_CACHE_DURATION
        return show.mediaDataCachedAt < staleThreshold
    }
    
    /**
     * Refresh only the list order without re-fetching all show data
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
        val traktShows = traktApiService.getTrendingShows(
            clientId = BuildConfig.TRAKT_CLIENT_ID,
            page = 1,
            limit = PAGE_SIZE * 2 // Get more to have a good list
        )
        
        val currentTime = System.currentTimeMillis()
        val newListEntries = mutableListOf<ShowListEntry>()
        
        traktShows.forEachIndexed { index, traktResponse ->
            val showId = traktResponse.show.ids.tmdb 
            if (showId == null) return@forEachIndexed
            
            // Check if we have this show cached
            var existingShow = showDao.getShowById(showId)
            
            // If show doesn't exist or data is stale (30 days), fetch fresh data
            if (isShowDataStale(existingShow)) {
                try {
                    val tmdbShow = tmdbApiService.getShowDetails(
                        showId = showId,
                        authorization = "Bearer ${BuildConfig.TMDB_ACCESS_TOKEN}",
                        appendToResponse = "content_ratings"
                    )
                    
                    val show = ShowMapper.mapTraktAndTmdbToShow(
                        traktResponse = traktResponse,
                        tmdbShowDetails = tmdbShow,
                        category = LIST_TYPE_TRENDING
                    )
                    
                    // Update show in database
                    showDao.insertShow(show)
                    existingShow = show
                    
                    // Also fetch cast data for this show
                    fetchAndCacheShowCast(showId)
                    
                    Timber.d("Fetched fresh data for show: ${show.name}")
                } catch (e: Exception) {
                    Timber.w(e, "Failed to fetch TMDB data for show ID: $showId")
                    // If we can't fetch TMDB data, still add to list with basic Trakt data
                    if (existingShow == null) {
                        val basicShow = ShowMapper.mapTraktAndTmdbToShow(
                            traktResponse = traktResponse,
                            tmdbShowDetails = null,
                            category = LIST_TYPE_TRENDING
                        )
                        showDao.insertShow(basicShow)
                        existingShow = basicShow
                    }
                }
            } else {
                // Update last accessed timestamp for existing show
                showDao.updateLastAccessed(showId, currentTime)
            }
            
            // Add to list entry
            newListEntries.add(
                ShowListEntry(
                    showId = showId,
                    listType = LIST_TYPE_TRENDING,
                    position = index,
                    listUpdatedAt = currentTime
                )
            )
        }
        
        // Replace the entire list
        listEntryDao.replaceListEntries(LIST_TYPE_TRENDING, newListEntries)
        Timber.d("Updated trending shows list with ${newListEntries.size} entries")
    }
    
    private suspend fun refreshPopularList() {
        val traktShows = traktApiService.getPopularShows(
            clientId = BuildConfig.TRAKT_CLIENT_ID,
            page = 1,
            limit = PAGE_SIZE * 2 // Get more to have a good list
        )
        
        val currentTime = System.currentTimeMillis()
        val newListEntries = mutableListOf<ShowListEntry>()
        
        traktShows.forEachIndexed { index, traktShow ->
            val showId = traktShow.ids.tmdb 
            if (showId == null) return@forEachIndexed
            
            // Check if we have this show cached
            var existingShow = showDao.getShowById(showId)
            
            // If show doesn't exist or data is stale (30 days), fetch fresh data
            if (isShowDataStale(existingShow)) {
                try {
                    val tmdbShow = tmdbApiService.getShowDetails(
                        showId = showId,
                        authorization = "Bearer ${BuildConfig.TMDB_ACCESS_TOKEN}",
                        appendToResponse = "content_ratings"
                    )
                    
                    // Create a fake TraktShowResponse for the mapper
                    val fakeResponse = org.jellyfin.androidtv.data.api.model.trakt.TraktShowResponse(
                        show = traktShow
                    )
                    
                    val show = ShowMapper.mapTraktAndTmdbToShow(
                        traktResponse = fakeResponse,
                        tmdbShowDetails = tmdbShow,
                        category = LIST_TYPE_POPULAR
                    )
                    
                    // Update show in database
                    showDao.insertShow(show)
                    existingShow = show
                    
                    // Also fetch cast data for this show
                    fetchAndCacheShowCast(showId)
                    
                    Timber.d("Fetched fresh data for show: ${show.name}")
                } catch (e: Exception) {
                    Timber.w(e, "Failed to fetch TMDB data for show ID: $showId")
                    // If we can't fetch TMDB data, still add to list with basic Trakt data
                    if (existingShow == null) {
                        val basicShow = ShowMapper.mapTraktToShow(
                            traktShow = traktShow,
                            category = LIST_TYPE_POPULAR
                        )
                        showDao.insertShow(basicShow)
                        existingShow = basicShow
                    }
                }
            } else {
                // Update last accessed timestamp for existing show
                showDao.updateLastAccessed(showId, currentTime)
            }
            
            // Add to list entry
            newListEntries.add(
                ShowListEntry(
                    showId = showId,
                    listType = LIST_TYPE_POPULAR,
                    position = index,
                    listUpdatedAt = currentTime
                )
            )
        }
        
        // Replace the entire list
        listEntryDao.replaceListEntries(LIST_TYPE_POPULAR, newListEntries)
        Timber.d("Updated popular shows list with ${newListEntries.size} entries")
    }
    
    /**
     * Get a specific show by TMDB ID
     */
    suspend fun getShowByTmdbId(tmdbId: Int): Show? {
        return showDao.getShowById(tmdbId)
    }
    
    /**
     * Get a specific show by Trakt ID
     */
    suspend fun getShowByTraktId(traktId: Int): Show? {
        return showDao.getShowByTraktId(traktId)
    }
    
    /**
     * Get cast for a specific show by TMDB ID
     */
    suspend fun getShowCast(showId: Int): List<ShowCastMember> {
        return castDao.getCastForShow(showId)
    }
    
    /**
     * Fetch and cache cast data for a show
     */
    private suspend fun fetchAndCacheShowCast(showId: Int) {
        try {
            val creditsResponse = tmdbApiService.getShowCredits(
                showId = showId,
                authorization = "Bearer ${BuildConfig.TMDB_ACCESS_TOKEN}"
            )
            
            // Map cast members (limit to top 20 for performance)
            val castMembers = creditsResponse.cast.take(20).map { tmdbCast ->
                ShowCastMember(
                    showId = showId,
                    tmdbCreditId = tmdbCast.credit_id,
                    personId = tmdbCast.id,
                    name = tmdbCast.name,
                    character = tmdbCast.character,
                    order = tmdbCast.order,
                    profilePath = tmdbCast.profile_path,
                    department = "Acting",
                    job = null
                )
            }
            
            // Add key crew members (creator, executive producer, producer)
            val keyCrewMembers = creditsResponse.crew
                .filter { crew ->
                    crew.job in listOf("Creator", "Executive Producer", "Producer", "Director", "Writer")
                }
                .take(10) // Limit crew members
                .mapIndexed { index, tmdbCrew ->
                    ShowCastMember(
                        showId = showId,
                        tmdbCreditId = tmdbCrew.credit_id,
                        personId = tmdbCrew.id,
                        name = tmdbCrew.name,
                        character = null,
                        order = 1000 + index, // Put crew after cast
                        profilePath = tmdbCrew.profile_path,
                        department = tmdbCrew.department,
                        job = tmdbCrew.job
                    )
                }
            
            // Save all cast and crew to database
            castDao.replaceShowCast(showId, castMembers + keyCrewMembers)
            
        } catch (e: Exception) {
            Timber.w(e, "Failed to fetch cast for show ID: $showId")
        }
    }
}