package org.jellyfin.androidtv.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.target
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
import org.jellyfin.androidtv.databinding.FragmentHomeBinding
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbar
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbarActiveButton
import org.koin.android.ext.android.inject

class HomeFragment : Fragment() {
	private var _binding: FragmentHomeBinding? = null
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
	private lateinit var heroYear: TextView
	private lateinit var heroRating: TextView
	private lateinit var heroOverview: TextView

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		_binding = FragmentHomeBinding.inflate(inflater, container, false)

		binding.toolbar.setContent {
			MainToolbar(MainToolbarActiveButton.Home)
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
		heroYear = heroOverlay.findViewById(R.id.hero_year)
		heroRating = heroOverlay.findViewById(R.id.hero_rating)
		heroOverview = heroOverlay.findViewById(R.id.hero_overview)
		
		// Initially hide the overlay until a movie is selected
		heroOverlay.visibility = View.GONE
	}
	
	private fun updateHeroOverlay(movie: Movie) {
		val heroOverlay = binding.root.findViewById<View>(R.id.hero_overlay)
		
		// Update hero text fields
		heroTitle.text = movie.title
		heroYear.text = movie.releaseDate?.substring(0, 4) ?: ""
		heroRating.text = if (movie.voteAverage > 0) "%.1f".format(movie.voteAverage) else ""
		heroOverview.text = movie.overview ?: ""
		
		// Show the overlay
		heroOverlay.visibility = View.VISIBLE
		
		// Load TMDB backdrop directly into backdrop ImageView
		if (!movie.backdropPath.isNullOrEmpty()) {
			val backdropUrl = "https://image.tmdb.org/t/p/original${movie.backdropPath}"
			
			val request = ImageRequest.Builder(requireContext())
				.data(backdropUrl)
				.target(backdropImage)
				.build()
			
			imageLoader.enqueue(request)
		}
	}
	
}
