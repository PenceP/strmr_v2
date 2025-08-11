package com.strmr.ai.data.model

data class AppNotification(
	val message: String,
	val dismiss: () -> Unit,
	val public: Boolean,
)
