package com.strmr.ai.constant

sealed interface CustomMessage {
	data object RefreshCurrentItem : CustomMessage
	data object ActionComplete : CustomMessage
}
