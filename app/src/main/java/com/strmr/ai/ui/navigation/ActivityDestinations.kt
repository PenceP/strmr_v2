package com.strmr.ai.ui.navigation

import android.content.Context
import android.content.Intent
import androidx.core.os.bundleOf
import com.strmr.ai.ui.browsing.DisplayPreferencesScreen
import com.strmr.ai.ui.livetv.GuideFiltersScreen
import com.strmr.ai.ui.livetv.GuideOptionsScreen
import com.strmr.ai.ui.playback.ExternalPlayerActivity
import com.strmr.ai.ui.preference.PreferencesActivity
import com.strmr.ai.ui.preference.dsl.OptionsFragment
import com.strmr.ai.ui.preference.screen.UserPreferencesScreen
import com.strmr.ai.ui.startup.StartupActivity
import kotlin.time.Duration

object ActivityDestinations {
	private inline fun <reified T : OptionsFragment> preferenceIntent(
		context: Context,
		vararg screenArguments: Pair<String, Any?>
	) = Intent(context, PreferencesActivity::class.java).apply {
		putExtras(
			bundleOf(
				PreferencesActivity.EXTRA_SCREEN to T::class.qualifiedName,
				PreferencesActivity.EXTRA_SCREEN_ARGS to bundleOf(*screenArguments),
			)
		)
	}

	fun userPreferences(context: Context) = preferenceIntent<UserPreferencesScreen>(context)
	fun displayPreferences(context: Context, displayPreferencesId: String, allowViewSelection: Boolean) =
		preferenceIntent<DisplayPreferencesScreen>(
			context,
			DisplayPreferencesScreen.ARG_PREFERENCES_ID to displayPreferencesId,
			DisplayPreferencesScreen.ARG_ALLOW_VIEW_SELECTION to allowViewSelection,
		)

	fun liveTvGuideFilterPreferences(context: Context) = preferenceIntent<GuideFiltersScreen>(context)
	fun liveTvGuideOptionPreferences(context: Context) = preferenceIntent<GuideOptionsScreen>(context)

	fun externalPlayer(context: Context, position: Duration = Duration.ZERO) = Intent(context, ExternalPlayerActivity::class.java).apply {
		putExtras(
			bundleOf(
				ExternalPlayerActivity.EXTRA_POSITION to position.inWholeMilliseconds
			)
		)
	}

	fun startup(context: Context, hideSplash: Boolean = true) = Intent(context, StartupActivity::class.java).apply {
		putExtra(StartupActivity.EXTRA_HIDE_SPLASH, hideSplash)
		// Remove history to prevent user going back to current activity
		addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
	}
}
