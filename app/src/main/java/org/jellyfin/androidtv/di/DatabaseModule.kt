package org.jellyfin.androidtv.di

import org.jellyfin.androidtv.data.database.AppDatabase
import org.jellyfin.androidtv.data.database.dao.MovieDao
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    
    // Room Database
    single<AppDatabase> { 
        AppDatabase.getDatabase(androidContext())
    }
    
    // DAOs
    single<MovieDao> { get<AppDatabase>().movieDao() }
}