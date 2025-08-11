package com.strmr.ai.ui.startup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.strmr.ai.R
import com.strmr.ai.auth.model.ApiClientErrorLoginState
import com.strmr.ai.auth.model.AuthenticatedState
import com.strmr.ai.auth.model.AuthenticatingState
import com.strmr.ai.auth.model.ConnectedQuickConnectState
import com.strmr.ai.auth.model.PendingQuickConnectState
import com.strmr.ai.auth.model.RequireSignInState
import com.strmr.ai.auth.model.ServerUnavailableState
import com.strmr.ai.auth.model.ServerVersionNotSupported
import com.strmr.ai.auth.model.UnavailableQuickConnectState
import com.strmr.ai.auth.model.UnknownQuickConnectState
import com.strmr.ai.auth.repository.ServerRepository
import com.strmr.ai.databinding.FragmentUserLoginQuickConnectBinding
import com.strmr.ai.ui.startup.UserLoginViewModel
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class UserLoginQuickConnectFragment : Fragment() {
	private val userLoginViewModel: UserLoginViewModel by activityViewModel()
	private var _binding: FragmentUserLoginQuickConnectBinding? = null
	private val binding get() = _binding!!

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		_binding = FragmentUserLoginQuickConnectBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		userLoginViewModel.clearLoginState()

		lifecycleScope.launch {
			viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
				userLoginViewModel.initiateQuickconnect()

				// React to Quick Connect specific state
				userLoginViewModel.quickConnectState.onEach { state ->
					when (state) {
						is PendingQuickConnectState -> {
							binding.quickConnectCode.text = state.code.formatCode()
							binding.loading.isVisible = false
						}

						UnavailableQuickConnectState,
						UnknownQuickConnectState,
						ConnectedQuickConnectState -> binding.loading.isVisible = true
					}
				}.launchIn(this)

				// React to login state
				userLoginViewModel.loginState.onEach { state ->
					when (state) {
						is ServerVersionNotSupported -> binding.error.setText(
							getString(
								R.string.server_issue_outdated_version,
								state.server.version,
								ServerRepository.recommendedServerVersion.toString()
							)
						)

						AuthenticatingState -> binding.error.setText(R.string.login_authenticating)
						RequireSignInState -> binding.error.setText(R.string.login_invalid_credentials)
						ServerUnavailableState,
						is ApiClientErrorLoginState -> binding.error.setText(R.string.login_server_unavailable)
						// Do nothing because the activity will respond to the new session
						AuthenticatedState -> Unit
						// Not initialized
						null -> Unit
					}
				}.launchIn(this)
			}
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()

		_binding = null
	}

	/**
	 * Add space after every 3 characters so "420420" becomes "420 420".
	 */
	private fun String.formatCode() = buildString {
		@Suppress("MagicNumber")
		val interval = 3
		this@formatCode.forEachIndexed { index, character ->
			if (index != 0 && index % interval == 0) append(" ")
			append(character)
		}
	}
}
