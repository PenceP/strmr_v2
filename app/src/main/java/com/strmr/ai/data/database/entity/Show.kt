package com.strmr.ai.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "shows")
data class Show(
    @PrimaryKey val id: Int,
    val name: String,
    val overview: String? = null,
    val firstAirDate: String? = null,
    val lastAirDate: String? = null,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val voteAverage: Double = 0.0,
    val voteCount: Int = 0,
    val genreIds: List<Int> = emptyList(),
    val originalLanguage: String = "en",
    val originalName: String,
    val popularity: Double = 0.0,
    val adult: Boolean = false,
    val originCountry: List<String> = emptyList(),
    val status: String? = null,
    val numberOfEpisodes: Int? = null,
    val numberOfSeasons: Int? = null,
    val episodeRunTime: List<Int> = emptyList(),
    val networks: List<String> = emptyList(),
    
    // Trakt-specific fields
    val traktId: Int? = null,
    val traktSlug: String? = null,
    
    // Additional metadata fields (similar to Movie)
    val contentRating: String? = null, // TV-MA, TV-14, etc.
    val rottenTomatoesRating: Int? = null, // Rotten Tomatoes percentage (0-100)
    
    // Cache management fields
    val mediaDataCachedAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis()
)