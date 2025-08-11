package com.strmr.ai.ui.playback.overlay.action

import android.content.Context
import android.view.View
import com.strmr.ai.R
import com.strmr.ai.ui.livetv.TvManager
import com.strmr.ai.ui.playback.PlaybackController
import com.strmr.ai.ui.playback.overlay.CustomPlaybackTransportControlGlue
import com.strmr.ai.ui.playback.overlay.LeanbackOverlayFragment
import com.strmr.ai.ui.playback.overlay.VideoPlayerAdapter as VideoPlayerAdapter

class PreviousLiveTvChannelAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
) : CustomAction(context, customPlaybackTransportControlGlue) {
	init {
		initializeWithIcon(R.drawable.ic_previous_episode)
	}

	@Override
	override fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {
		videoPlayerAdapter.masterOverlayFragment.switchChannel(TvManager.getPrevLiveTvChannel())
	}
}
