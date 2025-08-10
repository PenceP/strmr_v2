package org.jellyfin.androidtv.ui.itemdetail

import android.content.ActivityNotFoundException
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.model.DataRefreshService
import org.jellyfin.androidtv.data.repository.ItemMutationRepository
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.util.TimeUtils
import org.jellyfin.androidtv.util.apiclient.getSeriesOverview
import org.jellyfin.androidtv.util.popupMenu
import org.jellyfin.androidtv.util.sdk.TrailerUtils.getExternalTrailerIntent
import org.jellyfin.androidtv.util.sdk.compat.canResume
import org.jellyfin.androidtv.util.sdk.compat.copyWithUserData
import org.jellyfin.androidtv.util.showIfNotEmpty
import org.jellyfin.androidtv.data.database.entity.Movie
import org.jellyfin.androidtv.data.database.entity.Show
import org.jellyfin.androidtv.data.repository.ImprovedMovieRepository
import org.jellyfin.androidtv.data.repository.ShowRepository
import org.jellyfin.androidtv.data.database.entity.CastMember
import org.jellyfin.androidtv.data.database.entity.ShowCastMember
import org.jellyfin.androidtv.util.TmdbGenreMapper
import org.jellyfin.sdk.api.client.ApiClient
import timber.log.Timber
import org.jellyfin.sdk.model.api.UserItemDataDto
import org.jellyfin.androidtv.util.apiclient.JellyfinImage
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.api.client.extensions.liveTvApi
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.BaseItemPerson
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.PersonKind
import org.jellyfin.sdk.model.api.SeriesTimerInfoDto
import org.jellyfin.sdk.model.extensions.ticks
import org.jellyfin.sdk.model.serializer.toUUID
import org.koin.android.ext.android.inject
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

fun FullDetailsFragment.deleteItem(
	api: ApiClient,
	item: BaseItemDto,
	dataRefreshService: DataRefreshService,
	navigationRepository: NavigationRepository,
) = lifecycleScope.launch {
	Timber.i("Deleting item ${item.name} (id=${item.id})")

	try {
		withContext(Dispatchers.IO) {
			api.libraryApi.deleteItem(item.id)
		}
	} catch (error: ApiClientException) {
		Timber.e(error, "Failed to delete item ${item.name} (id=${item.id})")
		Toast.makeText(
			context,
			getString(R.string.item_deletion_failed, item.name),
			Toast.LENGTH_LONG
		).show()
		return@launch
	}

	dataRefreshService.lastDeletedItemId = item.id

	if (navigationRepository.canGoBack) navigationRepository.goBack()
	else navigationRepository.navigate(Destinations.home)

	Toast.makeText(context, getString(R.string.item_deleted, item.name), Toast.LENGTH_LONG).show()
}

fun FullDetailsFragment.showDetailsMenu(
	view: View,
	baseItemDto: BaseItemDto,
) = popupMenu(requireContext(), view) {
	// for each button check if it exists (not-null) and is invisible (overflow prevention)
	if (queueButton?.isVisible == false) {
		item(getString(R.string.lbl_add_to_queue)) { addItemToQueue() }
	}

	if (shuffleButton?.isVisible == false) {
		item(getString(R.string.lbl_shuffle_all)) { shufflePlay() }
	}

	if (trailerButton?.isVisible == false) {
		item(getString(R.string.lbl_play_trailers)) { playTrailers() }
	}

	if (favButton?.isVisible == false) {
		val favoriteStringRes = when (baseItemDto.userData?.isFavorite) {
			true -> R.string.lbl_remove_favorite
			else -> R.string.lbl_add_favorite
		}

		item(getString(favoriteStringRes)) { toggleFavorite() }
	}

	if (goToSeriesButton?.isVisible == false) {
		item(getString(R.string.lbl_goto_series)) { gotoSeries() }
	}
}.showIfNotEmpty()

fun FullDetailsFragment.createFakeSeriesTimerBaseItemDto(timer: SeriesTimerInfoDto) = BaseItemDto(
	id = requireNotNull(timer.id).toUUID(),
	type = BaseItemKind.FOLDER,
	mediaType = MediaType.UNKNOWN,
	seriesTimerId = timer.id,
	name = timer.name,
	overview = timer.getSeriesOverview(requireContext()),
)

