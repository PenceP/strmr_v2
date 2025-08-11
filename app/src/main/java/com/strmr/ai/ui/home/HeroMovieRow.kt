package com.strmr.ai.ui.home

import android.content.Context
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Row
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import com.strmr.ai.data.database.entity.Movie
import com.strmr.ai.data.repository.MovieRepository
import com.strmr.ai.ui.presentation.CardPresenter
import com.strmr.ai.ui.presentation.MutableObjectAdapter
import timber.log.Timber

/**
 * Hero section that displays a featured movie with backdrop, title, description, etc.
 * Similar to the Jellyfin hero section with large backdrop image and movie details.
 */
class HeroMovieRow(
    private val lifecycleOwner: LifecycleOwner,
    private val movieRepository: MovieRepository
) : HomeFragmentRow {
    
    private var adapter: MutableObjectAdapter<HeroMovieItem>? = null
    private val heroUpdateManager = HeroUpdateManager.getInstance()
    
    override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
        Timber.d("Adding HeroMovieRow")
        
        // Create the adapter for this row with custom presenter
        val heroPresenter = HeroMoviePresenter()
        val itemAdapter = MutableObjectAdapter<HeroMovieItem>(heroPresenter)
        
        // Create row without header (full width)
        val row = ListRow(null, itemAdapter) // No header for hero section
        
        // Store reference
        adapter = itemAdapter
        
        // Add row to main adapter at the top
        rowsAdapter.add(0, row)
        
        // Load initial featured movie
        loadInitialFeaturedMovie()
        
        // Listen for hero updates from focused movie cards
        observeHeroUpdates()
    }
    
    private fun observeHeroUpdates() {
        lifecycleOwner.lifecycleScope.launch {
            heroUpdateManager.currentHeroMovie
                .filterNotNull()
                .collect { movie ->
                    Timber.d("Hero section updating with movie: ${movie.title}")
                    updateHeroMovie(movie)
                }
        }
    }
    
    private fun updateHeroMovie(movie: Movie) {
        adapter?.clear()
        adapter?.add(HeroMovieItem(movie))
    }
    
    private fun loadInitialFeaturedMovie() {
        lifecycleOwner.lifecycleScope.launch {
            try {
                // Get a trending movie to feature
                val movies = movieRepository.getMoviesByCategorySync(MovieRepository.CATEGORY_TRENDING, 1)
                
                if (movies.isNotEmpty()) {
                    val featuredMovie = movies.first()
                    Timber.d("Loaded featured movie: ${featuredMovie.title}")
                    
                    // Clear and add featured movie
                    adapter?.clear()
                    adapter?.add(HeroMovieItem(featuredMovie))
                } else {
                    // Try popular movies if no trending
                    val popularMovies = movieRepository.getMoviesByCategorySync(MovieRepository.CATEGORY_POPULAR, 1)
                    if (popularMovies.isNotEmpty()) {
                        val featuredMovie = popularMovies.first()
                        Timber.d("Loaded featured movie from popular: ${featuredMovie.title}")
                        
                        adapter?.clear()
                        adapter?.add(HeroMovieItem(featuredMovie))
                    }
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to load featured movie")
            }
        }
    }
}

/**
 * Data class for hero movie item
 */
data class HeroMovieItem(
    val movie: Movie
)