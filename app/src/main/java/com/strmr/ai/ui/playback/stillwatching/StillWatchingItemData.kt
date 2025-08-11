package com.strmr.ai.ui.playback.stillwatching

import com.strmr.ai.util.apiclient.JellyfinImage
import org.jellyfin.sdk.model.UUID
import org.jellyfin.sdk.model.api.BaseItemDto

data class StillWatchingItemData(
	val baseItem: BaseItemDto,
	val id: UUID,
	val title: String,
	val thumbnail: JellyfinImage?,
	val logo: JellyfinImage?,
)
