package org.jellyfin.androidtv.ui.home

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jellyfin.androidtv.data.database.entity.Movie

/**
 * Manages updates to the hero section based on focused movie cards
 */
class HeroUpdateManager {
    private val _currentHeroMovie = MutableStateFlow<Movie?>(null)
    val currentHeroMovie: StateFlow<Movie?> = _currentHeroMovie.asStateFlow()
    
    fun updateHeroMovie(movie: Movie?) {
        _currentHeroMovie.value = movie
    }
    
    companion object {
        @Volatile
        private var INSTANCE: HeroUpdateManager? = null
        
        fun getInstance(): HeroUpdateManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HeroUpdateManager().also { INSTANCE = it }
            }
        }
    }
}