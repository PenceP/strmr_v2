package org.jellyfin.androidtv.data.mapper

import org.jellyfin.androidtv.data.api.model.tmdb.TmdbMovie
import org.jellyfin.androidtv.data.api.model.tmdb.TmdbMovieDetails
import org.jellyfin.androidtv.data.api.model.tmdb.TmdbMovieReleasesResponse
import org.jellyfin.androidtv.data.api.model.trakt.TraktMovie
import org.jellyfin.androidtv.data.api.model.trakt.TraktMovieResponse
import org.jellyfin.androidtv.data.database.entity.Movie

object MovieMapper {
    
    /**
     * Extract US certification from release dates response
     */
    private fun extractCertification(releasesResponse: TmdbMovieReleasesResponse?): String? {
        return releasesResponse?.results?.find { it.countryCode == "US" }
            ?.releaseDates?.firstOrNull { it.certification.isNotEmpty() }
            ?.certification
    }
    
    /**
     * Maps a TraktMovieResponse and TmdbMovieDetails to a Room Movie entity with complete data
     */
    fun mapTraktAndTmdbToMovie(
        traktResponse: TraktMovieResponse,
        tmdbMovieDetails: TmdbMovieDetails?,
        category: String,
        releasesResponse: TmdbMovieReleasesResponse? = null
    ): Movie {
        val traktMovie = traktResponse.movie
        return Movie(
            id = tmdbMovieDetails?.id ?: traktMovie.ids.tmdb ?: 0,
            title = tmdbMovieDetails?.title ?: traktMovie.title,
            overview = tmdbMovieDetails?.overview ?: traktMovie.overview,
            releaseDate = tmdbMovieDetails?.releaseDate ?: traktMovie.year?.toString(),
            posterPath = tmdbMovieDetails?.posterPath,
            backdropPath = tmdbMovieDetails?.backdropPath,
            voteAverage = tmdbMovieDetails?.voteAverage ?: traktMovie.rating ?: 0.0,
            voteCount = tmdbMovieDetails?.voteCount ?: traktMovie.votes ?: 0,
            genreIds = tmdbMovieDetails?.genres?.map { it.id } ?: emptyList(),
            originalLanguage = tmdbMovieDetails?.originalLanguage ?: traktMovie.language ?: "en",
            originalTitle = tmdbMovieDetails?.originalTitle ?: traktMovie.title,
            popularity = tmdbMovieDetails?.popularity ?: 0.0,
            video = tmdbMovieDetails?.video ?: false,
            adult = tmdbMovieDetails?.adult ?: false,
            traktId = traktMovie.ids.trakt,
            traktSlug = traktMovie.ids.slug,
            runtime = tmdbMovieDetails?.runtime,
            certification = extractCertification(releasesResponse),
            rottenTomatoesRating = null, // Not available from current APIs
            collectionId = tmdbMovieDetails?.belongsToCollection?.id,
            collectionName = tmdbMovieDetails?.belongsToCollection?.name
        )
    }
    
    /**
     * Maps a TraktMovie (direct) and TmdbMovieDetails to a Room Movie entity with complete data
     */
    fun mapTraktAndTmdbToMovie(
        traktMovie: TraktMovie,
        tmdbMovieDetails: TmdbMovieDetails?,
        category: String,
        releasesResponse: TmdbMovieReleasesResponse? = null
    ): Movie {
        return Movie(
            id = tmdbMovieDetails?.id ?: traktMovie.ids.tmdb ?: 0,
            title = tmdbMovieDetails?.title ?: traktMovie.title,
            overview = tmdbMovieDetails?.overview ?: traktMovie.overview,
            releaseDate = tmdbMovieDetails?.releaseDate ?: traktMovie.year?.toString(),
            posterPath = tmdbMovieDetails?.posterPath,
            backdropPath = tmdbMovieDetails?.backdropPath,
            voteAverage = tmdbMovieDetails?.voteAverage ?: traktMovie.rating ?: 0.0,
            voteCount = tmdbMovieDetails?.voteCount ?: traktMovie.votes ?: 0,
            genreIds = tmdbMovieDetails?.genres?.map { it.id } ?: emptyList(),
            originalLanguage = tmdbMovieDetails?.originalLanguage ?: traktMovie.language ?: "en",
            originalTitle = tmdbMovieDetails?.originalTitle ?: traktMovie.title,
            popularity = tmdbMovieDetails?.popularity ?: 0.0,
            video = tmdbMovieDetails?.video ?: false,
            adult = tmdbMovieDetails?.adult ?: false,
            traktId = traktMovie.ids.trakt,
            traktSlug = traktMovie.ids.slug,
            runtime = tmdbMovieDetails?.runtime,
            certification = extractCertification(releasesResponse),
            rottenTomatoesRating = null, // Not available from current APIs
            collectionId = tmdbMovieDetails?.belongsToCollection?.id,
            collectionName = tmdbMovieDetails?.belongsToCollection?.name
        )
    }
    
