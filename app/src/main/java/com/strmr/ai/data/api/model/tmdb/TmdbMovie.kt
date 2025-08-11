package com.strmr.ai.data.api.model.tmdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbMovie(
    @SerialName("id")
    val id: Int,
    @SerialName("title")
    val title: String,
    @SerialName("original_title")
    val originalTitle: String,
    @SerialName("overview")
    val overview: String?,
    @SerialName("release_date")
    val releaseDate: String?,
    @SerialName("poster_path")
    val posterPath: String?,
    @SerialName("backdrop_path")
    val backdropPath: String?,
    @SerialName("vote_average")
    val voteAverage: Double,
    @SerialName("vote_count")
    val voteCount: Int,
    @SerialName("genre_ids")
    val genreIds: List<Int>,
    @SerialName("original_language")
    val originalLanguage: String,
    @SerialName("popularity")
    val popularity: Double,
    @SerialName("video")
    val video: Boolean,
    @SerialName("adult")
    val adult: Boolean
)

@Serializable
data class TmdbMovieResponse(
    @SerialName("page")
    val page: Int,
    @SerialName("results")
    val results: List<TmdbMovie>,
    @SerialName("total_pages")
    val totalPages: Int,
    @SerialName("total_results")
    val totalResults: Int
)

@Serializable
data class TmdbMovieDetails(
    @SerialName("id")
    val id: Int,
    @SerialName("title")
    val title: String,
    @SerialName("original_title")
    val originalTitle: String,
    @SerialName("overview")
    val overview: String?,
    @SerialName("release_date")
    val releaseDate: String?,
    @SerialName("poster_path")
    val posterPath: String?,
    @SerialName("backdrop_path")
    val backdropPath: String?,
    @SerialName("vote_average")
    val voteAverage: Double,
    @SerialName("vote_count")
    val voteCount: Int,
    @SerialName("genres")
    val genres: List<TmdbGenre>,
    @SerialName("original_language")
    val originalLanguage: String,
    @SerialName("popularity")
    val popularity: Double,
    @SerialName("video")
    val video: Boolean,
    @SerialName("adult")
    val adult: Boolean,
    @SerialName("runtime")
    val runtime: Int?,
    @SerialName("belongs_to_collection")
    val belongsToCollection: TmdbCollection?
)

@Serializable
data class TmdbGenre(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String
)

@Serializable
data class TmdbConfiguration(
    @SerialName("images")
    val images: TmdbImageConfiguration,
    @SerialName("change_keys")
    val changeKeys: List<String>
)

@Serializable
data class TmdbImageConfiguration(
    @SerialName("base_url")
    val baseUrl: String,
    @SerialName("secure_base_url")
    val secureBaseUrl: String,
    @SerialName("backdrop_sizes")
    val backdropSizes: List<String>,
    @SerialName("logo_sizes")
    val logoSizes: List<String>,
    @SerialName("poster_sizes")
    val posterSizes: List<String>,
    @SerialName("profile_sizes")
    val profileSizes: List<String>,
    @SerialName("still_sizes")
    val stillSizes: List<String>
)

@Serializable
data class TmdbMovieReleasesResponse(
    @SerialName("id")
    val id: Int,
    @SerialName("results")
    val results: List<TmdbCountryRelease>
)

@Serializable
data class TmdbCountryRelease(
    @SerialName("iso_3166_1")
    val countryCode: String,
    @SerialName("release_dates")
    val releaseDates: List<TmdbReleaseDate>
)

@Serializable
data class TmdbReleaseDate(
    @SerialName("certification")
    val certification: String,
    @SerialName("descriptors")
    val descriptors: List<String> = emptyList(),
    @SerialName("iso_639_1")
    val languageCode: String = "",
    @SerialName("note")
    val note: String = "",
    @SerialName("release_date")
    val releaseDate: String = "",
    @SerialName("type")
    val type: Int
)

@Serializable
data class TmdbCollection(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
    @SerialName("poster_path")
    val posterPath: String?,
    @SerialName("backdrop_path")
    val backdropPath: String?
)

@Serializable
data class TmdbCollectionDetails(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
    @SerialName("overview")
    val overview: String?,
    @SerialName("poster_path")
    val posterPath: String?,
    @SerialName("backdrop_path")
    val backdropPath: String?,
    @SerialName("parts")
    val parts: List<TmdbMovie>
)