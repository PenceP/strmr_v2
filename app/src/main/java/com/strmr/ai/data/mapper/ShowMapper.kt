package com.strmr.ai.data.mapper

import com.strmr.ai.data.api.model.tmdb.TmdbShow
import com.strmr.ai.data.api.model.tmdb.TmdbShowDetails
import com.strmr.ai.data.api.model.trakt.TraktShow
import com.strmr.ai.data.api.model.trakt.TraktShowResponse
import com.strmr.ai.data.database.entity.Show

object ShowMapper {
    
    /**
     * Extract US content rating from TMDB show details
     */
    private fun extractContentRating(tmdbShowDetails: TmdbShowDetails?): String? {
        return tmdbShowDetails?.contentRatings?.results?.find { 
            it.iso31661 == "US" 
        }?.rating
    }
    
    /**
     * Maps a TraktShowResponse and TmdbShowDetails to a Room Show entity with complete data
     */
    fun mapTraktAndTmdbToShow(
        traktResponse: TraktShowResponse,
        tmdbShowDetails: TmdbShowDetails?,
        category: String
    ): Show {
        val traktShow = traktResponse.show
        return Show(
            id = tmdbShowDetails?.id ?: traktShow.ids.tmdb ?: 0,
            name = tmdbShowDetails?.name ?: traktShow.title,
            overview = tmdbShowDetails?.overview ?: traktShow.overview,
            firstAirDate = tmdbShowDetails?.firstAirDate ?: traktShow.year?.toString(),
            lastAirDate = tmdbShowDetails?.lastAirDate,
            posterPath = tmdbShowDetails?.posterPath,
            backdropPath = tmdbShowDetails?.backdropPath,
            voteAverage = tmdbShowDetails?.voteAverage ?: traktShow.rating ?: 0.0,
            voteCount = tmdbShowDetails?.voteCount ?: traktShow.votes ?: 0,
            genreIds = tmdbShowDetails?.genres?.map { it.id } ?: emptyList(),
            originalLanguage = tmdbShowDetails?.originalLanguage ?: traktShow.language ?: "en",
            originalName = tmdbShowDetails?.originalName ?: traktShow.title,
            popularity = tmdbShowDetails?.popularity ?: 0.0,
            adult = tmdbShowDetails?.adult ?: false,
            originCountry = tmdbShowDetails?.originCountry ?: emptyList(),
            status = tmdbShowDetails?.status,
            numberOfEpisodes = tmdbShowDetails?.numberOfEpisodes,
            numberOfSeasons = tmdbShowDetails?.numberOfSeasons,
            episodeRunTime = tmdbShowDetails?.episodeRunTime ?: emptyList(),
            networks = tmdbShowDetails?.networks?.map { it.name } ?: emptyList(),
            traktId = traktShow.ids.trakt,
            traktSlug = traktShow.ids.slug,
            contentRating = extractContentRating(tmdbShowDetails),
            rottenTomatoesRating = null // Not available from current APIs
        )
    }
    
    /**
     * Maps a TraktShow to a Show entity (for popular shows endpoint)
     */
    fun mapTraktToShow(
        traktShow: TraktShow,
        category: String
    ): Show {
        return Show(
            id = traktShow.ids.tmdb ?: 0,
            name = traktShow.title,
            overview = traktShow.overview,
            firstAirDate = traktShow.year?.toString(),
            lastAirDate = null,
            posterPath = null, // Will need TMDB lookup
            backdropPath = null, // Will need TMDB lookup
            voteAverage = traktShow.rating ?: 0.0,
            voteCount = traktShow.votes ?: 0,
            genreIds = emptyList(), // Will need TMDB lookup
            originalLanguage = traktShow.language ?: "en",
            originalName = traktShow.title,
            popularity = 0.0, // Will need TMDB lookup
            adult = false,
            originCountry = emptyList(), // Will need TMDB lookup
            status = null,
            numberOfEpisodes = null,
            numberOfSeasons = null,
            episodeRunTime = emptyList(),
            networks = emptyList(),
            traktId = traktShow.ids.trakt,
            traktSlug = traktShow.ids.slug,
            contentRating = null,
            rottenTomatoesRating = null
        )
    }
    
    /**
     * Maps a TmdbShow to a Show entity (if we had pure TMDB data)
     */
    fun mapTmdbToShow(tmdbShow: TmdbShow): Show {
        return Show(
            id = tmdbShow.id,
            name = tmdbShow.name,
            overview = tmdbShow.overview,
            firstAirDate = tmdbShow.firstAirDate,
            lastAirDate = null,
            posterPath = tmdbShow.posterPath,
            backdropPath = tmdbShow.backdropPath,
            voteAverage = tmdbShow.voteAverage,
            voteCount = tmdbShow.voteCount,
            genreIds = tmdbShow.genreIds,
            originalLanguage = tmdbShow.originalLanguage,
            originalName = tmdbShow.originalName,
            popularity = tmdbShow.popularity,
            adult = tmdbShow.adult ?: false,
            originCountry = tmdbShow.originCountry,
            status = null,
            numberOfEpisodes = null,
            numberOfSeasons = null,
            episodeRunTime = emptyList(),
            networks = emptyList(),
            traktId = null,
            traktSlug = null,
            contentRating = null,
            rottenTomatoesRating = null
        )
    }
}