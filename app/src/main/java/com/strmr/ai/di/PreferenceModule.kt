package com.strmr.ai.di

import com.strmr.ai.preference.LiveTvPreferences
import com.strmr.ai.preference.PreferencesRepository
import com.strmr.ai.preference.SystemPreferences
import com.strmr.ai.preference.TelemetryPreferences
import com.strmr.ai.preference.UserPreferences
import com.strmr.ai.preference.UserSettingPreferences
import org.koin.dsl.module

val preferenceModule = module {
	single { PreferencesRepository(get(), get(), get()) }

	single { LiveTvPreferences(get()) }
	single { UserSettingPreferences(get()) }
	single { UserPreferences(get()) }
	single { SystemPreferences(get()) }
	single { TelemetryPreferences(get()) }
}
