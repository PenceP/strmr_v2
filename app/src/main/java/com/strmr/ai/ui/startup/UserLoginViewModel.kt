package com.strmr.ai.ui.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.strmr.ai.auth.model.AuthenticatingState
import com.strmr.ai.auth.model.AutomaticAuthenticateMethod
import com.strmr.ai.auth.model.ConnectedQuickConnectState
import com.strmr.ai.auth.model.CredentialAuthenticateMethod
import com.strmr.ai.auth.model.LoginState
import com.strmr.ai.auth.model.PendingQuickConnectState
import com.strmr.ai.auth.model.QuickConnectAuthenticateMethod
import com.strmr.ai.auth.model.QuickConnectState
import com.strmr.ai.auth.model.Server
import com.strmr.ai.auth.model.UnavailableQuickConnectState
import com.strmr.ai.auth.model.UnknownQuickConnectState
import com.strmr.ai.auth.model.User
import com.strmr.ai.auth.repository.AuthenticationRepository
import com.strmr.ai.auth.repository.ServerRepository
import com.strmr.ai.util.sdk.forUser
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.quickConnectApi
import org.jellyfin.sdk.model.DeviceInfo
import timber.log.Timber
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class UserLoginViewModel(
	jellyfin: Jellyfin,
	private val serverRepository: ServerRepository,
	private val authenticationRepository: AuthenticationRepository,
	private val defaultDeviceInfo: DeviceInfo,
) : ViewModel() {
	private val _loginState = MutableStateFlow<LoginState?>(null)
	val loginState = _loginState.asStateFlow()

	var forcedUsername: String? = null

	private val _server = MutableStateFlow<Server?>(null)
	val server = _server.asStateFlow()

	private val quickConnectApi = jellyfin.createApi()
	private var quickConnectSecret: String? = null
	private val _quickConnectState = MutableStateFlow<QuickConnectState>(UnknownQuickConnectState)
	val quickConnectState = _quickConnectState.asStateFlow()
	fun authenticate(server: Server, user: User): Flow<LoginState> =
		authenticationRepository.authenticate(server, AutomaticAuthenticateMethod(user))

	fun login(username: String, password: String) {
		val server = server.value ?: return
		_loginState.value = AuthenticatingState
		authenticationRepository.authenticate(server, CredentialAuthenticateMethod(username, password)).onEach {
			_loginState.value = it
		}.launchIn(viewModelScope)
	}

	fun clearLoginState() {
		_loginState.value = null
		_quickConnectState.value = UnknownQuickConnectState
	}

	/**
	 * Start a new Quick Connect flow. Does nothing when already active.
	 */
	suspend fun initiateQuickconnect() {
		// Already initialized
		if (quickConnectState.value != UnknownQuickConnectState) return

		val server = server.value ?: return
		_quickConnectState.emit(UnknownQuickConnectState)
		quickConnectSecret = null

		try {
			val response = withContext(Dispatchers.IO) {
				quickConnectApi.update(
					baseUrl = server.address,
					deviceInfo = defaultDeviceInfo.forUser(UUID.randomUUID()),
				)

				quickConnectApi.quickConnectApi.initiateQuickConnect().content
			}

			quickConnectSecret = response.secret
			_quickConnectState.emit(PendingQuickConnectState(response.code))
		} catch (err: ApiClientException) {
			Timber.e(err, "Unable to initiate QuickConnect")
			_quickConnectState.emit(UnavailableQuickConnectState)
		}

		// Update every 5 seconds until QuickConnect is inactive or the view model is cancelled
		viewModelScope.launch {
			while (isActive) {
				delay(5.seconds)
				if (!updateQuickConnectState()) break
			}
		}
	}

	/**
	 * Update the Quick Connect state.
	 *
	 * @return true when Quick Connect is active, false when inactive.
	 */
	private suspend fun updateQuickConnectState(): Boolean {
		val server = server.value ?: return false
		val secret = quickConnectSecret ?: return false

		try {
			val state = withContext(Dispatchers.IO) {
				quickConnectApi.quickConnectApi.getQuickConnectState(secret = secret).content
			}

			if (state.authenticated) {
				_quickConnectState.emit(ConnectedQuickConnectState)

				authenticationRepository.authenticate(server, QuickConnectAuthenticateMethod(state.secret)).collect {
					_loginState.emit(it)
				}

				return false
			} else {
				_quickConnectState.emit(PendingQuickConnectState(state.code))
				return true
			}
		} catch (err: ApiClientException) {
			Timber.e(err, "Unable to initiate QuickConnect")
			_quickConnectState.emit(UnavailableQuickConnectState)
			return false
		}
	}

	fun setServer(id: UUID?) {
		_server.value = serverRepository.storedServers.value
			.firstOrNull { it.id == id }
	}
}
