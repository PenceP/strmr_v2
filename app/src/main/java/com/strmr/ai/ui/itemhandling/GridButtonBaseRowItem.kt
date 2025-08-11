package com.strmr.ai.ui.itemhandling

import android.content.Context
import com.strmr.ai.constant.ImageType
import com.strmr.ai.ui.GridButton
import com.strmr.ai.util.ImageHelper

class GridButtonBaseRowItem(
	val gridButton: GridButton,
) : BaseRowItem(
	baseRowType = BaseRowType.GridButton,
	staticHeight = true,
) {
	override fun getImageUrl(
		context: Context,
		imageHelper: ImageHelper,
		imageType: ImageType,
		fillWidth: Int,
		fillHeight: Int
	) = gridButton.imageRes?.let { imageHelper.getResourceUrl(context, it) }

	override fun getFullName(context: Context) = gridButton.text
	override fun getName(context: Context) = gridButton.text
}
