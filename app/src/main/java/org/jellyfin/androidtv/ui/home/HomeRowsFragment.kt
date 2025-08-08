package org.jellyfin.androidtv.ui.home

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.constant.CustomMessage
import org.jellyfin.androidtv.constant.HomeSectionType
import org.jellyfin.androidtv.data.model.DataRefreshService
import org.jellyfin.androidtv.data.repository.CustomMessageRepository
import org.jellyfin.androidtv.data.repository.NotificationsRepository
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.androidtv.ui.browsing.CompositeClickedListener
import org.jellyfin.androidtv.ui.browsing.CompositeSelectedListener
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.ui.itemhandling.refreshItem
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.playback.AudioEventListener
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.androidtv.ui.presentation.PositionableListRowPresenter
import org.jellyfin.androidtv.util.KeyProcessor
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.liveTvApi
import org.jellyfin.sdk.api.sockets.subscribe
import org.jellyfin.sdk.model.api.LibraryChangedMessage
import org.jellyfin.sdk.model.api.UserDataChangedMessage
import org.jellyfin.androidtv.data.repository.MovieRepository
import org.koin.android.ext.android.inject
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

class HomeRowsFragment : RowsSupportFragment(), AudioEventListener, View.OnKeyListener {
	private val api by inject<ApiClient>()
	private val backgroundService by inject<BackgroundService>()
	private val playbackManager by inject<PlaybackManager>()
	private val mediaManager by inject<MediaManager>()
	private val notificationsRepository by inject<NotificationsRepository>()
	private val userRepository by inject<UserRepository>()
	private val userSettingPreferences by inject<UserSettingPreferences>()
	private val userViewsRepository by inject<UserViewsRepository>()
	private val dataRefreshService by inject<DataRefreshService>()
	private val customMessageRepository by inject<CustomMessageRepository>()
	private val navigationRepository by inject<NavigationRepository>()
	private val itemLauncher by inject<ItemLauncher>()
	private val keyProcessor by inject<KeyProcessor>()
	private val movieRepository by inject<MovieRepository>()

	private val helper by lazy { HomeFragmentHelper(requireContext(), userRepository) }

	// Data
	private var currentItem: BaseRowItem? = null
	private var currentRow: ListRow? = null
	private var justLoaded = true