    /**
     * Maps basic data without detailed fields (for backwards compatibility)
     */
    fun mapTraktAndTmdbToMovie(
        traktResponse: TraktMovieResponse,
        tmdbMovie: TmdbMovie?,
        category: String
    ): Movie {
        val traktMovie = traktResponse.movie
        return Movie(
            id = tmdbMovie?.id ?: traktMovie.ids.tmdb ?: 0,
            title = tmdbMovie?.title ?: traktMovie.title,
            overview = tmdbMovie?.overview ?: traktMovie.overview,
            releaseDate = tmdbMovie?.releaseDate ?: traktMovie.year?.toString(),
            posterPath = tmdbMovie?.posterPath,
            backdropPath = tmdbMovie?.backdropPath,
            voteAverage = tmdbMovie?.voteAverage ?: traktMovie.rating ?: 0.0,
            voteCount = tmdbMovie?.voteCount ?: traktMovie.votes ?: 0,
            genreIds = tmdbMovie?.genreIds ?: emptyList(),
            originalLanguage = tmdbMovie?.originalLanguage ?: traktMovie.language ?: "en",
            originalTitle = tmdbMovie?.originalTitle ?: traktMovie.title,
            popularity = tmdbMovie?.popularity ?: 0.0,
            video = tmdbMovie?.video ?: false,
            adult = tmdbMovie?.adult ?: false,
            traktId = traktMovie.ids.trakt,
            traktSlug = traktMovie.ids.slug,
            runtime = null, // Not available in TmdbMovie
            certification = null, // Not available without releases call
            rottenTomatoesRating = null, // Not available from current APIs
            collectionId = null, // Not available from basic TMDB data
            collectionName = null // Not available from basic TMDB data
        )
    }
    
    /**
     * Maps a TraktMovieResponse to a Room Movie entity (Trakt data only)
     */
    fun mapTraktToMovie(
        traktResponse: TraktMovieResponse,
        category: String
    ): Movie {
        val traktMovie = traktResponse.movie
        return Movie(
            id = traktMovie.ids.tmdb ?: traktMovie.ids.trakt,
            title = traktMovie.title,
            overview = traktMovie.overview,
            releaseDate = traktMovie.year?.toString(),
            posterPath = null,
            backdropPath = null,
            voteAverage = traktMovie.rating ?: 0.0,
            voteCount = traktMovie.votes ?: 0,
            genreIds = emptyList(),
            originalLanguage = traktMovie.language ?: "en",
            originalTitle = traktMovie.title,
            popularity = 0.0,
            video = false,
            adult = false,
            traktId = traktMovie.ids.trakt,
            traktSlug = traktMovie.ids.slug,
            runtime = null,
            certification = null,
            rottenTomatoesRating = null,
            collectionId = null,
            collectionName = null
        )
    }
    
    /**
     * Maps a TraktMovie (direct) to a Room Movie entity (Trakt data only)
     */
    fun mapTraktToMovie(
        traktMovie: TraktMovie,
        category: String
    ): Movie {
        return Movie(
            id = traktMovie.ids.tmdb ?: traktMovie.ids.trakt,
            title = traktMovie.title,
            overview = traktMovie.overview,
            releaseDate = traktMovie.year?.toString(),
            posterPath = null,
            backdropPath = null,
            voteAverage = traktMovie.rating ?: 0.0,
            voteCount = traktMovie.votes ?: 0,
            genreIds = emptyList(),
            originalLanguage = traktMovie.language ?: "en",
            originalTitle = traktMovie.title,
            popularity = 0.0,
            video = false,
            adult = false,
            traktId = traktMovie.ids.trakt,
            traktSlug = traktMovie.ids.slug,
            runtime = null,
            certification = null,
            rottenTomatoesRating = null,
            collectionId = null,
            collectionName = null
        )
    }
}