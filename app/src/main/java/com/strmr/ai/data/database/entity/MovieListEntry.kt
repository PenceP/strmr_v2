package com.strmr.ai.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Represents a movie's position in a specific list (trending, popular, etc.)
 * This allows us to update list orders without touching the cached movie data
 */
@Entity(
    tableName = "movie_list_entries",
    primaryKeys = ["movieId", "listType"],
    foreignKeys = [
        ForeignKey(
            entity = Movie::class,
            parentColumns = ["id"],
            childColumns = ["movieId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["listType", "position"]),
        Index(value = ["movieId"])
    ]
)
data class MovieListEntry(
    val movieId: Int,
    val listType: String, // "trending", "popular", etc.
    val position: Int, // Order in the list
    val listUpdatedAt: Long = System.currentTimeMillis()
)