package com.strmr.ai.data.api.model.tmdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbVideoResponse(
    val id: Int,
    val results: List<TmdbVideo>
)

@Serializable
data class TmdbVideo(
    val id: String,
    @SerialName("iso_639_1")
    val iso639_1: String,
    @SerialName("iso_3166_1")
    val iso3166_1: String,
    val key: String,
    val name: String,
    val site: String,
    val size: Int,
    val type: String,
    val official: Boolean,
    @SerialName("published_at")
    val publishedAt: String
) {
    /**
     * Check if this video is a YouTube trailer
     */
    val isYouTubeTrailer: Boolean
        get() = site.equals("YouTube", ignoreCase = true) && 
                type.equals("Trailer", ignoreCase = true)
    
    /**
     * Get the YouTube URL for this video
     */
    val youTubeUrl: String?
        get() = if (site.equals("YouTube", ignoreCase = true)) {
            "https://www.youtube.com/watch?v=$key"
        } else null
    
    /**
     * Get the YouTube embed URL for this video
     */
    val youTubeEmbedUrl: String?
        get() = if (site.equals("YouTube", ignoreCase = true)) {
            "https://www.youtube.com/embed/$key"
        } else null
}