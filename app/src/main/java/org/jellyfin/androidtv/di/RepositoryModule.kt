package org.jellyfin.androidtv.di

import org.jellyfin.androidtv.data.repository.MovieRepository
import org.koin.dsl.module

val repositoryModule = module {
    
    // Repositories
    single<MovieRepository> { 
        MovieRepository(
            traktApiService = get(),
            tmdbApiService = get(),
            database = get()
        )
    }
}