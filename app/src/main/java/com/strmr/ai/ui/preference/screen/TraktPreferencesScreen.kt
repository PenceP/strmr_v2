package com.strmr.ai.ui.preference.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import com.strmr.ai.R
import com.strmr.ai.data.entity.TraktSession
import com.strmr.ai.data.repository.TraktRepository
import com.strmr.ai.data.trakt.DeviceCodeResponse
import com.strmr.ai.ui.preference.dsl.OptionsFragment
import com.strmr.ai.ui.preference.dsl.action
import com.strmr.ai.ui.preference.dsl.checkbox
import com.strmr.ai.ui.preference.dsl.optionsScreen
import com.strmr.ai.preference.UserPreferences
import org.koin.android.ext.android.inject

class TraktPreferencesScreen : OptionsFragment() {
	private val userPreferences: UserPreferences by inject()
	private val traktRepository: TraktRepository by inject()
	
	private var currentSession: TraktSession? = null
	private var authInProgress = false
	private var currentDeviceCode: DeviceCodeResponse? = null
	private var authDialog: AlertDialog? = null
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		
		viewLifecycleOwner.lifecycleScope.launch {
			viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
				traktRepository.getSessionFlow().collect { session ->
					currentSession = session
					rebuild()
				}
			}
		}
	}
	
	override val screen by optionsScreen {
		setTitle(R.string.pref_trakt)
		
		val isLoggedIn = currentSession != null
		
		if (isLoggedIn) {
			// Show logged in state with username and options
			category {
				setTitle(R.string.pref_trakt_account)
				
				action {
					setTitle(R.string.pref_trakt_logged_in_as)
					content = currentSession?.username ?: getString(R.string.pref_trakt_logged_in)
					icon = R.drawable.ic_check
				}
				
				action {
					setTitle(R.string.pref_trakt_logout)
					setContent(R.string.pref_trakt_logout_description)
					icon = R.drawable.ic_logout
					onActivate = {
						viewLifecycleOwner.lifecycleScope.launch {
							traktRepository.logout()
							Toast.makeText(requireContext(), "Logged out from Trakt", Toast.LENGTH_SHORT).show()
						}
					}
				}
			}
			
			// Sync Settings
			category {
				setTitle(R.string.pref_trakt_sync_settings)
				
				checkbox {
					setTitle(R.string.pref_trakt_sync_on_startup)
					setContent(R.string.pref_trakt_sync_on_startup_description)
					bind(userPreferences, UserPreferences.traktSyncOnStartup)
				}
				
				checkbox {
					setTitle(R.string.pref_trakt_sync_on_watched)
					setContent(R.string.pref_trakt_sync_on_watched_description)
					bind(userPreferences, UserPreferences.traktSyncOnWatched)
				}
			}
			
			// Manual Actions
			category {
				setTitle(R.string.pref_trakt_actions)
				
				action {
					setTitle(R.string.pref_trakt_sync_now)
					setContent(R.string.pref_trakt_sync_now_description)
					icon = R.drawable.ic_loop
					onActivate = {
						// TODO: Implement Trakt sync
						// This will trigger a manual sync with Trakt
					}
				}
			}
		} else {
			// Show login option
			category {
				action {
					setTitle(if (authInProgress) R.string.pref_trakt_auth_in_progress else R.string.pref_trakt_login)
					setContent(if (authInProgress) R.string.pref_trakt_waiting_for_auth else R.string.pref_trakt_login_oauth_description)
					icon = if (authInProgress) R.drawable.ic_loop else R.drawable.ic_user
					onActivate = {
						if (authInProgress) {
							// Show the device code again or cancel
							currentDeviceCode?.let { showDeviceCodeInfo(it) }
						} else {
							startTraktAuth()
						}
					}
				}
			}
			
			category {
				setTitle(R.string.pref_trakt_about)
				
				action {
					setTitle(R.string.pref_trakt_what_is)
					setContent(R.string.pref_trakt_what_is_description)
					icon = R.drawable.ic_help
				}
				
				if (authInProgress) {
					action {
						setTitle(R.string.pref_trakt_cancel_auth)
						setContent(R.string.pref_trakt_cancel_auth_description)
						icon = R.drawable.ic_delete
						onActivate = {
							cancelTraktAuth()
						}
					}
				}
			}
		}
	}
	
	private fun startTraktAuth() {
		Log.d("TraktPreferencesScreen", "🎬 Starting Trakt authentication...")
		Log.d("TraktPreferencesScreen", "📋 BuildConfig check:")
		Log.d("TraktPreferencesScreen", "  - TRAKT_CLIENT_ID: ${com.strmr.ai.BuildConfig.TRAKT_CLIENT_ID}")
		Log.d("TraktPreferencesScreen", "  - TRAKT_CLIENT_SECRET length: ${com.strmr.ai.BuildConfig.TRAKT_CLIENT_SECRET.length}")
		Log.d("TraktPreferencesScreen", "  - Is CLIENT_ID empty? ${com.strmr.ai.BuildConfig.TRAKT_CLIENT_ID.isEmpty()}")
		Log.d("TraktPreferencesScreen", "  - Is CLIENT_SECRET empty? ${com.strmr.ai.BuildConfig.TRAKT_CLIENT_SECRET.isEmpty()}")
		
		authInProgress = true
		rebuild()
		
		viewLifecycleOwner.lifecycleScope.launch {
			try {
				Log.d("TraktPreferencesScreen", "📡 Calling traktRepository.startDeviceAuth()...")
				val deviceCode = traktRepository.startDeviceAuth()
				currentDeviceCode = deviceCode
				
				// Show the device code to user
				showDeviceCodeInfo(deviceCode)
				
				// Start polling for authentication
				traktRepository.startPollingInBackground(deviceCode.device_code, deviceCode.interval) { result ->
					authInProgress = false
					currentDeviceCode = null
					authDialog?.dismiss()
					authDialog = null
					rebuild()
					
					result.fold(
						onSuccess = {
							Toast.makeText(requireContext(), "Successfully connected to Trakt!", Toast.LENGTH_LONG).show()
						},
						onFailure = { error ->
							Toast.makeText(requireContext(), "Authentication failed: ${error.message}", Toast.LENGTH_LONG).show()
						}
					)
				}
			} catch (e: Exception) {
				authInProgress = false
				currentDeviceCode = null
				rebuild()
				Toast.makeText(requireContext(), "Failed to start authentication: ${e.message}", Toast.LENGTH_LONG).show()
			}
		}
	}
	
	private fun showDeviceCodeInfo(deviceCode: DeviceCodeResponse) {
		val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
		val clip = ClipData.newPlainText("Trakt Device Code", deviceCode.user_code)
		clipboard.setPrimaryClip(clip)
		
		// Show dialog with the device code
		val message = """To connect your Trakt account:

1. Go to: ${deviceCode.verification_url}
2. Enter this code: ${deviceCode.user_code}

The code has been copied to your clipboard.

The app will automatically detect when you complete the authorization."""
		
		authDialog = AlertDialog.Builder(requireContext())
			.setTitle("Trakt Authentication")
			.setMessage(message)
			.setPositiveButton("Open Website") { _, _ ->
				// Open the verification URL in browser
				try {
					val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deviceCode.verification_url))
					startActivity(intent)
				} catch (e: Exception) {
					Toast.makeText(requireContext(), "Please open ${deviceCode.verification_url} manually", Toast.LENGTH_LONG).show()
				}
			}
			.setNeutralButton("Copy Code") { _, _ ->
				// Copy code again (already copied above, but user might want it again)
				Toast.makeText(requireContext(), "Code copied: ${deviceCode.user_code}", Toast.LENGTH_SHORT).show()
			}
			.setNegativeButton("Cancel") { _, _ ->
				cancelTraktAuth()
			}
			.setCancelable(false) // Prevent accidental dismissal
			.show()
	}
	
	private fun cancelTraktAuth() {
		authInProgress = false
		currentDeviceCode = null
		authDialog?.dismiss()
		authDialog = null
		traktRepository.stopPolling()
		rebuild()
		Toast.makeText(requireContext(), "Authentication cancelled", Toast.LENGTH_SHORT).show()
	}
}