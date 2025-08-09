package org.jellyfin.androidtv.ui.movies

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.data.database.entity.Movie
import org.jellyfin.androidtv.data.repository.ImprovedMovieRepository
import org.jellyfin.androidtv.ui.home.HeroUpdateManager
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.MovieRowItem
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.androidtv.ui.presentation.PositionableListRowPresenter
import org.koin.android.ext.android.inject
import timber.log.Timber

class MoviesRowsFragment : RowsSupportFragment(), View.OnKeyListener {
	private val movieRepository by inject<ImprovedMovieRepository>()
	
	private val heroUpdateManager = HeroUpdateManager.getInstance()

	// Data
	private var currentItem: BaseRowItem? = null
	private var currentRow: ListRow? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		adapter = MutableObjectAdapter<Row>(PositionableListRowPresenter())

		// Setup item selection listener for hero updates
		onItemViewSelectedListener = OnItemViewSelectedListener { _, item, _, _ ->
			currentItem = item as? BaseRowItem
			currentRow = null
			
			// Update hero overlay when movie is selected
			if (item is MovieRowItem) {
				heroUpdateManager.updateHeroMovie(item.movie)
				Timber.d("Updated hero with movie: ${item.movie.title}")
			}
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		view.setOnKeyListener(this)
		
		// Setup movie rows after view is created so viewLifecycleOwner is available
		setupMovieRows()
	}

	private fun setupMovieRows() {
		lifecycleScope.launch {
			try {
				// Add trending movies row
				addTrendingMoviesRow()
				
				// Add popular movies row  
				addPopularMoviesRow()
				
			} catch (e: Exception) {
				Timber.e(e, "Failed to setup movie rows")
			}
		}
	}

	private suspend fun addTrendingMoviesRow() {
		try {
			val headerItem = HeaderItem(0, "Trending Movies")
			val itemsAdapter = ArrayObjectAdapter(CardPresenter())
			val listRow = ListRow(headerItem, itemsAdapter)
			
			// Add row to adapter immediately
			(adapter as MutableObjectAdapter<Row>).add(listRow)
			
			// Observe trending movies
			movieRepository.getTrendingMovies()
				.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
				.onEach { movies ->
					Timber.d("Received ${movies.size} trending movies")
					
					// Clear and add new items
					itemsAdapter.clear()
					movies.forEach { movie ->
						itemsAdapter.add(MovieRowItem(movie))
					}
					
					// Set first movie as hero if no current hero
					if (movies.isNotEmpty() && heroUpdateManager.currentHeroMovie.value == null) {
						heroUpdateManager.updateHeroMovie(movies.first())
					}
				}
				.launchIn(lifecycleScope)
				
		} catch (e: Exception) {
			Timber.e(e, "Failed to add trending movies row")
		}
	}

	private suspend fun addPopularMoviesRow() {
		try {
			val headerItem = HeaderItem(1, "Popular Movies")
			val itemsAdapter = ArrayObjectAdapter(CardPresenter())
			val listRow = ListRow(headerItem, itemsAdapter)
			
			// Add row to adapter immediately
			(adapter as MutableObjectAdapter<Row>).add(listRow)
			
			// Observe popular movies
			movieRepository.getPopularMovies()
				.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
				.onEach { movies ->
					Timber.d("Received ${movies.size} popular movies")
					
					// Clear and add new items
					itemsAdapter.clear()
					movies.forEach { movie ->
						itemsAdapter.add(MovieRowItem(movie))
					}
				}
				.launchIn(lifecycleScope)
				
		} catch (e: Exception) {
			Timber.e(e, "Failed to add popular movies row")
		}
	}

	override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
		if (event?.action != KeyEvent.ACTION_DOWN) return false
		
		return when (keyCode) {
			KeyEvent.KEYCODE_DPAD_RIGHT,
			KeyEvent.KEYCODE_DPAD_LEFT,
			KeyEvent.KEYCODE_DPAD_UP,
			KeyEvent.KEYCODE_DPAD_DOWN -> {
				// Let the fragment handle navigation
				false
			}
			else -> false
		}
	}
}