	// Special rows
	private val notificationsRow by lazy { NotificationsHomeFragmentRow(lifecycleScope, notificationsRepository) }
	private val nowPlaying by lazy { HomeFragmentNowPlayingRow(lifecycleScope, playbackManager, mediaManager) }
	private val liveTVRow by lazy { HomeFragmentLiveTVRow(requireActivity(), userRepository, navigationRepository) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		adapter = MutableObjectAdapter<Row>(PositionableListRowPresenter())

		lifecycleScope.launch(Dispatchers.IO) {
			// Skip authentication-dependent logic for Trakt app
			try {
				// Check if user session exists
				val currentUser = userRepository.currentUser.value
				val hasSession = currentUser != null
				
				if (hasSession) {
					// Original Jellyfin logic when user is authenticated
					val homesections = userSettingPreferences.activeHomesections
					var includeLiveTvRows = false

					// Check for live TV support
					if (homesections.contains(HomeSectionType.LIVE_TV) && currentUser?.policy?.enableLiveTvAccess == true) {
						try {
							val recommendedPrograms by api.liveTvApi.getRecommendedPrograms(
								enableTotalRecordCount = false,
								imageTypeLimit = 1,
								isAiring = true,
								limit = 1,
							)
							includeLiveTvRows = recommendedPrograms.items.isNotEmpty()
						} catch (e: Exception) {
							Timber.w(e, "Failed to check live TV support")
							includeLiveTvRows = false
						}
					}

					// Make sure the rows are empty
					val rows = mutableListOf<HomeFragmentRow>()

					// Check for coroutine cancellation
					if (!isActive) return@launch

					// Actually add the sections
					for (section in homesections) when (section) {
						HomeSectionType.LATEST_MEDIA -> try {
							rows.add(helper.loadRecentlyAdded(userViewsRepository.views.first()))
						} catch (e: Exception) { 
							Timber.w(e, "Failed to load recently added")
						}
						HomeSectionType.LIBRARY_TILES_SMALL -> rows.add(HomeFragmentViewsRow(small = false))
						HomeSectionType.LIBRARY_BUTTONS -> rows.add(HomeFragmentViewsRow(small = true))
						HomeSectionType.RESUME -> try {
							rows.add(helper.loadResumeVideo())
						} catch (e: Exception) { 
							Timber.w(e, "Failed to load resume video")
						}
						HomeSectionType.RESUME_AUDIO -> try {
							rows.add(helper.loadResumeAudio())
						} catch (e: Exception) { 
							Timber.w(e, "Failed to load resume audio")
						}
						HomeSectionType.RESUME_BOOK -> Unit // Books are not (yet) supported
						HomeSectionType.ACTIVE_RECORDINGS -> try {
							rows.add(helper.loadLatestLiveTvRecordings())
						} catch (e: Exception) { 
							Timber.w(e, "Failed to load recordings")
						}
						HomeSectionType.NEXT_UP -> try {
							rows.add(helper.loadNextUp())
						} catch (e: Exception) { 
							Timber.w(e, "Failed to load next up")
						}
						HomeSectionType.LIVE_TV -> if (includeLiveTvRows) {
							rows.add(liveTVRow)
							try {
								rows.add(helper.loadOnNow())
							} catch (e: Exception) { 
								Timber.w(e, "Failed to load on now")
							}
						}

						HomeSectionType.NONE -> Unit
					}

					// Add sections to layout
					withContext(Dispatchers.Main) {
						val cardPresenter = CardPresenter()

						// Add rows in order
						notificationsRow.addToRowsAdapter(requireContext(), cardPresenter, adapter as MutableObjectAdapter<Row>)
						nowPlaying.addToRowsAdapter(requireContext(), cardPresenter, adapter as MutableObjectAdapter<Row>)
						
						// Add Trakt movie rows
						val trendingRow = TraktMovieRow(
							lifecycleOwner = this@HomeRowsFragment,
							title = "Trending Movies",
							movieRepository = movieRepository,
							category = MovieRepository.CATEGORY_TRENDING
						)
						trendingRow.addToRowsAdapter(requireContext(), cardPresenter, adapter as MutableObjectAdapter<Row>)
						
						val popularRow = TraktMovieRow(
							lifecycleOwner = this@HomeRowsFragment,
							title = "Popular Movies", 
							movieRepository = movieRepository,
							category = MovieRepository.CATEGORY_POPULAR
						)
						popularRow.addToRowsAdapter(requireContext(), cardPresenter, adapter as MutableObjectAdapter<Row>)
						
						for (row in rows) row.addToRowsAdapter(requireContext(), cardPresenter, adapter as MutableObjectAdapter<Row>)
					}
				} else {
					// No user session - show only Trakt movie rows
					Timber.d("No user session - showing only Trakt movie rows")
					
					withContext(Dispatchers.Main) {
						val cardPresenter = CardPresenter()

						// Add Trakt movie rows only
						val trendingRow = TraktMovieRow(
							lifecycleOwner = this@HomeRowsFragment,
							title = "Trending Movies",
							movieRepository = movieRepository,
							category = MovieRepository.CATEGORY_TRENDING
						)
						trendingRow.addToRowsAdapter(requireContext(), cardPresenter, adapter as MutableObjectAdapter<Row>)
						
						val popularRow = TraktMovieRow(
							lifecycleOwner = this@HomeRowsFragment,
							title = "Popular Movies", 
							movieRepository = movieRepository,
							category = MovieRepository.CATEGORY_POPULAR
						)
						popularRow.addToRowsAdapter(requireContext(), cardPresenter, adapter as MutableObjectAdapter<Row>)
					}
				}
			} catch (e: Exception) {
				Timber.e(e, "Failed to load home rows, falling back to Trakt only")
				
				// Fallback - show only Trakt movie rows
				withContext(Dispatchers.Main) {
					val cardPresenter = CardPresenter()

					val trendingRow = TraktMovieRow(
						lifecycleOwner = this@HomeRowsFragment,
						title = "Trending Movies",
						movieRepository = movieRepository,
						category = MovieRepository.CATEGORY_TRENDING
					)
					trendingRow.addToRowsAdapter(requireContext(), cardPresenter, adapter as MutableObjectAdapter<Row>)
					
					val popularRow = TraktMovieRow(
						lifecycleOwner = this@HomeRowsFragment,
						title = "Popular Movies", 
						movieRepository = movieRepository,
						category = MovieRepository.CATEGORY_POPULAR
					)
					popularRow.addToRowsAdapter(requireContext(), cardPresenter, adapter as MutableObjectAdapter<Row>)
				}
			}
		}

		onItemViewClickedListener = CompositeClickedListener().apply {
			registerListener(ItemViewClickedListener())
			registerListener(liveTVRow::onItemClicked)
			registerListener(notificationsRow::onItemClicked)
		}

		onItemViewSelectedListener = CompositeSelectedListener().apply {
			registerListener(ItemViewSelectedListener())
		}

		customMessageRepository.message
			.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
			.onEach { message ->
				when (message) {
					CustomMessage.RefreshCurrentItem -> refreshCurrentItem()
					else -> Unit
				}
			}.launchIn(lifecycleScope)

		lifecycleScope.launch {
			lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
				api.webSocket.subscribe<UserDataChangedMessage>()
					.onEach { refreshRows(force = true, delayed = false) }
					.launchIn(this)

				api.webSocket.subscribe<LibraryChangedMessage>()
					.onEach { refreshRows(force = true, delayed = false) }
					.launchIn(this)
			}
		}

