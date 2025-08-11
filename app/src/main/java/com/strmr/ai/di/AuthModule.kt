package com.strmr.ai.di

import com.strmr.ai.auth.AccountManagerMigration
import com.strmr.ai.auth.repository.AuthenticationRepository
import com.strmr.ai.auth.repository.AuthenticationRepositoryImpl
import com.strmr.ai.auth.repository.ServerRepository
import com.strmr.ai.auth.repository.ServerRepositoryImpl
import com.strmr.ai.auth.repository.ServerUserRepository
import com.strmr.ai.auth.repository.ServerUserRepositoryImpl
import com.strmr.ai.auth.repository.SessionRepository
import com.strmr.ai.auth.repository.SessionRepositoryImpl
import com.strmr.ai.auth.store.AuthenticationPreferences
import com.strmr.ai.auth.store.AuthenticationStore
import org.koin.dsl.module

val authModule = module {
	single { AccountManagerMigration(get()) }
	single { AuthenticationStore(get(), get()) }
	single { AuthenticationPreferences(get()) }

	single<AuthenticationRepository> {
		AuthenticationRepositoryImpl(get(), get(), get(), get(), get(), get(defaultDeviceInfo))
	}
	single<ServerRepository> { ServerRepositoryImpl(get(), get()) }
	single<ServerUserRepository> { ServerUserRepositoryImpl(get(), get()) }
	single<SessionRepository> {
		SessionRepositoryImpl(get(), get(), get(), get(), get(defaultDeviceInfo), get(), get(), get())
	}
}
