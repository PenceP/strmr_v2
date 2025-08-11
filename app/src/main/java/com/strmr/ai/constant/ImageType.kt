package com.strmr.ai.constant

import com.strmr.ai.R
import org.jellyfin.preference.PreferenceEnum

enum class ImageType(
	override val nameRes: Int,
) : PreferenceEnum {
	/**
	 * Poster.
	 */
	POSTER(R.string.image_type_poster),

	/**
	 * Thumbnail.
	 */
	THUMB(R.string.image_type_thumbnail),

	/**
	 * Banner.
	 */
	BANNER(R.string.image_type_banner),
}
