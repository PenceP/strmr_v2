package com.strmr.ai.ui.preference.screen

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.strmr.ai.R
import com.strmr.ai.ui.preference.dsl.OptionsFragment
import com.strmr.ai.ui.preference.dsl.action
import com.strmr.ai.ui.preference.dsl.optionsScreen
import com.strmr.ai.preference.UserPreferences
import org.koin.android.ext.android.inject

class PremiumizePreferencesScreen : OptionsFragment() {
	private val userPreferences: UserPreferences by inject()
	
	// TODO: Implement Premiumize preferences storage
	// For now, using temporary variables
	private var premiumizeLoggedIn = false
	private var premiumizeUsername = ""
	private var premiumizeAccountType = ""
	private var premiumizeExpiryDate = ""
	private var premiumizeSpaceUsed = ""
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		rebuild()
	}
	
	override val screen by optionsScreen {
		setTitle(R.string.pref_premiumize)
		
		val isLoggedIn = premiumizeLoggedIn
		
		if (isLoggedIn) {
			// Show logged in state with account info
			category {
				setTitle(R.string.pref_premiumize_account)
				
				action {
					setTitle(R.string.pref_premiumize_logged_in_as)
					content = if (premiumizeUsername.isNotEmpty()) premiumizeUsername else getString(R.string.pref_premiumize_logged_in)
					icon = R.drawable.ic_check
				}
				
				// Account details
				if (premiumizeAccountType.isNotEmpty()) {
					action {
						setTitle(R.string.pref_premiumize_account_type)
						content = premiumizeAccountType
						icon = R.drawable.ic_star
					}
				}
				
				if (premiumizeExpiryDate.isNotEmpty()) {
					action {
						setTitle(R.string.pref_premiumize_expiry)
						content = premiumizeExpiryDate
						icon = R.drawable.ic_calendar
					}
				}
				
				if (premiumizeSpaceUsed.isNotEmpty()) {
					action {
						setTitle(R.string.pref_premiumize_space_used)
						content = premiumizeSpaceUsed
						icon = R.drawable.ic_folder
					}
				}
				
				action {
					setTitle(R.string.pref_premiumize_logout)
					setContent(R.string.pref_premiumize_logout_description)
					icon = R.drawable.ic_logout
					onActivate = {
						// TODO: Implement Premiumize logout
						premiumizeLoggedIn = false
						premiumizeUsername = ""
						premiumizeAccountType = ""
						premiumizeExpiryDate = ""
						premiumizeSpaceUsed = ""
						rebuild()
					}
				}
			}
			
			// Settings
			category {
				setTitle(R.string.pref_premiumize_settings)
				
				// TODO: Implement checkbox preferences for settings
				// These will be implemented when Premiumize integration is added
				action {
					setTitle(R.string.pref_premiumize_prefer_cached)
					setContent(R.string.pref_premiumize_prefer_cached_description)
					icon = R.drawable.ic_settings
				}
				
				action {
					setTitle(R.string.pref_premiumize_auto_transcode)
					setContent(R.string.pref_premiumize_auto_transcode_description)
					icon = R.drawable.ic_settings
				}
			}
			
			// Actions
			category {
				setTitle(R.string.pref_premiumize_actions)
				
				action {
					setTitle(R.string.pref_premiumize_refresh_account)
					setContent(R.string.pref_premiumize_refresh_account_description)
					icon = R.drawable.ic_loop
					onActivate = {
						// TODO: Implement account info refresh
						// This will fetch latest account details from Premiumize API
					}
				}
				
				action {
					setTitle(R.string.pref_premiumize_clear_cache)
					setContent(R.string.pref_premiumize_clear_cache_description)
					icon = R.drawable.ic_trash
					onActivate = {
						// TODO: Implement cache clearing
						// This will clear cached torrents from Premiumize
					}
				}
			}
		} else {
			// Show login option
			category {
				action {
					setTitle(R.string.pref_premiumize_login)
					setContent(R.string.pref_premiumize_login_description)
					icon = R.drawable.ic_user
					onActivate = {
						// TODO: Implement Premiumize API key input and validation
						// This will show a dialog to enter API key,
						// validate it and fetch account info
						// After successful validation, save account details and rebuild
					}
				}
			}
			
			category {
				setTitle(R.string.pref_premiumize_about)
				
				action {
					setTitle(R.string.pref_premiumize_what_is)
					setContent(R.string.pref_premiumize_what_is_description)
					icon = R.drawable.ic_help
				}
				
				action {
					setTitle(R.string.pref_premiumize_get_api_key)
					setContent(R.string.pref_premiumize_get_api_key_description)
					icon = R.drawable.ic_settings
					onActivate = {
						// TODO: Open Premiumize website to get API key
						// This will open https://www.premiumize.me/account
					}
				}
			}
		}
	}
}