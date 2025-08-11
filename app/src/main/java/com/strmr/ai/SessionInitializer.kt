package com.strmr.ai

import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.startup.AppInitializer
import androidx.startup.Initializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.strmr.ai.auth.repository.SessionRepository
import com.strmr.ai.di.KoinInitializer

@Suppress("unused")
class SessionInitializer : Initializer<Unit> {
	override fun create(context: Context) {
		val koin = AppInitializer.getInstance(context)
			.initializeComponent(KoinInitializer::class.java)
			.koin

		// Restore system session
		ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.IO) {
			koin.get<SessionRepository>().restoreSession(destroyOnly = false)
		}
	}

	override fun dependencies() = listOf(KoinInitializer::class.java)
}
