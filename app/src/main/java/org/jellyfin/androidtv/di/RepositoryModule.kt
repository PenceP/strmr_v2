package org.jellyfin.androidtv.di

import org.jellyfin.androidtv.data.repository.MovieRepository
import org.jellyfin.androidtv.data.repository.ImprovedMovieRepository
import org.jellyfin.androidtv.data.repository.ShowRepository
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
    
    single<ImprovedMovieRepository> { 
        ImprovedMovieRepository(
            traktApiService = get(),
            tmdbApiService = get(),
            database = get()
        )
    }
    
    single<ShowRepository> { 
        ShowRepository(
            traktApiService = get(),
            tmdbApiService = get(),
            database = get()
        )
    }
}