package com.strmr.ai.di

import android.content.Context
import android.os.Build
import androidx.lifecycle.ProcessLifecycleOwner
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.network.NetworkFetcher
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.serviceLoaderEnabled
import coil3.svg.SvgDecoder
import coil3.util.Logger
import com.strmr.ai.BuildConfig
import com.strmr.ai.auth.repository.ServerRepository
import com.strmr.ai.auth.repository.UserRepository
import com.strmr.ai.auth.repository.UserRepositoryImpl
import com.strmr.ai.data.eventhandling.SocketHandler
import com.strmr.ai.data.model.DataRefreshService
import com.strmr.ai.data.repository.CustomMessageRepository
import com.strmr.ai.data.repository.CustomMessageRepositoryImpl
import com.strmr.ai.data.repository.ItemMutationRepository
import com.strmr.ai.data.repository.ItemMutationRepositoryImpl
import com.strmr.ai.data.repository.NotificationsRepository
import com.strmr.ai.data.repository.NotificationsRepositoryImpl
import com.strmr.ai.data.repository.UserViewsRepository
import com.strmr.ai.data.repository.UserViewsRepositoryImpl
import com.strmr.ai.data.service.BackgroundService
import com.strmr.ai.integration.dream.DreamViewModel
import com.strmr.ai.ui.InteractionTrackerViewModel
import com.strmr.ai.ui.itemhandling.ItemLauncher
import com.strmr.ai.ui.navigation.Destinations
import com.strmr.ai.ui.navigation.NavigationRepository
import com.strmr.ai.ui.navigation.NavigationRepositoryImpl
import com.strmr.ai.ui.picture.PictureViewerViewModel
import com.strmr.ai.ui.playback.PlaybackControllerContainer
import com.strmr.ai.ui.playback.nextup.NextUpViewModel
import com.strmr.ai.ui.playback.segment.MediaSegmentRepository
import com.strmr.ai.ui.playback.segment.MediaSegmentRepositoryImpl
import com.strmr.ai.ui.playback.stillwatching.StillWatchingViewModel
import com.strmr.ai.ui.search.SearchFragmentDelegate
import com.strmr.ai.ui.search.SearchRepository
import com.strmr.ai.ui.search.SearchRepositoryImpl
import com.strmr.ai.ui.search.SearchViewModel
import com.strmr.ai.ui.startup.ServerAddViewModel
import com.strmr.ai.ui.startup.StartupViewModel
import com.strmr.ai.ui.startup.UserLoginViewModel
import com.strmr.ai.ui.movies.MoviesViewModel
import com.strmr.ai.ui.movies.MovieDetailsViewModel
import com.strmr.ai.util.KeyProcessor
import com.strmr.ai.util.MarkdownRenderer
import com.strmr.ai.util.PlaybackHelper
import com.strmr.ai.util.apiclient.ReportingHelper
import com.strmr.ai.util.coil.CoilTimberLogger
import com.strmr.ai.util.coil.createCoilConnectivityChecker
import com.strmr.ai.util.sdk.SdkPlaybackHelper
import org.jellyfin.sdk.android.androidDevice
import org.jellyfin.sdk.api.client.HttpClientOptions
import org.jellyfin.sdk.api.okhttp.OkHttpFactory
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.jellyfin.sdk.Jellyfin as JellyfinSdk

val defaultDeviceInfo = named("defaultDeviceInfo")

val appModule = module {
	// SDK
	single(defaultDeviceInfo) { androidDevice(get()) }
	single { OkHttpFactory() }
	single { HttpClientOptions() }
	single {
		createJellyfin {
			context = androidContext()

			// Add client info
			val clientName = buildString {
				append("Jellyfin Android TV")
				if (BuildConfig.DEBUG) append(" (debug)")
			}
			clientInfo = ClientInfo(clientName, BuildConfig.VERSION_NAME)
			deviceInfo = get(defaultDeviceInfo)

			// Change server version
			minimumServerVersion = ServerRepository.minimumServerVersion

			// Use our own shared factory instance
			apiClientFactory = get<OkHttpFactory>()
			socketConnectionFactory = get<OkHttpFactory>()
		}
	}

	single {
		// Create an empty API instance, the actual values are set by the SessionRepository
		get<JellyfinSdk>().createApi(httpClientOptions = get<HttpClientOptions>())
	}

	single { SocketHandler(get(), get(), get(), get(), get(), get(), get(), get(), get(), ProcessLifecycleOwner.get().lifecycle) }

	// Coil (images)
	single {
		val okHttpFactory = get<OkHttpFactory>()
		val httpClientOptions = get<HttpClientOptions>()

		@OptIn(ExperimentalCoilApi::class)
		OkHttpNetworkFetcherFactory(
			callFactory = { okHttpFactory.createClient(httpClientOptions) },
			connectivityChecker = ::createCoilConnectivityChecker,
		)
	}

	single {
		ImageLoader.Builder(androidContext()).apply {
			serviceLoaderEnabled(false)
			logger(CoilTimberLogger(if (BuildConfig.DEBUG) Logger.Level.Warn else Logger.Level.Error))
			
			// Note: Coil 3 cache configuration is handled automatically
			// Images will be cached to disk and memory by default

			components {
				add(get<NetworkFetcher.Factory>())

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) add(AnimatedImageDecoder.Factory())
				else add(GifDecoder.Factory())
				add(SvgDecoder.Factory())
			}
		}.build()
	}

	// Non API related
	single { DataRefreshService() }
	single { PlaybackControllerContainer() }
	single { InteractionTrackerViewModel(get(), get()) }

	single<UserRepository> { UserRepositoryImpl() }
	single<UserViewsRepository> { UserViewsRepositoryImpl(get()) }
	single<NotificationsRepository> { NotificationsRepositoryImpl(get(), get()) }
	single<ItemMutationRepository> { ItemMutationRepositoryImpl(get(), get()) }
	single<CustomMessageRepository> { CustomMessageRepositoryImpl() }
	single<NavigationRepository> { NavigationRepositoryImpl(Destinations.home) }
	single<SearchRepository> { SearchRepositoryImpl(get()) }
	single<MediaSegmentRepository> { MediaSegmentRepositoryImpl(get(), get()) }

	viewModel { StartupViewModel(get(), get(), get(), get()) }
	viewModel { UserLoginViewModel(get(), get(), get(), get(defaultDeviceInfo)) }
	viewModel { ServerAddViewModel(get()) }
	viewModel { NextUpViewModel(get(), get(), get()) }
	viewModel { StillWatchingViewModel(get(), get(), get()) }
	viewModel { PictureViewerViewModel(get()) }
	viewModel { SearchViewModel(get()) }
	viewModel { DreamViewModel(get(), get(), get(), get(), get()) }
	viewModel { MoviesViewModel(get()) }
	factory { (movieId: Int) -> MovieDetailsViewModel(get(), movieId) }

	single { BackgroundService(get(), get(), get(), get(), get()) }

	single { MarkdownRenderer(get()) }
	single { ItemLauncher() }
	single { KeyProcessor() }
	single { ReportingHelper(get(), get()) }
	single<PlaybackHelper> { SdkPlaybackHelper(get(), get(), get(), get()) }

	factory { (context: Context) -> SearchFragmentDelegate(context, get(), get()) }
}
