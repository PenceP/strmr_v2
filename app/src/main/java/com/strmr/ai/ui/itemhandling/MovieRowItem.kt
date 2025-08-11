package com.strmr.ai.ui.itemhandling

import android.content.Context
import com.strmr.ai.constant.ImageType
import com.strmr.ai.data.database.entity.Movie
import com.strmr.ai.util.ImageHelper
import java.util.UUID

class MovieRowItem(
    val movie: Movie,
    staticHeight: Boolean = false,
    preferParentThumb: Boolean = false,
    selectAction: BaseRowItemSelectAction = BaseRowItemSelectAction.ShowDetails
) : BaseRowItem(
    baseRowType = BaseRowType.Movie,
    staticHeight = staticHeight,
    preferParentThumb = preferParentThumb,
    selectAction = selectAction,
    baseItem = null // Don't pass Movie as BaseItemDto
) {
    override val showCardInfoOverlay: Boolean = false
    
    override val itemId: UUID? get() = UUID.fromString("${movie.id}-0000-0000-0000-000000000000")
    
    override val isFavorite: Boolean = false
    override val isPlayed: Boolean = false
    
    override fun getCardName(context: Context): String? = movie.title
    
    override fun getFullName(context: Context): String? = movie.title
    
    override fun getName(context: Context): String? = movie.title
    
    override fun getSummary(context: Context): String? = movie.overview
    
    override fun getSubText(context: Context): String? {
        val year = movie.releaseDate?.substring(0, 4)
        val rating = if (movie.voteAverage > 0) "%.1f".format(movie.voteAverage) else null
        
        return listOfNotNull(year, rating?.let { "★$it" }).joinToString(" • ")
    }
    
    override fun getImageUrl(
        context: Context,
        imageHelper: ImageHelper,
        imageType: ImageType,
        fillWidth: Int,
        fillHeight: Int
    ): String? {
        return when (imageType) {
            ImageType.BANNER -> movie.backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" }
            ImageType.THUMB -> movie.backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" }
            else -> movie.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
        }
    }
    
    override fun getBadgeImage(
        context: Context,
        imageHelper: ImageHelper
    ) = null // Could add TMDB or RT ratings badge here
}