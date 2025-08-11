package com.strmr.ai.ui.preference.category

import android.os.Build
import com.strmr.ai.BuildConfig
import com.strmr.ai.R
import com.strmr.ai.ui.preference.dsl.OptionsScreen
import com.strmr.ai.ui.preference.dsl.link
import com.strmr.ai.ui.preference.screen.LicensesScreen

fun OptionsScreen.aboutCategory() = category {
	setTitle(R.string.pref_about_title)

	link {
		// Hardcoded strings for troubleshooting purposes
		title = "Strmr app version"
		content = "strmr ${BuildConfig.VERSION_NAME} ${BuildConfig.BUILD_TYPE}"
		icon = R.drawable.ic_jellyfin
	}

	link {
		setTitle(R.string.pref_device_model)
		content = "${Build.MANUFACTURER} ${Build.MODEL}"
		icon = R.drawable.ic_tv
	}

	link {
		setTitle(R.string.licenses_link)
		setContent(R.string.licenses_link_description)
		icon = R.drawable.ic_guide
		withFragment<LicensesScreen>()
	}
}
