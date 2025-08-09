package org.jellyfin.androidtv.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "show_list_entries",
    primaryKeys = ["showId", "listType"],
    foreignKeys = [
        ForeignKey(
            entity = Show::class,
            parentColumns = ["id"],
            childColumns = ["showId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["listType", "position"]),
        Index(value = ["showId"])
    ]
)
data class ShowListEntry(
    val showId: Int,
    val listType: String, // "trending", "popular", etc.
    val position: Int,
    val listUpdatedAt: Long
)