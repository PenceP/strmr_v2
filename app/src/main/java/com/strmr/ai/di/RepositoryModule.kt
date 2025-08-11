package com.strmr.ai.di

import com.strmr.ai.data.repository.MovieRepository
import com.strmr.ai.data.repository.ImprovedMovieRepository
import com.strmr.ai.data.repository.ShowRepository
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