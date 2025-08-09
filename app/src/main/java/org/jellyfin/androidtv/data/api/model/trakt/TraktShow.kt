package org.jellyfin.androidtv.data.api.model.trakt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TraktShow(
    val title: String,
    val year: Int?,
    val ids: TraktIds,
    val overview: String? = null,
    val rating: Double? = null,
    val votes: Int? = null,
    val language: String? = null,
    val genres: List<String>? = null,
    val aired_episodes: Int? = null,
    val status: String? = null,
    val network: String? = null,
    val country: String? = null,
    val runtime: Int? = null,
    @SerialName("first_aired") val firstAired: String? = null
)

@Serializable
data class TraktShowResponse(
    val watchers: Int? = null,
    val plays: Int? = null,
    val show: TraktShow
)