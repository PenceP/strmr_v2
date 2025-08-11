package com.strmr.ai.integration.dream.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.strmr.ai.ui.base.Text
import com.strmr.ai.ui.composable.modifier.overscan
import com.strmr.ai.ui.composable.rememberCurrentTime

@Composable
fun DreamHeader(
	showClock: Boolean,
) = Box(
	modifier = Modifier
		.fillMaxWidth()
		.overscan(),
) {
	// Clock
	AnimatedVisibility(
		visible = showClock,
		enter = fadeIn(),
		exit = fadeOut(),
		modifier = Modifier
			.align(Alignment.TopEnd),
	) {
		val currentTime by rememberCurrentTime()
		Text(
			text = currentTime,
			style = TextStyle(
				color = Color.White,
				fontSize = 20.sp
			),
		)
	}
}