fun FullDetailsFragment.createFakeMovieBaseItemDto(movie: Movie, cast: List<CastMember> = emptyList()): BaseItemDto {
	val baseItem = BaseItemDto(
		id = UUID.fromString(movie.id.toString() + "-0000-0000-0000-000000000000"),
		type = BaseItemKind.MOVIE,
		mediaType = MediaType.VIDEO,
		name = movie.title,
		overview = movie.overview,
		originalTitle = movie.originalTitle,
		// Add runtime in ticks (1 minute = 600,000,000 ticks)
		runTimeTicks = movie.runtime?.let { it * 60 * 10_000_000L },
		// Add community rating (convert from 0-10 to 0-100 scale)
		communityRating = if (movie.voteAverage > 0) (movie.voteAverage * 10).toFloat() else null,
		// Add official rating (certification)
		officialRating = movie.certification,
		// Add production year
		productionYear = movie.releaseDate?.substring(0, 4)?.toIntOrNull(),
		// Add premiere date
		premiereDate = movie.releaseDate?.let { 
			try {
				java.time.LocalDateTime.parse(it + "T00:00:00")
			} catch (e: Exception) { null }
		},
		// Add image tags for Jellyfin image system - encode image paths in the tag
		imageTags = if (!movie.posterPath.isNullOrEmpty()) {
			mapOf(org.jellyfin.sdk.model.api.ImageType.PRIMARY to "tmdb-poster:${movie.posterPath}")
		} else null,
		backdropImageTags = if (!movie.backdropPath.isNullOrEmpty()) {
			listOf("tmdb-backdrop:${movie.backdropPath}")
		} else null,
		// Add additional metadata
		genres = TmdbGenreMapper.getMovieGenres(movie.genreIds),
		// Add people/cast
		people = cast.map { castMember ->
			val personType = when {
				castMember.department == "Acting" -> PersonKind.ACTOR
				castMember.job == "Director" -> PersonKind.DIRECTOR
				castMember.job == "Writer" || castMember.job == "Screenplay" -> PersonKind.WRITER
				castMember.job == "Producer" || castMember.job == "Executive Producer" -> PersonKind.PRODUCER
				else -> PersonKind.UNKNOWN
			}
			Timber.d("Creating person: ${castMember.name} (${castMember.character ?: castMember.job}) -> type: $personType")
			BaseItemPerson(
				id = UUID.nameUUIDFromBytes("person-${castMember.personId}".toByteArray()),
				name = castMember.name,
				role = castMember.character,
				type = personType,
				primaryImageTag = castMember.profilePath?.let { "tmdb-profile:$it" }
			)
		}.also { peopleList ->
			Timber.d("Created BaseItemDto with ${peopleList.size} people total")
			val directors = peopleList.filter { it.type == PersonKind.DIRECTOR }
			Timber.d("Directors found: ${directors.map { it.name }}")
		}
	)
	
	// Create a fake UserItemDataDto to prevent NullPointerException
	return baseItem.copyWithUserData(
		UserItemDataDto(
			rating = null,
			played = false,
			playedPercentage = 0.0,
			playbackPositionTicks = 0L,
			playCount = 0,
			isFavorite = false,
			likes = null,
			lastPlayedDate = null,
			unplayedItemCount = null,
			key = "",
			itemId = UUID.fromString(movie.id.toString() + "-0000-0000-0000-000000000000")
		)
	)
}

