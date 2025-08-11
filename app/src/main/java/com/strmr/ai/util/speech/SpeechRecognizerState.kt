package com.strmr.ai.util.speech

import androidx.compose.runtime.Stable

@Stable
class SpeechRecognizerState(
	val status: SpeechRecognizerStatus,
	val startListening: () -> Unit,
	val stopListening: () -> Unit,
)
