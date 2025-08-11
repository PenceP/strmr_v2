package com.strmr.ai.ui.playback.overlay.action

import android.content.Context
import androidx.leanback.widget.PlaybackControlsRow
import com.strmr.ai.ui.playback.overlay.VideoPlayerAdapter

class SkipPreviousAction(context: Context) : PlaybackControlsRow.SkipPreviousAction(context),
	AndroidAction {
	override fun onActionClicked(videoPlayerAdapter: VideoPlayerAdapter) =
		videoPlayerAdapter.previous()
}