fun FullDetailsFragment.createFakeShowBaseItemDto(show: Show, cast: List<ShowCastMember> = emptyList()): BaseItemDto {
	val baseItem = BaseItemDto(
		id = UUID.fromString(show.id.toString() + "-1111-1111-1111-111111111111"),
		type = BaseItemKind.SERIES,
		mediaType = MediaType.VIDEO,
		name = show.name,
		overview = show.overview,
		originalTitle = show.originalName,
		// Add community rating (convert from 0-10 to 0-100 scale)
		communityRating = if (show.voteAverage > 0) (show.voteAverage * 10).toFloat() else null,
		// Add content rating (status field could contain rating info)
		officialRating = show.contentRating,
		// Add production year from first air date
		productionYear = show.firstAirDate?.substring(0, 4)?.toIntOrNull(),
		// Add premiere date
		premiereDate = show.firstAirDate?.let { 
			try {
				java.time.LocalDateTime.parse(it + "T00:00:00")
			} catch (e: Exception) { null }
		},
		// Add end date if available
		endDate = show.lastAirDate?.let {
			try {
				java.time.LocalDateTime.parse(it + "T00:00:00")
			} catch (e: Exception) { null }
		},
		// Add season/episode counts
		childCount = show.numberOfSeasons,
		recursiveItemCount = show.numberOfEpisodes,
		// Add image tags for Jellyfin image system - encode image paths in the tag
		imageTags = if (!show.posterPath.isNullOrEmpty()) {
			mapOf(org.jellyfin.sdk.model.api.ImageType.PRIMARY to "tmdb-poster:${show.posterPath}")
		} else null,
		backdropImageTags = if (!show.backdropPath.isNullOrEmpty()) {
			listOf("tmdb-backdrop:${show.backdropPath}")
		} else null,
		// Add additional metadata
		genres = TmdbGenreMapper.getTvGenres(show.genreIds),
		// Add status information
		status = show.status,
		// Add people/cast
		people = cast.map { castMember ->
			BaseItemPerson(
				id = UUID.nameUUIDFromBytes("person-${castMember.personId}".toByteArray()),
				name = castMember.name,
				role = castMember.character,
				type = when {
					castMember.department == "Acting" -> PersonKind.ACTOR
					castMember.job == "Creator" -> PersonKind.DIRECTOR // Use Director for Creator
					castMember.job == "Director" -> PersonKind.DIRECTOR
					castMember.job == "Writer" -> PersonKind.WRITER
					castMember.job == "Producer" || castMember.job == "Executive Producer" -> PersonKind.PRODUCER
					else -> PersonKind.UNKNOWN
				},
				primaryImageTag = castMember.profilePath?.let { "tmdb-profile:$it" }
			)
		}
	)
	
	// Create a fake UserItemDataDto to prevent NullPointerException
	return baseItem.copyWithUserData(
		UserItemDataDto(
			rating = null,
			played = false,
			playedPercentage = 0.0,
			playbackPositionTicks = 0L,
			playCount = 0,
			isFavorite = false,
			likes = null,
			lastPlayedDate = null,
			unplayedItemCount = null,
			key = "",
			itemId = UUID.fromString(show.id.toString() + "-1111-1111-1111-111111111111")
		)
	)
}

fun FullDetailsFragment.loadMovieFromDatabase(tmdbId: Int, callback: (BaseItemDto?) -> Unit) {
	val movieRepository: ImprovedMovieRepository by inject()
	
	lifecycleScope.launch {
		val movie: Movie? = movieRepository.getMovieByTmdbId(tmdbId)
		if (movie != null) {
			// Try to get existing cast data first
			var cast: List<CastMember> = movieRepository.getMovieCast(tmdbId)
			Timber.d("Loading movie: ${movie.title} with ${cast.size} cast members from database")
			
			// If we have no cast data, or very few cast members, try to fetch fresh data
			if (cast.isEmpty() || cast.size < 3) {
				Timber.d("Cast data seems incomplete (${cast.size} members), attempting to fetch fresh data")
				try {
					// Force refresh cast data - this will fetch from TMDB API and update database
					cast = movieRepository.refreshMovieCast(tmdbId)
					Timber.d("After refresh: ${cast.size} cast members")
				} catch (e: Exception) {
					Timber.w(e, "Failed to refresh cast data for movie $tmdbId")
				}
			}
			
			if (cast.isNotEmpty()) {
				Timber.d("Cast members: ${cast.take(5).map { "${it.name} as ${it.character ?: it.job}" }}")
			} else {
				Timber.w("No cast data found for movie: ${movie.title} (TMDB ID: $tmdbId)")
			}
			val baseItem: BaseItemDto = createFakeMovieBaseItemDto(movie, cast)
			callback(baseItem)
		} else {
			Timber.w("No movie found in database for TMDB ID: $tmdbId")
			callback(null)
		}
	}
}

fun FullDetailsFragment.loadShowFromDatabase(tmdbId: Int, callback: (BaseItemDto?) -> Unit) {
	val showRepository: ShowRepository by inject()
	
	lifecycleScope.launch {
		val show: Show? = showRepository.getShowByTmdbId(tmdbId)
		if (show != null) {
			val cast: List<ShowCastMember> = showRepository.getShowCast(tmdbId)
			val baseItem: BaseItemDto = createFakeShowBaseItemDto(show, cast)
			callback(baseItem)
		} else {
			callback(null)
		}
	}
}

