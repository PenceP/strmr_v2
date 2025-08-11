package com.strmr.ai.auth.model

import com.strmr.ai.R
import org.jellyfin.preference.PreferenceEnum

enum class AuthenticationSortBy(
	override val nameRes: Int
) : PreferenceEnum {
	LAST_USE(R.string.last_use),
	ALPHABETICAL(R.string.alphabetical);
}
