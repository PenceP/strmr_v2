package com.strmr.ai.ui.tv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.target
import coil3.request.transformations
import com.strmr.ai.util.coil.BlurTransformation
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import com.strmr.ai.R
import com.strmr.ai.auth.repository.ServerRepository
import com.strmr.ai.auth.repository.SessionRepository
import com.strmr.ai.data.database.entity.Show
import com.strmr.ai.data.repository.NotificationsRepository
import com.strmr.ai.data.service.BackgroundService
import com.strmr.ai.databinding.FragmentTvShowsBinding
import com.strmr.ai.ui.shared.toolbar.MainToolbar
import com.strmr.ai.ui.shared.toolbar.MainToolbarActiveButton
import com.strmr.ai.ui.home.HeroUpdateManager
import com.strmr.ai.util.TmdbGenreMapper
import org.koin.android.ext.android.inject

class TVShowsFragment : Fragment() {
	private var _binding: FragmentTvShowsBinding? = null
	private val binding get() = _binding!!

	private val sessionRepository by inject<SessionRepository>()
	private val serverRepository by inject<ServerRepository>()
	private val notificationRepository by inject<NotificationsRepository>()
	private val backgroundService by inject<BackgroundService>()
	private val imageLoader by inject<ImageLoader>()

	private val heroUpdateManager = HeroUpdateManager.getInstance()

	// Backdrop and hero overlay views
	private lateinit var backdropImage: ImageView
	private lateinit var heroTitle: TextView
	private lateinit var heroTmdbScore: TextView
	private lateinit var heroFirstAirDate: TextView
	private lateinit var heroSeasons: TextView
	private lateinit var heroEpisodes: TextView
	private lateinit var heroContentRating: TextView
	private lateinit var heroGenres: TextView
	private lateinit var heroOverview: TextView

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		_binding = FragmentTvShowsBinding.inflate(inflater, container, false)

		binding.toolbar.setContent {
			MainToolbar(MainToolbarActiveButton.TVShows)
		}

		// Initialize hero overlay views
		initializeHeroOverlay()

		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		sessionRepository.currentSession
			.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
			.map { session ->
				if (session == null) null
				else serverRepository.getServer(session.serverId)
			}
			.onEach { server ->
				notificationRepository.updateServerNotifications(server)
			}
			.launchIn(viewLifecycleOwner.lifecycleScope)

		// Observe hero updates from focus changes in show rows
		heroUpdateManager.currentHeroShow
			.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
			.onEach { show ->
				if (show != null) {
					updateHeroOverlay(show)
				}
			}
			.launchIn(viewLifecycleOwner.lifecycleScope)
	}

	override fun onDestroyView() {
		super.onDestroyView()

		_binding = null
	}

	private fun initializeHeroOverlay() {
		// Initialize backdrop ImageView
		backdropImage = binding.root.findViewById(R.id.backdrop_image)

		// Initialize hero overlay views
		val heroOverlay = binding.root.findViewById<View>(R.id.hero_overlay)
		heroTitle = heroOverlay.findViewById(R.id.hero_title)
		heroTmdbScore = heroOverlay.findViewById(R.id.hero_tmdb_score)
		heroFirstAirDate = heroOverlay.findViewById(R.id.hero_first_air_date)
		heroSeasons = heroOverlay.findViewById(R.id.hero_seasons)
		//heroEpisodes = heroOverlay.findViewById(R.id.hero_episodes)
		heroContentRating = heroOverlay.findViewById(R.id.hero_content_rating)
		heroGenres = heroOverlay.findViewById(R.id.hero_genres)
		heroOverview = heroOverlay.findViewById(R.id.hero_overview)

		// Initially hide the overlay until a show is selected
		heroOverlay.visibility = View.GONE
	}

	private fun updateHeroOverlay(show: Show) {
		val heroOverlay = binding.root.findViewById<View>(R.id.hero_overlay)

		// Update hero text fields
		heroTitle.text = show.name

		// TMDB Rating
		heroTmdbScore.text = if (show.voteAverage > 0) "%.1f".format(show.voteAverage) else ""

		// First Air Date - format as "MMM d, yyyy"
		heroFirstAirDate.text = formatAirDate(show.firstAirDate)

		// Seasons
		heroSeasons.text = formatSeasons(show.numberOfSeasons)

		// Episodes
		//heroEpisodes.text = formatEpisodes(show.numberOfEpisodes)

		// Content Rating
		heroContentRating.text = show.contentRating ?: ""
		heroContentRating.visibility = if (show.contentRating.isNullOrEmpty()) View.GONE else View.VISIBLE

		// Genres
		val genres = TmdbGenreMapper.getTvGenres(show.genreIds)
		if (genres.isNotEmpty()) {
			heroGenres.text = genres.joinToString(" / ")
			heroGenres.visibility = View.VISIBLE
		} else {
			heroGenres.visibility = View.GONE
		}

		// Overview
		heroOverview.text = show.overview ?: ""

		// Show the overlay
		heroOverlay.visibility = View.VISIBLE

		// Load TMDB backdrop directly into backdrop ImageView
		if (!show.backdropPath.isNullOrEmpty()) {
			val backdropUrl = "https://image.tmdb.org/t/p/original${show.backdropPath}"

			val request = ImageRequest.Builder(requireContext())
				.data(backdropUrl)
				.transformations(BlurTransformation(requireContext(), radius = 25f))
				.target(backdropImage)
				.build()

			imageLoader.enqueue(request)
		}
	}

	/**
	 * Format air date from "yyyy-MM-dd" to "MMM d, yyyy" (e.g., "Jun 3, 1998")
	 */
	private fun formatAirDate(airDate: String?): String {
		return try {
			if (airDate.isNullOrEmpty()) return ""
			val parts = airDate.split("-")
			if (parts.size < 3) return airDate

			val year = parts[0]
			val month = parts[1].toInt()
			val day = parts[2].toInt()

			val monthNames = arrayOf(
				"", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
				"Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
			)

			"${monthNames[month]} $day, $year"
		} catch (e: Exception) {
			airDate ?: ""
		}
	}

	/**
	 * Format number of seasons
	 */
	private fun formatSeasons(seasons: Int?): String {
		return if (seasons != null && seasons > 0) {
			if (seasons == 1) "$seasons season" else "$seasons seasons"
		} else {
			""
		}
	}

	/**
	 * Format number of episodes
	 */
//	private fun formatEpisodes(episodes: Int?): String {
//		return if (episodes != null && episodes > 0) {
//			if (episodes == 1) "$episodes episode" else "$episodes episodes"
//		} else {
//			""
//		}
//	}

}
