package org.jellyfin.androidtv.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "movies")
@Serializable
data class Movie(
    @PrimaryKey
    val id: Int,
    val title: String,
    val overview: String?,
    val releaseDate: String?,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: Double,
    val voteCount: Int,
    val genreIds: List<Int>,
    val originalLanguage: String,
    val originalTitle: String,
    val popularity: Double,
    val video: Boolean,
    val adult: Boolean,
    val traktId: Int?,
    val traktSlug: String?,
    val lastUpdated: Long = System.currentTimeMillis(),
    val category: String // "trending" or "popular"
)