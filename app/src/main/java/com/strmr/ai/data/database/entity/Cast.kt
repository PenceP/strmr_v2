package com.strmr.ai.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "cast_members",
    foreignKeys = [
        ForeignKey(
            entity = Movie::class,
            parentColumns = ["id"],
            childColumns = ["movieId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("movieId")]
)
@Serializable
data class CastMember(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val movieId: Int, // Foreign key to Movie
    val tmdbCreditId: String, // TMDB credit ID
    val personId: Int, // TMDB person ID
    val name: String,
    val character: String?,
    val order: Int, // Display order
    val profilePath: String?, // Path to actor's image
    val department: String = "Acting", // Department (Acting, Directing, etc.)
    val job: String? = null // Job title for crew members
)

@Entity(
    tableName = "show_cast_members",
    foreignKeys = [
        ForeignKey(
            entity = Show::class,
            parentColumns = ["id"],
            childColumns = ["showId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("showId")]
)
@Serializable
data class ShowCastMember(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val showId: Int, // Foreign key to Show
    val tmdbCreditId: String, // TMDB credit ID
    val personId: Int, // TMDB person ID
    val name: String,
    val character: String?,
    val order: Int, // Display order
    val profilePath: String?, // Path to actor's image
    val department: String = "Acting",
    val job: String? = null
)