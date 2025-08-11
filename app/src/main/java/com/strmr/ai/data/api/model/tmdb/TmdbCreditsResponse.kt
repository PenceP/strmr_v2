package com.strmr.ai.data.api.model.tmdb

import kotlinx.serialization.Serializable

@Serializable
data class TmdbCreditsResponse(
    val id: Int,
    val cast: List<TmdbCastMember>,
    val crew: List<TmdbCrewMember>
)

@Serializable
data class TmdbCastMember(
    val adult: Boolean = false,
    val gender: Int? = null,
    val id: Int,
    val known_for_department: String = "",  // Use snake_case to match JSON
    val name: String,
    val original_name: String = "",          // Use snake_case to match JSON
    val popularity: Double = 0.0,
    val profile_path: String? = null,        // Use snake_case to match JSON
    val cast_id: Int? = null,                // Use snake_case to match JSON
    val character: String? = null,
    val credit_id: String,                   // Use snake_case to match JSON
    val order: Int
)

@Serializable
data class TmdbCrewMember(
    val adult: Boolean = false,
    val gender: Int? = null,
    val id: Int,
    val known_for_department: String = "",   // Use snake_case to match JSON
    val name: String,
    val original_name: String = "",          // Use snake_case to match JSON
    val popularity: Double = 0.0,
    val profile_path: String? = null,        // Use snake_case to match JSON
    val credit_id: String,                   // Use snake_case to match JSON
    val department: String,
    val job: String
)