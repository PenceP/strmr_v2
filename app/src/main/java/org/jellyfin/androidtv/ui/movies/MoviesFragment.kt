package org.jellyfin.androidtv.ui.movies

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
import org.jellyfin.androidtv.util.coil.BlurTransformation
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.data.database.entity.Movie
import org.jellyfin.androidtv.data.repository.NotificationsRepository
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.databinding.FragmentMoviesBinding
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbar
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbarActiveButton
import org.jellyfin.androidtv.ui.home.HeroUpdateManager
import org.koin.android.ext.android.inject

class MoviesFragment : Fragment() {
	private var _binding: FragmentMoviesBinding? = null
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
	private lateinit var heroRtRating: TextView
	private lateinit var heroRtScore: TextView
	private lateinit var heroReleaseDate: TextView
	private lateinit var heroRuntime: TextView
	private lateinit var heroCertification: TextView
	private lateinit var heroOverview: TextView

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		_binding = FragmentMoviesBinding.inflate(inflater, container, false)

		binding.toolbar.setContent {
			MainToolbar(MainToolbarActiveButton.Movies)
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
			
		// Observe hero updates from focus changes in movie rows
		heroUpdateManager.currentHeroMovie
			.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
			.onEach { movie -> 
				if (movie != null) {
					updateHeroOverlay(movie)
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
		heroRtRating = heroOverlay.findViewById(R.id.hero_rt_rating)
		heroRtScore = heroOverlay.findViewById(R.id.hero_rt_score)
		heroReleaseDate = heroOverlay.findViewById(R.id.hero_release_date)
		heroRuntime = heroOverlay.findViewById(R.id.hero_runtime)
		heroCertification = heroOverlay.findViewById(R.id.hero_certification)
		heroOverview = heroOverlay.findViewById(R.id.hero_overview)
		
		// Initially hide the overlay until a movie is selected
		heroOverlay.visibility = View.GONE
	}
	
	private fun updateHeroOverlay(movie: Movie) {
		val heroOverlay = binding.root.findViewById<View>(R.id.hero_overlay)
		
		// Update hero text fields
		heroTitle.text = movie.title
		
		// TMDB Rating
		heroTmdbScore.text = if (movie.voteAverage > 0) "%.1f".format(movie.voteAverage) else ""
		
		// Rotten Tomatoes Rating (show only if available)
		if (movie.rottenTomatoesRating != null && movie.rottenTomatoesRating > 0) {
			heroRtRating.visibility = View.VISIBLE
			heroRtScore.visibility = View.VISIBLE
			heroRtScore.text = "${movie.rottenTomatoesRating}%"
		} else {
			heroRtRating.visibility = View.GONE
			heroRtScore.visibility = View.GONE
		}
		
		// Release Date - format as "MMM d, yyyy"
		heroReleaseDate.text = formatReleaseDate(movie.releaseDate)
		
		// Runtime - format as "h:mm:ss"
		heroRuntime.text = formatRuntime(movie.runtime)
		
		// Certification
		heroCertification.text = movie.certification ?: ""
		heroCertification.visibility = if (movie.certification.isNullOrEmpty()) View.GONE else View.VISIBLE
		
		// Overview
		heroOverview.text = movie.overview ?: ""
		
		// Show the overlay
		heroOverlay.visibility = View.VISIBLE
		
		// Load TMDB backdrop directly into backdrop ImageView
		if (!movie.backdropPath.isNullOrEmpty()) {
			val backdropUrl = "https://image.tmdb.org/t/p/original${movie.backdropPath}"
			
			val request = ImageRequest.Builder(requireContext())
				.data(backdropUrl)
				.transformations(BlurTransformation(requireContext(), radius = 25f))
				.target(backdropImage)
				.build()
			
			imageLoader.enqueue(request)
		}
	}
	
	/**
	 * Format release date from "yyyy-MM-dd" to "MMM d, yyyy" (e.g., "Jun 3, 1998")
	 */
	private fun formatReleaseDate(releaseDate: String?): String {
		return try {
			if (releaseDate.isNullOrEmpty()) return ""
			val parts = releaseDate.split("-")
			if (parts.size < 3) return releaseDate
			
			val year = parts[0]
			val month = parts[1].toInt()
			val day = parts[2].toInt()
			
			val monthNames = arrayOf(
				"", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
				"Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
			)
			
			"${monthNames[month]} $day, $year"
		} catch (e: Exception) {
			releaseDate ?: ""
		}
	}
	
	/**
	 * Format runtime from minutes to "h:mm:ss" format
	 */
	private fun formatRuntime(runtime: Int?): String {
		return if (runtime != null && runtime > 0) {
			val hours = runtime / 60
			val minutes = runtime % 60
			String.format("%d:%02d:00", hours, minutes)
		} else {
			""
		}
	}
	
}