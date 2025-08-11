package com.strmr.ai.ui.home

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.strmr.ai.data.database.entity.Movie
import com.strmr.ai.data.database.entity.Show

/**
 * Manages updates to the hero section based on focused movie and show cards
 */
class HeroUpdateManager {
    private val _currentHeroMovie = MutableStateFlow<Movie?>(null)
    val currentHeroMovie: StateFlow<Movie?> = _currentHeroMovie.asStateFlow()
    
    private val _currentHeroShow = MutableStateFlow<Show?>(null)
    val currentHeroShow: StateFlow<Show?> = _currentHeroShow.asStateFlow()
    
    fun updateHeroMovie(movie: Movie?) {
        _currentHeroMovie.value = movie
        _currentHeroShow.value = null // Clear show when movie is selected
    }
    
    fun updateHeroShow(show: Show?) {
        _currentHeroShow.value = show
        _currentHeroMovie.value = null // Clear movie when show is selected
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