fun FullDetailsFragment.toggleFavorite() {
	val itemMutationRepository by inject<ItemMutationRepository>()
	val dataRefreshService by inject<DataRefreshService>()

	lifecycleScope.launch {
		val userData = itemMutationRepository.setFavorite(
			item = mBaseItem.id,
			favorite = !(mBaseItem.userData?.isFavorite ?: false)
		)
		mBaseItem = mBaseItem.copyWithUserData(userData)
		favButton.isActivated = userData.isFavorite
		dataRefreshService.lastFavoriteUpdate = Instant.now()
	}
}

fun FullDetailsFragment.togglePlayed() {
	val itemMutationRepository by inject<ItemMutationRepository>()
	val dataRefreshService by inject<DataRefreshService>()

	lifecycleScope.launch {
		val userData = itemMutationRepository.setPlayed(
			item = mBaseItem.id,
			played = !(mBaseItem.userData?.played ?: false)
		)
		mBaseItem = mBaseItem.copyWithUserData(userData)
		mWatchedToggleButton.isActivated = userData.played

		// Adjust resume
		mResumeButton?.apply {
			isVisible = mBaseItem.canResume
		}

		// Force lists to re-fetch
		dataRefreshService.lastPlayback = Instant.now()
		when (mBaseItem.type) {
			BaseItemKind.MOVIE -> dataRefreshService.lastMoviePlayback = Instant.now()
			BaseItemKind.EPISODE -> dataRefreshService.lastTvPlayback = Instant.now()
			else -> Unit
		}

		showMoreButtonIfNeeded()
	}
}

fun FullDetailsFragment.playTrailers() {
	val localTrailerCount = mBaseItem.localTrailerCount ?: 0

	// External trailer
	if (localTrailerCount < 1) try {
		val intent = getExternalTrailerIntent(requireContext(), mBaseItem)
		if (intent != null) startActivity(intent)
	} catch (exception: ActivityNotFoundException) {
		Timber.w(exception, "Unable to open external trailer")
		Toast.makeText(
			requireContext(),
			getString(R.string.no_player_message),
			Toast.LENGTH_LONG
		).show()
	} else lifecycleScope.launch {
		val api by inject<ApiClient>()

		try {
			val trailers = withContext(Dispatchers.IO) {
				api.userLibraryApi.getLocalTrailers(mBaseItem.id).content
			}
			play(trailers, 0, false)
		} catch (exception: ApiClientException) {
			Timber.e(exception, "Error retrieving trailers for playback")
			Toast.makeText(
				requireContext(),
				getString(R.string.msg_video_playback_error),
				Toast.LENGTH_LONG
			).show()
		}
	}
}

fun FullDetailsFragment.getItem(id: UUID, callback: (item: BaseItemDto?) -> Unit) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		val response = try {
			withContext(Dispatchers.IO) {
				api.userLibraryApi.getItem(id).content
			}
		} catch (err: ApiClientException) {
			Timber.w(err, "Failed to get item $id")
			null
		}

		callback(response)
	}
}

fun FullDetailsFragment.populatePreviousButton() {
	if (mBaseItem.type != BaseItemKind.EPISODE) return

	val api by inject<ApiClient>()

	lifecycleScope.launch {
		val siblings = withContext(Dispatchers.IO) {
			api.tvShowsApi.getEpisodes(
				seriesId = requireNotNull(mBaseItem.seriesId),
				adjacentTo = mBaseItem.id,
			).content
		}

		val previousItem = siblings.items
			.filterNot { it.id == mBaseItem.id }
			.firstOrNull()
			?.id

		mPrevItemId = previousItem
		mPrevButton.isVisible = previousItem != null

		showMoreButtonIfNeeded()
	}
}

fun FullDetailsFragment.getNextUpEpisode(callback: (BaseItemDto?) -> Unit) {
	lifecycleScope.launch {
		val nextUpEpisode = getNextUpEpisode()
		callback(nextUpEpisode)
	}
}

suspend fun FullDetailsFragment.getNextUpEpisode(): BaseItemDto? {
	val api by inject<ApiClient>()

	try {
		val episodes = withContext(Dispatchers.IO) {
			api.tvShowsApi.getNextUp(
				seriesId = mBaseItem.seriesId ?: mBaseItem.id,
				fields = ItemRepository.itemFields,
				limit = 1,
			).content
		}
		return episodes.items.firstOrNull()
	} catch (err: ApiClientException) {
		Timber.w(err, "Failed to get next up items")
		return null
	}
}

