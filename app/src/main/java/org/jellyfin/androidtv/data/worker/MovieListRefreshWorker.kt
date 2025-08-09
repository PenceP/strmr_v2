package org.jellyfin.androidtv.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.data.repository.ImprovedMovieRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Background worker that refreshes movie lists every 24 hours
 * This only updates the list ordering, not the movie data itself
 */
class MovieListRefreshWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {
    
    private val movieRepository: ImprovedMovieRepository by inject()
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Timber.d("Starting movie list refresh worker")
            
            // Refresh trending list order
            movieRepository.getTrendingMovies(forceRefresh = true).collect { _ ->
                // Just trigger the refresh, we don't need to process the results here
            }
            
            // Refresh popular list order
            movieRepository.getPopularMovies(forceRefresh = true).collect { _ ->
                // Just trigger the refresh
            }
            
            // Optional: Clean up very old cached movies (60+ days)
            movieRepository.cleanupOldCache()
            
            // Log cache statistics
            val stats = movieRepository.getCacheStats()
            Timber.d("Cache stats - Total movies: ${stats.totalCachedMovies}, " +
                    "Stale: ${stats.staleMovies}, " +
                    "Trending age: ${stats.trendingListAge / 1000 / 60 / 60}h, " +
                    "Popular age: ${stats.popularListAge / 1000 / 60 / 60}h")
            
            Timber.d("Movie list refresh completed successfully")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh movie lists")
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
    
    companion object {
        private const val WORK_NAME = "movie_list_refresh"
        
        /**
         * Schedule the periodic work to refresh lists every 24 hours
         */
        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<MovieListRefreshWorker>(
                24, TimeUnit.HOURS,
                1, TimeUnit.HOURS // Flex interval
            )
                .addTag(WORK_NAME)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
                workRequest
            )
            
            Timber.d("Movie list refresh worker scheduled")
        }
        
        /**
         * Cancel the scheduled work
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Timber.d("Movie list refresh worker cancelled")
        }
    }
}