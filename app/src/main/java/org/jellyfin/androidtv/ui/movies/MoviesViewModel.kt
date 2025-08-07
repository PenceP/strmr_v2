package org.jellyfin.androidtv.ui.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.data.database.entity.Movie
import org.jellyfin.androidtv.data.repository.MovieRepository
import timber.log.Timber

class MoviesViewModel(
    private val movieRepository: MovieRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MoviesUiState())
    val uiState: StateFlow<MoviesUiState> = _uiState.asStateFlow()
    
    private val _events = MutableSharedFlow<MoviesEvent>()
    val events: Flow<MoviesEvent> = _events.asSharedFlow()
    
    // Cached paging data flows
    val trendingMovies: Flow<PagingData<Movie>> = movieRepository.getTrendingMovies()
        .cachedIn(viewModelScope)
        
    val popularMovies: Flow<PagingData<Movie>> = movieRepository.getPopularMovies()
        .cachedIn(viewModelScope)
    
    init {
        Timber.d("MoviesViewModel initialized")
    }
    
    fun onMovieSelected(movie: Movie) {
        Timber.d("Movie selected: ${movie.title}")
        viewModelScope.launch {
            _events.emit(MoviesEvent.NavigateToMovieDetails(movie.id))
        }
    }
    
    fun onRefreshTrending() {
        Timber.d("Refreshing trending movies")
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        
        viewModelScope.launch {
            try {
                movieRepository.refreshMovies(MovieRepository.CATEGORY_TRENDING)
                    .collect { result ->
                        result.fold(
                            onSuccess = {
                                _uiState.value = _uiState.value.copy(
                                    isRefreshing = false,
                                    error = null
                                )
                                _events.emit(MoviesEvent.ShowMessage("Trending movies refreshed"))
                            },
                            onFailure = { error ->
                                _uiState.value = _uiState.value.copy(
                                    isRefreshing = false,
                                    error = error.message
                                )
                                _events.emit(MoviesEvent.ShowError("Failed to refresh trending movies"))
                                Timber.e(error, "Failed to refresh trending movies")
                            }
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = e.message
                )
                _events.emit(MoviesEvent.ShowError("Failed to refresh trending movies"))
                Timber.e(e, "Failed to refresh trending movies")
            }
        }
    }
    
    fun onRefreshPopular() {
        Timber.d("Refreshing popular movies")
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        
        viewModelScope.launch {
            try {
                movieRepository.refreshMovies(MovieRepository.CATEGORY_POPULAR)
                    .collect { result ->
                        result.fold(
                            onSuccess = {
                                _uiState.value = _uiState.value.copy(
                                    isRefreshing = false,
                                    error = null
                                )
                                _events.emit(MoviesEvent.ShowMessage("Popular movies refreshed"))
                            },
                            onFailure = { error ->
                                _uiState.value = _uiState.value.copy(
                                    isRefreshing = false,
                                    error = error.message
                                )
                                _events.emit(MoviesEvent.ShowError("Failed to refresh popular movies"))
                                Timber.e(error, "Failed to refresh popular movies")
                            }
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = e.message
                )
                _events.emit(MoviesEvent.ShowError("Failed to refresh popular movies"))
                Timber.e(e, "Failed to refresh popular movies")
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class MoviesUiState(
    val isRefreshing: Boolean = false,
    val error: String? = null
)

sealed class MoviesEvent {
    data class NavigateToMovieDetails(val movieId: Int) : MoviesEvent()
    data class ShowMessage(val message: String) : MoviesEvent()
    data class ShowError(val message: String) : MoviesEvent()
}