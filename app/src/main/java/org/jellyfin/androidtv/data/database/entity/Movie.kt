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
    val runtime: Int?, // Runtime in minutes
    val certification: String?, // Rating like "PG", "R", "PG-13"
    val rottenTomatoesRating: Int?, // Rotten Tomatoes percentage (0-100)
    val mediaDataCachedAt: Long = System.currentTimeMillis(), // When movie details were fetched
    val lastAccessedAt: Long = System.currentTimeMillis() // For LRU cache cleanup if needed
)