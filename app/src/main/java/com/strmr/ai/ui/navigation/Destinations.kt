package com.strmr.ai.ui.navigation

import kotlinx.serialization.json.Json
import com.strmr.ai.constant.Extras
import com.strmr.ai.ui.browsing.BrowseGridFragment
import com.strmr.ai.ui.browsing.BrowseRecordingsFragment
import com.strmr.ai.ui.browsing.BrowseScheduleFragment
import com.strmr.ai.ui.browsing.BrowseViewFragment
import com.strmr.ai.ui.browsing.ByGenreFragment
import com.strmr.ai.ui.browsing.ByLetterFragment
import com.strmr.ai.ui.browsing.CollectionFragment
import com.strmr.ai.ui.browsing.GenericFolderFragment
import com.strmr.ai.ui.browsing.SuggestedMoviesFragment
import com.strmr.ai.ui.home.HomeFragment
import com.strmr.ai.ui.movies.MoviesFragment
import com.strmr.ai.ui.tv.TVShowsFragment
import com.strmr.ai.ui.itemdetail.FullDetailsFragment
import com.strmr.ai.ui.itemdetail.ItemListFragment
import com.strmr.ai.ui.itemdetail.MusicFavoritesListFragment
import com.strmr.ai.ui.livetv.LiveTvGuideFragment
import com.strmr.ai.ui.picture.PictureViewerFragment
import com.strmr.ai.ui.playback.AudioNowPlayingFragment
import com.strmr.ai.ui.playback.CustomPlaybackOverlayFragment
import com.strmr.ai.ui.playback.nextup.NextUpFragment
import com.strmr.ai.ui.playback.rewrite.PlaybackRewriteFragment
import com.strmr.ai.ui.playback.stillwatching.StillWatchingFragment
import com.strmr.ai.ui.search.SearchFragment
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SeriesTimerInfoDto
import org.jellyfin.sdk.model.api.SortOrder
import java.util.UUID

@Suppress("TooManyFunctions")
object Destinations {
	// General
	val home = fragmentDestination<HomeFragment>()
	val movies = fragmentDestination<MoviesFragment>()
	val tvShows = fragmentDestination<TVShowsFragment>()
	fun search(query: String? = null) = fragmentDestination<SearchFragment>(
		SearchFragment.EXTRA_QUERY to query,
	)

	// Browsing
	// TODO only pass item id instead of complete JSON to browsing destinations
	fun libraryBrowser(item: BaseItemDto) = fragmentDestination<BrowseGridFragment>(
		Extras.Folder to Json.Default.encodeToString(item),
	)

	// TODO only pass item id instead of complete JSON to browsing destinations
	fun libraryBrowser(item: BaseItemDto, includeType: String) =
		fragmentDestination<BrowseGridFragment>(
			Extras.Folder to Json.Default.encodeToString(item),
			Extras.IncludeType to includeType,
		)

	// TODO only pass item id instead of complete JSON to browsing destinations
	fun librarySmartScreen(item: BaseItemDto) = fragmentDestination<BrowseViewFragment>(
		Extras.Folder to Json.Default.encodeToString(item),
	)

	// TODO only pass item id instead of complete JSON to browsing destinations
	fun collectionBrowser(item: BaseItemDto) = fragmentDestination<CollectionFragment>(
		Extras.Folder to Json.Default.encodeToString(item),
	)

	// TODO only pass item id instead of complete JSON to browsing destinations
	fun folderBrowser(item: BaseItemDto) = fragmentDestination<GenericFolderFragment>(
		Extras.Folder to Json.Default.encodeToString(item),
	)

	// TODO only pass item id instead of complete JSON to browsing destinations
	fun libraryByGenres(item: BaseItemDto, includeType: String) =
		fragmentDestination<ByGenreFragment>(
			Extras.Folder to Json.Default.encodeToString(item),
			Extras.IncludeType to includeType,
		)

	// TODO only pass item id instead of complete JSON to browsing destinations
	fun libraryByLetter(item: BaseItemDto, includeType: String) =
		fragmentDestination<ByLetterFragment>(
			Extras.Folder to Json.Default.encodeToString(item),
			Extras.IncludeType to includeType,
		)

	// TODO only pass item id instead of complete JSON to browsing destinations
	fun librarySuggestions(item: BaseItemDto) =
		fragmentDestination<SuggestedMoviesFragment>(
			Extras.Folder to Json.Default.encodeToString(item),
		)

	// Item details
	fun itemDetails(item: UUID) = fragmentDestination<FullDetailsFragment>(
		"ItemId" to item.toString(),
	)

	// TODO only pass item id instead of complete JSON to browsing destinations
	fun channelDetails(item: UUID, channel: UUID, programInfo: BaseItemDto) =
		fragmentDestination<FullDetailsFragment>(
			"ItemId" to item.toString(),
			"ChannelId" to channel.toString(),
			"ProgramInfo" to Json.Default.encodeToString(programInfo),
		)

	// TODO only pass item id instead of complete JSON to browsing destinations
	fun seriesTimerDetails(item: UUID, seriesTimer: SeriesTimerInfoDto) =
		fragmentDestination<FullDetailsFragment>(
			"ItemId" to item.toString(),
			"SeriesTimer" to Json.Default.encodeToString(seriesTimer),
		)

	fun itemList(item: UUID) = fragmentDestination<ItemListFragment>(
		"ItemId" to item.toString(),
	)

	fun musicFavorites(parent: UUID) = fragmentDestination<MusicFavoritesListFragment>(
		"ParentId" to parent.toString(),
	)

	// Live TV
	val liveTvGuide = fragmentDestination<LiveTvGuideFragment>()
	val liveTvSchedule = fragmentDestination<BrowseScheduleFragment>()
	val liveTvRecordings = fragmentDestination<BrowseRecordingsFragment>()
	val liveTvSeriesRecordings = fragmentDestination<BrowseViewFragment>(Extras.IsLiveTvSeriesRecordings to true)

	// Playback
	val nowPlaying = fragmentDestination<AudioNowPlayingFragment>()

	fun pictureViewer(item: UUID, autoPlay: Boolean, albumSortBy: ItemSortBy?, albumSortOrder: SortOrder?) =
		fragmentDestination<PictureViewerFragment>(
			PictureViewerFragment.ARGUMENT_ITEM_ID to item.toString(),
			PictureViewerFragment.ARGUMENT_ALBUM_SORT_BY to albumSortBy?.serialName,
			PictureViewerFragment.ARGUMENT_ALBUM_SORT_ORDER to albumSortOrder?.serialName,
			PictureViewerFragment.ARGUMENT_AUTO_PLAY to autoPlay,
		)

	fun videoPlayer(position: Int?) = fragmentDestination<CustomPlaybackOverlayFragment>(
		"Position" to (position ?: 0)
	)

	fun playbackRewritePlayer(position: Int?) = fragmentDestination<PlaybackRewriteFragment>(
		PlaybackRewriteFragment.EXTRA_POSITION to position
	)

	fun nextUp(item: UUID) = fragmentDestination<NextUpFragment>(
		NextUpFragment.ARGUMENT_ITEM_ID to item.toString()
	)

	fun stillWatching(item: UUID) = fragmentDestination<StillWatchingFragment>(
		NextUpFragment.ARGUMENT_ITEM_ID to item.toString()
	)
}
