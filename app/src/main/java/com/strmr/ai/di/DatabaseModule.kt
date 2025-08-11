package com.strmr.ai.di

import com.strmr.ai.data.database.AppDatabase
import com.strmr.ai.data.database.dao.MovieDao
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