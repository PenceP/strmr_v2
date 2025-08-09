package org.jellyfin.androidtv.ui.tv

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
import org.jellyfin.androidtv.data.database.entity.Show
import org.jellyfin.androidtv.data.repository.ShowRepository
import org.jellyfin.androidtv.ui.home.HeroUpdateManager
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.ShowRowItem
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.androidtv.ui.presentation.PositionableListRowPresenter
import org.koin.android.ext.android.inject
import timber.log.Timber

class TVShowsRowsFragment : RowsSupportFragment(), View.OnKeyListener {
	private val showRepository by inject<ShowRepository>()
	
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
			
			// Update hero overlay when show is selected
			if (item is ShowRowItem) {
				heroUpdateManager.updateHeroShow(item.show)
				Timber.d("Updated hero with show: ${item.show.name}")
			}
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		view.setOnKeyListener(this)
		
		// Setup show rows after view is created so viewLifecycleOwner is available
		setupShowRows()
	}

	private fun setupShowRows() {
		lifecycleScope.launch {
			try {
				// Add trending shows row
				addTrendingShowsRow()
				
				// Add popular shows row  
				addPopularShowsRow()
				
			} catch (e: Exception) {
				Timber.e(e, "Failed to setup show rows")
			}
		}
	}

	private suspend fun addTrendingShowsRow() {
		try {
			val headerItem = HeaderItem(0, "Trending TV Shows")
			val itemsAdapter = ArrayObjectAdapter(CardPresenter())
			val listRow = ListRow(headerItem, itemsAdapter)
			
			// Add row to adapter immediately
			(adapter as MutableObjectAdapter<Row>).add(listRow)
			
			// Observe trending shows
			showRepository.getTrendingShows()
				.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
				.onEach { shows ->
					Timber.d("Received ${shows.size} trending shows")
					
					// Clear and add new items
					itemsAdapter.clear()
					shows.forEach { show ->
						itemsAdapter.add(ShowRowItem(show))
					}
					
					// Set first show as hero if no current hero
					if (shows.isNotEmpty() && heroUpdateManager.currentHeroShow.value == null) {
						heroUpdateManager.updateHeroShow(shows.first())
					}
				}
				.launchIn(lifecycleScope)
				
		} catch (e: Exception) {
			Timber.e(e, "Failed to add trending shows row")
		}
	}

	private suspend fun addPopularShowsRow() {
		try {
			val headerItem = HeaderItem(1, "Popular TV Shows")
			val itemsAdapter = ArrayObjectAdapter(CardPresenter())
			val listRow = ListRow(headerItem, itemsAdapter)
			
			// Add row to adapter immediately
			(adapter as MutableObjectAdapter<Row>).add(listRow)
			
			// Observe popular shows
			showRepository.getPopularShows()
				.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
				.onEach { shows ->
					Timber.d("Received ${shows.size} popular shows")
					
					// Clear and add new items
					itemsAdapter.clear()
					shows.forEach { show ->
						itemsAdapter.add(ShowRowItem(show))
					}
				}
				.launchIn(lifecycleScope)
				
		} catch (e: Exception) {
			Timber.e(e, "Failed to add popular shows row")
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