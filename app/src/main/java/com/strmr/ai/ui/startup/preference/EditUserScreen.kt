package com.strmr.ai.ui.startup.preference

import com.strmr.ai.R
import com.strmr.ai.auth.repository.AuthenticationRepository
import com.strmr.ai.auth.repository.ServerUserRepository
import com.strmr.ai.ui.preference.dsl.OptionsFragment
import com.strmr.ai.ui.preference.dsl.action
import com.strmr.ai.ui.preference.dsl.optionsScreen
import com.strmr.ai.ui.startup.StartupViewModel
import com.strmr.ai.util.getValue
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import java.util.UUID

class EditUserScreen : OptionsFragment() {
	private val startupViewModel: StartupViewModel by activityViewModel()
	private val authenticationRepository by inject<AuthenticationRepository>()
	private val serverUserRepository: ServerUserRepository by inject()

	override val screen by optionsScreen {
		val serverUUID = requireNotNull(
			requireArguments().getValue<UUID>(ARG_SERVER_UUID)
		) { "Missing server id" }
		val userUUID = requireNotNull(
			requireArguments().getValue<UUID>(ARG_USER_UUID)
		) { "Missing user id" }

		val server = requireNotNull(startupViewModel.getServer(serverUUID)) { "Server not found" }
		val user = requireNotNull(
			serverUserRepository.getStoredServerUsers(server).find { it.id == userUUID }
		) { "User not found" }

		title = context?.getString(R.string.lbl_user_server, user.name, server.name)

		category {
			action {
				setTitle(R.string.lbl_sign_out)
				setContent(R.string.lbl_sign_out_content)

				icon = R.drawable.ic_logout

				onActivate = {
					authenticationRepository.logout(user)
					rebuild()
				}

				// Disable action when access token is not set (already signed out)
				depends {
					user.accessToken != null
				}
			}

			action {
				setTitle(R.string.lbl_remove)
				setContent(R.string.lbl_remove_user_content)

				icon = R.drawable.ic_delete

				onActivate = {
					serverUserRepository.deleteStoredUser(user)
					parentFragmentManager.popBackStack()
				}
			}
		}
	}

	companion object {
		const val ARG_SERVER_UUID = "server_uuid"
		const val ARG_USER_UUID = "user_uuid"
	}
}
