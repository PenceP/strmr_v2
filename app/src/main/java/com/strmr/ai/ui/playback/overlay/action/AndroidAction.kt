package com.strmr.ai.ui.playback.overlay.action

import com.strmr.ai.ui.playback.overlay.VideoPlayerAdapter

interface AndroidAction {
	fun onActionClicked(
		videoPlayerAdapter: VideoPlayerAdapter
	)
}
