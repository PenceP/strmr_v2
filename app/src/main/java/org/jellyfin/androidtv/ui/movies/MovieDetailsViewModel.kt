package org.jellyfin.androidtv.ui.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.data.api.service.TmdbApiService
import org.jellyfin.androidtv.data.database.entity.Movie
import org.jellyfin.androidtv.data.repository.MovieRepository
import timber.log.Timber

class MovieDetailsViewModel(
    private val movieRepository: MovieRepository,
    private val movieId: Int
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MovieDetailsUiState())
    val uiState: StateFlow<MovieDetailsUiState> = _uiState.asStateFlow()
    
    private val _events = MutableSharedFlow<MovieDetailsEvent>()
    val events = _events.asSharedFlow()
    
    init {
        Timber.d("MovieDetailsViewModel initialized for movie ID: $movieId")
        loadMovieDetails()
    }
    
    private fun loadMovieDetails() {
        Timber.d("Loading movie details for ID: $movieId")
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        viewModelScope.launch {
            try {
                movieRepository.getMovieByIdFlow(movieId).collect { movie ->
                    _uiState.value = _uiState.value.copy(
                        movie = movie,
                        isLoading = false,
                        error = null
                    )
                    Timber.d("Movie details loaded: ${movie?.title}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load movie details"
                )
                Timber.e(e, "Failed to load movie details for ID: $movieId")
            }
        }
    }
    
    fun onPlayMovie() {
        val movie = _uiState.value.movie
        if (movie != null) {
            Timber.d("Play movie requested: ${movie.title}")
            viewModelScope.launch {
                // For now, just show a message since we're not implementing playback yet
                _events.emit(MovieDetailsEvent.ShowMessage("Playback not yet implemented"))
            }
        }
    }
    
    fun onAddToWatchlist() {
        val movie = _uiState.value.movie
        if (movie != null) {
            Timber.d("Add to watchlist requested: ${movie.title}")
            viewModelScope.launch {
                // For now, just show a message since watchlist isn't implemented yet
                _events.emit(MovieDetailsEvent.ShowMessage("Watchlist not yet implemented"))
            }
        }
    }
    
    fun onShareMovie() {
        val movie = _uiState.value.movie
        if (movie != null) {
            Timber.d("Share movie requested: ${movie.title}")
            viewModelScope.launch {
                _events.emit(MovieDetailsEvent.ShareMovie(movie.title, movie.overview ?: ""))
            }
        }
    }
    
    fun onRetry() {
        loadMovieDetails()
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun getPosterUrl(): String? {
        return _uiState.value.movie?.posterPath?.let { path ->
            TmdbApiService.getPosterUrl(path, TmdbApiService.POSTER_SIZE_W500)
        }
    }
    
    fun getBackdropUrl(): String? {
        return _uiState.value.movie?.backdropPath?.let { path ->
            TmdbApiService.getBackdropUrl(path, TmdbApiService.BACKDROP_SIZE_W1280)
        }
    }
}

data class MovieDetailsUiState(
    val movie: Movie? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class MovieDetailsEvent {
    data class ShowMessage(val message: String) : MovieDetailsEvent()
    data class ShowError(val message: String) : MovieDetailsEvent()
    data class ShareMovie(val title: String, val overview: String) : MovieDetailsEvent()
}