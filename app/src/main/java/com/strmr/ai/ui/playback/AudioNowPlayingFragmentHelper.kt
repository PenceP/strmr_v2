package com.strmr.ai.ui.playback

import android.widget.ImageView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strmr.ai.R
import com.strmr.ai.ui.base.Icon
import com.strmr.ai.ui.composable.AsyncImage
import com.strmr.ai.ui.composable.LyricsDtoBox
import com.strmr.ai.ui.composable.modifier.fadingEdges
import com.strmr.ai.ui.composable.rememberPlayerProgress
import com.strmr.ai.ui.composable.rememberQueueEntry
import com.strmr.ai.util.apiclient.albumPrimaryImage
import com.strmr.ai.util.apiclient.getUrl
import com.strmr.ai.util.apiclient.itemImages
import com.strmr.ai.util.apiclient.parentImages
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.jellyfin.lyrics
import org.jellyfin.playback.jellyfin.lyricsFlow
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.playback.jellyfin.queue.baseItemFlow
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.ImageType
import org.koin.compose.koinInject

fun initializePreviewView(
	lyricsView: ComposeView,
	playbackManager: PlaybackManager,
) {
	lyricsView.setContent {
		val api = koinInject<ApiClient>()
		val entry by rememberQueueEntry(playbackManager)
		val baseItem = entry?.run { baseItemFlow.collectAsState(baseItem).value }
		val lyrics = entry?.run { lyricsFlow.collectAsState(lyrics) }?.value
		val cover = baseItem?.itemImages[ImageType.PRIMARY] ?: baseItem?.albumPrimaryImage ?: baseItem?.parentImages[ImageType.PRIMARY]

		// Show track/album art when available and fade it out when lyrics are displayed on top
		val coverViewAlpha by animateFloatAsState(
			label = "coverViewAlpha",
			targetValue = if (lyrics == null) 1f else 0.2f,
		)

		AnimatedContent(cover) { cover ->
			if (cover != null) {
				Box(
					modifier = Modifier
						.wrapContentSize()
						.clip(RoundedCornerShape(4.dp))
						.background(Color.Black)
				) {
					AsyncImage(
						url = cover.getUrl(api),
						blurHash = cover.blurHash,
						aspectRatio = cover.aspectRatio?.toFloat() ?: 1f,
						scaleType = ImageView.ScaleType.CENTER_INSIDE,
						modifier = Modifier
							.alpha(coverViewAlpha)
					)
				}
			} else if (lyrics == null) {
				// "placeholder" image
				Icon(ImageVector.vectorResource(R.drawable.ic_album), contentDescription = null, tint = Color.White.copy(alpha = 0.4f))
			}
		}

		// Display lyrics overlay
		if (lyrics != null) {
			val playState by remember { playbackManager.state.playState }.collectAsState()

			// Using the progress animation causes the layout to recompose, which we need for synced lyrics to work
			// we don't actually use the animation value here
			rememberPlayerProgress(playbackManager)

			LyricsDtoBox(
				lyricDto = lyrics,
				currentTimestamp = playbackManager.state.positionInfo.active,
				duration = playbackManager.state.positionInfo.duration,
				paused = playState != PlayState.PLAYING,
				fontSize = 12.sp,
				color = Color.White,
				modifier = Modifier
					.fillMaxSize()
					.fadingEdges(vertical = 50.dp)
					.padding(horizontal = 15.dp),
			)
		}
	}
}
