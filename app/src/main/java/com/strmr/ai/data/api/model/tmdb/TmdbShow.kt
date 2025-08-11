package com.strmr.ai.data.api.model.tmdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbShow(
    val id: Int,
    val name: String,
    val overview: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("last_air_date") val lastAirDate: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
    @SerialName("original_language") val originalLanguage: String = "en",
    @SerialName("original_name") val originalName: String,
    val popularity: Double = 0.0,
    val adult: Boolean = false,
    @SerialName("origin_country") val originCountry: List<String> = emptyList()
)

@Serializable
data class TmdbShowDetails(
    val id: Int,
    val name: String,
    val overview: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("last_air_date") val lastAirDate: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    val genres: List<TmdbGenre> = emptyList(),
    @SerialName("original_language") val originalLanguage: String = "en",
    @SerialName("original_name") val originalName: String,
    val popularity: Double = 0.0,
    val adult: Boolean = false,
    @SerialName("origin_country") val originCountry: List<String> = emptyList(),
    val status: String? = null,
    val tagline: String? = null,
    val type: String? = null,
    @SerialName("number_of_episodes") val numberOfEpisodes: Int? = null,
    @SerialName("number_of_seasons") val numberOfSeasons: Int? = null,
    @SerialName("episode_run_time") val episodeRunTime: List<Int> = emptyList(),
    val networks: List<TmdbNetwork> = emptyList(),
    @SerialName("created_by") val createdBy: List<TmdbCreator> = emptyList(),
    @SerialName("content_ratings") val contentRatings: TmdbContentRatingsResponse? = null
)

@Serializable
data class TmdbNetwork(
    val id: Int,
    val name: String,
    @SerialName("logo_path") val logoPath: String? = null,
    @SerialName("origin_country") val originCountry: String = ""
)

@Serializable
data class TmdbCreator(
    val id: Int,
    val name: String,
    @SerialName("profile_path") val profilePath: String? = null
)

@Serializable
data class TmdbShowResponse(
    val page: Int = 1,
    val results: List<TmdbShow> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 1,
    @SerialName("total_results") val totalResults: Int = 0
)

@Serializable
data class TmdbContentRatingsResponse(
    val results: List<TmdbContentRating> = emptyList()
)

@Serializable
data class TmdbContentRating(
    @SerialName("iso_3166_1") val iso31661: String,
    val rating: String
)