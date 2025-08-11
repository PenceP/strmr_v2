package com.strmr.ai.ui.playback.overlay.action

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import com.strmr.ai.R
import com.strmr.ai.constant.getQualityProfiles
import com.strmr.ai.preference.UserPreferences
import com.strmr.ai.ui.playback.PlaybackController
import com.strmr.ai.ui.playback.VideoQualityController
import com.strmr.ai.ui.playback.overlay.CustomPlaybackTransportControlGlue
import com.strmr.ai.ui.playback.overlay.VideoPlayerAdapter

class SelectQualityAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
	userPreferences: UserPreferences
) : CustomAction(context, customPlaybackTransportControlGlue) {
	private val previousQualitySelection = userPreferences[UserPreferences.maxBitrate]
	private val qualityController = VideoQualityController(previousQualitySelection, userPreferences)
	private val qualityProfiles = getQualityProfiles(context)
	private var popup: PopupMenu? = null

	init {
		initializeWithIcon(R.drawable.ic_select_quality)
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {
		videoPlayerAdapter.leanbackOverlayFragment.setFading(false)
		dismissPopup()
		popup = PopupMenu(context, view, Gravity.END).apply {
			qualityProfiles.values.forEachIndexed { i, selected ->
				menu.add(0, i, i, selected)
			}

			menu.setGroupCheckable(0, true, true)
			val currentQualityIndex = qualityProfiles.keys.indexOf(qualityController.currentQuality)
			if (currentQualityIndex != -1) {
				menu.getItem(currentQualityIndex)?.let { item ->
					item.isChecked = true
				}
			}

			setOnDismissListener {
				videoPlayerAdapter.leanbackOverlayFragment.setFading(true)
				popup = null
			}
			setOnMenuItemClickListener { menuItem ->
				qualityController.currentQuality = qualityProfiles.keys.elementAt(menuItem.itemId)
				playbackController.refreshStream()
				dismiss()
				true
			}
		}
		popup?.show()
	}

	fun dismissPopup() {
		popup?.dismiss()
	}
}
