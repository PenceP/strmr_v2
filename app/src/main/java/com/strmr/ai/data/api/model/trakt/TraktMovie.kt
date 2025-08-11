package com.strmr.ai.data.api.model.trakt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TraktMovie(
    @SerialName("title")
    val title: String,
    @SerialName("year")
    val year: Int?,
    @SerialName("ids")
    val ids: TraktIds,
    @SerialName("overview")
    val overview: String? = null,
    @SerialName("rating")
    val rating: Double? = null,
    @SerialName("votes")
    val votes: Int? = null,
    @SerialName("comment_count")
    val commentCount: Int? = null,
    @SerialName("first_aired")
    val firstAired: String? = null,
    @SerialName("runtime")
    val runtime: Int? = null,
    @SerialName("certification")
    val certification: String? = null,
    @SerialName("trailer")
    val trailer: String? = null,
    @SerialName("homepage")
    val homepage: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("language")
    val language: String? = null,
    @SerialName("available_translations")
    val availableTranslations: List<String>? = null,
    @SerialName("genres")
    val genres: List<String>? = null,
    @SerialName("aired_episodes")
    val airedEpisodes: Int? = null
)

@Serializable
data class TraktIds(
    @SerialName("trakt")
    val trakt: Int,
    @SerialName("slug")
    val slug: String,
    @SerialName("imdb")
    val imdb: String? = null,
    @SerialName("tmdb")
    val tmdb: Int? = null
)

@Serializable
data class TraktMovieResponse(
    @SerialName("watchers")
    val watchers: Int? = null,
    @SerialName("plays")
    val plays: Int? = null,
    @SerialName("collected")
    val collected: Int? = null,
    @SerialName("movie")
    val movie: TraktMovie
)