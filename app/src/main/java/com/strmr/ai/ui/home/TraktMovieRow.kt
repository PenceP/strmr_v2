package com.strmr.ai.ui.home

import android.content.Context
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Row
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.strmr.ai.constant.ImageType
import com.strmr.ai.data.database.entity.Movie
import com.strmr.ai.data.repository.MovieRepository
import com.strmr.ai.ui.itemhandling.BaseRowItem
import com.strmr.ai.ui.itemhandling.BaseRowItemSelectAction
import com.strmr.ai.ui.itemhandling.BaseRowType
import com.strmr.ai.ui.presentation.CardPresenter
import com.strmr.ai.ui.presentation.MutableObjectAdapter
import com.strmr.ai.util.ImageHelper
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID

class TraktMovieRow(
    private val lifecycleOwner: LifecycleOwner,
    private val title: String,
    private val movieRepository: MovieRepository,
    private val category: String
) : HomeFragmentRow {
    
    private var adapter: MutableObjectAdapter<BaseRowItem>? = null
    private val heroUpdateManager = HeroUpdateManager.getInstance()
    
    override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
        Timber.d("Adding TraktMovieRow: $title")
        
        // Create the adapter for this row
        val itemAdapter = MutableObjectAdapter<BaseRowItem>(cardPresenter)
        val headerItem = HeaderItem(title)
        val row = ListRow(headerItem, itemAdapter)
        
        // Store reference
        adapter = itemAdapter
        
        // Add row to main adapter
        rowsAdapter.add(row)
        
        // Load initial data
        loadMovies()
    }
    
    private fun loadMovies() {
        lifecycleOwner.lifecycleScope.launch {
            try {
                // Get movies from database (sync method for initial load)
                val movies = movieRepository.getMoviesByCategorySync(category, 20)
                
                Timber.d("Loaded ${movies.size} movies for $title")
                
                // Clear existing items
                adapter?.clear()
                
                // Add movies as BaseRowItems
                movies.forEach { movie ->
                    val movieItem = MovieRowItem(movie)
                    adapter?.add(movieItem)
                }
                
                // Trigger refresh from network if no local data
                if (movies.isEmpty()) {
                    movieRepository.refreshMovies(category)
                        .collect { result ->
                            result.onSuccess {
                                // Reload from database after refresh
                                val refreshedMovies = movieRepository.getMoviesByCategorySync(category, 20)
                                
                                adapter?.clear()
                                refreshedMovies.forEach { movie ->
                                    val movieItem = MovieRowItem(movie)
                                    adapter?.add(movieItem)
                                }
                                
                                Timber.d("Refreshed ${refreshedMovies.size} movies for $title")
                            }.onFailure { error ->
                                Timber.e(error, "Failed to refresh movies for $title")
                            }
                        }
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to load movies for $title")
            }
        }
    }
}

/**
 * Custom BaseRowItem for our Movie entities
 */
class MovieRowItem(
    private val movie: Movie
) : BaseRowItem(
    baseRowType = BaseRowType.BaseItem,
    staticHeight = false,
    preferParentThumb = false,
    selectAction = BaseRowItemSelectAction.ShowDetails,
    baseItem = MovieRowItem.createBaseItemDtoFromMovie(movie)
) {
    
    override val itemId: UUID = UUID.nameUUIDFromBytes(movie.id.toString().toByteArray())
    
    override fun getFullName(context: Context): String = movie.title
    
    override fun getName(context: Context): String = movie.title
    
    override fun getSubText(context: Context): String? {
        return when {
            movie.releaseDate != null -> movie.releaseDate
            movie.voteAverage > 0 -> "★ %.1f".format(movie.voteAverage)
            else -> null
        }
    }
    
    override fun getImageUrl(
        context: Context,
        imageHelper: ImageHelper,
        imageType: ImageType,
        fillWidth: Int,
        fillHeight: Int
    ): String? {
        return movie.posterPath?.let { path ->
            "https://image.tmdb.org/t/p/w500$path"
        }
    }
    
    override fun getCardName(context: Context): String = movie.title
    
    override fun getSummary(context: Context): String? = movie.overview
    
    fun getMovie(): Movie = movie
    
    companion object {
        /**
         * Creates a minimal BaseItemDto from our Movie entity for compatibility with CardPresenter
         */
        fun createBaseItemDtoFromMovie(movie: Movie): BaseItemDto {
            return BaseItemDto(
                id = UUID.nameUUIDFromBytes(movie.id.toString().toByteArray()),
                name = movie.title,
                originalTitle = movie.originalTitle,
                overview = movie.overview,
                type = BaseItemKind.MOVIE,
                communityRating = movie.voteAverage.takeIf { it > 0.0 }?.toFloat(),
                primaryImageAspectRatio = 2.0 / 3.0, // Standard movie poster ratio
                premiereDate = movie.releaseDate?.let { dateStr ->
                    try {
                        // Parse release date string to LocalDateTime
                        LocalDateTime.parse("${dateStr}T00:00:00")
                    } catch (e: Exception) {
                        null
                    }
                },
                productionYear = movie.releaseDate?.let { dateStr ->
                    try {
                        dateStr.substring(0, 4).toIntOrNull()
                    } catch (e: Exception) {
                        null
                    }
                },
            )
        }
    }
}