fun FullDetailsFragment.resumePlayback(v: View) {
	if (mBaseItem.type != BaseItemKind.SERIES) {
		val pos = (mBaseItem.userData?.playbackPositionTicks?.ticks
			?: Duration.ZERO) - resumePreroll.milliseconds
		play(mBaseItem, pos.inWholeMilliseconds.toInt(), false)
		return
	}

	lifecycleScope.launch {
		val nextUpEpisode = getNextUpEpisode()
		if (nextUpEpisode == null) {
			Toast.makeText(
				requireContext(),
				getString(R.string.msg_video_playback_error),
				Toast.LENGTH_LONG
			).show()
		}

		if (nextUpEpisode?.userData?.playbackPositionTicks == 0L) {
			play(nextUpEpisode, 0, false)
		} else {
			showResumeMenu(v, nextUpEpisode!!)
		}
	}
}

fun FullDetailsFragment.showResumeMenu(
	view: View,
	nextUpEpisode: BaseItemDto
) = popupMenu(requireContext(), view) {
	val pos = (nextUpEpisode.userData?.playbackPositionTicks?.ticks
		?: Duration.ZERO) - resumePreroll.milliseconds
	item(
		getString(
			R.string.lbl_resume_from,
			TimeUtils.formatMillis(pos.inWholeMilliseconds)
		)
	) {
		play(nextUpEpisode, pos.inWholeMilliseconds.toInt(), false)
	}
	item(getString(R.string.lbl_from_beginning)) {
		play(nextUpEpisode, 0, false)
	}
}.showIfNotEmpty()

fun FullDetailsFragment.getLiveTvSeriesTimer(
	id: String,
	callback: (timer: SeriesTimerInfoDto) -> Unit,
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.getSeriesTimer(id).content
			}
		}.onSuccess { timer ->
			callback(timer)
		}
	}
}

fun FullDetailsFragment.getLiveTvProgram(
	id: UUID,
	callback: (program: BaseItemDto) -> Unit,
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.getProgram(id.toString()).content
			}
		}.onSuccess { program ->
			callback(program)
		}
	}
}

fun FullDetailsFragment.createLiveTvSeriesTimer(
	seriesTimer: SeriesTimerInfoDto,
	callback: () -> Unit,
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.createSeriesTimer(seriesTimer)
			}
		}.onSuccess {
			callback()
		}
	}
}

fun FullDetailsFragment.getLiveTvDefaultTimer(
	id: UUID,
	callback: (seriesTimer: SeriesTimerInfoDto) -> Unit,
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.getDefaultTimer(id.toString()).content
			}
		}.onSuccess { seriesTimer ->
			callback(seriesTimer)
		}
	}
}

fun FullDetailsFragment.cancelLiveTvSeriesTimer(
	timerId: String,
	callback: () -> Unit,
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.cancelTimer(timerId)
			}
		}.onSuccess {
			callback()
		}
	}
}

fun FullDetailsFragment.getLiveTvChannel(
	id: UUID,
	callback: (channel: BaseItemDto) -> Unit,
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.getChannel(id).content
			}
		}.onSuccess { channel ->
			callback(channel)
		}
	}
}

/**
 * Custom extension to ImageHelper that handles TMDB images for fake items
 */
fun org.jellyfin.androidtv.util.ImageHelper.getImageUrlWithTmdbSupport(image: JellyfinImage): String {
	val itemIdString = image.item.toString()
	
	// Check if this is a fake Movie/Show UUID - if so, return TMDB URL
	if (itemIdString.endsWith("-0000-0000-0000-000000000000") || itemIdString.endsWith("-1111-1111-1111-111111111111")) {
		when {
			image.type == ImageType.PRIMARY && image.tag.startsWith("tmdb-poster:") -> {
				val posterPath = image.tag.substringAfter("tmdb-poster:")
				return "https://image.tmdb.org/t/p/w500$posterPath"
			}
			image.type == ImageType.BACKDROP && image.tag.startsWith("tmdb-backdrop:") -> {
				val backdropPath = image.tag.substringAfter("tmdb-backdrop:")
				return "https://image.tmdb.org/t/p/w1280$backdropPath"
			}
		}
	}
	
	// Fall back to default Jellyfin behavior for real items
	return getImageUrl(image)
}
