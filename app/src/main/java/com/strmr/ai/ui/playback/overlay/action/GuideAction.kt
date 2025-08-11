package com.strmr.ai.ui.playback.overlay.action

import android.content.Context
import android.view.View
import com.strmr.ai.R
import com.strmr.ai.ui.playback.PlaybackController
import com.strmr.ai.ui.playback.overlay.CustomPlaybackTransportControlGlue
import com.strmr.ai.ui.playback.overlay.LeanbackOverlayFragment
import com.strmr.ai.ui.playback.overlay.VideoPlayerAdapter

class GuideAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue
) : CustomAction(context, customPlaybackTransportControlGlue) {
	init {
		initializeWithIcon(R.drawable.ic_guide)
	}

	@Override
	override fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {
		videoPlayerAdapter.leanbackOverlayFragment.hideOverlay()
		videoPlayerAdapter.masterOverlayFragment.showGuide()
	}
}