		// Subscribe to Audio messages
		mediaManager.addAudioEventListener(this)
	}

	override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
		if (event?.action != KeyEvent.ACTION_UP) return false
		return keyProcessor.handleKey(keyCode, currentItem, activity)
	}

	override fun onResume() {
		super.onResume()

		//React to deletion
		if (currentRow != null && currentItem != null && currentItem?.baseItem != null && currentItem!!.baseItem!!.id == dataRefreshService.lastDeletedItemId) {
			(currentRow!!.adapter as ItemRowAdapter).remove(currentItem)
			currentItem = null
			dataRefreshService.lastDeletedItemId = null
		}

		if (!justLoaded) {
			//Re-retrieve anything that needs it but delay slightly so we don't take away gui landing
			refreshCurrentItem()
			refreshRows()
		} else {
			justLoaded = false
		}

		// Update audio queue
		Timber.i("Updating audio queue in HomeFragment (onResume)")
		nowPlaying.update(requireContext(), adapter as MutableObjectAdapter<Row>)
	}

	override fun onQueueStatusChanged(hasQueue: Boolean) {
		if (activity == null || requireActivity().isFinishing) return

		Timber.i("Updating audio queue in HomeFragment (onQueueStatusChanged)")
		nowPlaying.update(requireContext(), adapter as MutableObjectAdapter<Row>)
	}

	private fun refreshRows(force: Boolean = false, delayed: Boolean = true) {
		lifecycleScope.launch(Dispatchers.IO) {
			if (delayed) delay(1.5.seconds)

			repeat(adapter.size()) { i ->
				val rowAdapter = (adapter[i] as? ListRow)?.adapter as? ItemRowAdapter
				if (force) rowAdapter?.Retrieve()
				else rowAdapter?.ReRetrieveIfNeeded()
			}
		}
	}

	private fun refreshCurrentItem() {
		val adapter = currentRow?.adapter as? ItemRowAdapter ?: return
		val item = currentItem ?: return

		Timber.d("Refresh item ${item.getFullName(requireContext())}")
		adapter.refreshItem(api, this, item)
	}

	override fun onDestroy() {
		super.onDestroy()

		mediaManager.removeAudioEventListener(this)
	}

	private inner class ItemViewClickedListener : OnItemViewClickedListener {
		override fun onItemClicked(
			itemViewHolder: Presenter.ViewHolder?,
			item: Any?,
			rowViewHolder: RowPresenter.ViewHolder?,
			row: Row?,
		) {
			if (item !is BaseRowItem) return
			if (row !is ListRow) return
			@Suppress("UNCHECKED_CAST")
			itemLauncher.launch(item, row.adapter as MutableObjectAdapter<Any>, requireContext())
		}
	}

	private inner class ItemViewSelectedListener : OnItemViewSelectedListener {
		override fun onItemSelected(
			itemViewHolder: Presenter.ViewHolder?,
			item: Any?,
			rowViewHolder: RowPresenter.ViewHolder?,
			row: Row?,
		) {
			if (item !is BaseRowItem) {
				currentItem = null
				//fill in default background
				backgroundService.clearBackgrounds()
			} else {
				currentItem = item
				currentRow = row as ListRow

				val itemRowAdapter = row.adapter as? ItemRowAdapter
				itemRowAdapter?.loadMoreItemsIfNeeded(itemRowAdapter.indexOf(item))

				backgroundService.setBackground(item.baseItem)
				
				// Update hero overlay if this is a MovieRowItem
				if (item is MovieRowItem) {
					// The hero overlay will be managed separately in the HomeFragment
					HeroUpdateManager.getInstance().updateHeroMovie(item.getMovie())
				}
			}
		}
	}
}
