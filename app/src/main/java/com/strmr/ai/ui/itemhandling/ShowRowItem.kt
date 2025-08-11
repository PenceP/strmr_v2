package com.strmr.ai.ui.itemhandling

import android.content.Context
import com.strmr.ai.constant.ImageType
import com.strmr.ai.data.database.entity.Show
import com.strmr.ai.util.ImageHelper
import java.util.UUID

class ShowRowItem(
    val show: Show,
    staticHeight: Boolean = false,
    preferParentThumb: Boolean = false,
    selectAction: BaseRowItemSelectAction = BaseRowItemSelectAction.ShowDetails
) : BaseRowItem(
    baseRowType = BaseRowType.Show,
    staticHeight = staticHeight,
    preferParentThumb = preferParentThumb,
    selectAction = selectAction,
    baseItem = null // Don't pass Show as BaseItemDto
) {
    override val showCardInfoOverlay: Boolean = false
    
    override val itemId: UUID? get() = UUID.fromString("${show.id}-1111-1111-1111-111111111111")
    
    override val isFavorite: Boolean = false
    override val isPlayed: Boolean = false
    
    override fun getCardName(context: Context): String? = show.name
    
    override fun getFullName(context: Context): String? = show.name
    
    override fun getName(context: Context): String? = show.name
    
    override fun getSummary(context: Context): String? = show.overview
    
    override fun getSubText(context: Context): String? {
        val year = show.firstAirDate?.substring(0, 4)
        val rating = if (show.voteAverage > 0) "%.1f".format(show.voteAverage) else null
        val seasons = show.numberOfSeasons?.let { 
            if (it == 1) "$it season" else "$it seasons"
        }
        
        return listOfNotNull(year, rating?.let { "★$it" }, seasons).joinToString(" • ")
    }
    
    override fun getImageUrl(
        context: Context,
        imageHelper: ImageHelper,
        imageType: ImageType,
        fillWidth: Int,
        fillHeight: Int
    ): String? {
        return when (imageType) {
            ImageType.BANNER -> show.backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" }
            ImageType.THUMB -> show.backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" }
            else -> show.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
        }
    }
    
    override fun getBadgeImage(
        context: Context,
        imageHelper: ImageHelper
    ) = null // Could add TMDB or RT ratings badge here
}