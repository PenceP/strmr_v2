package com.strmr.ai.ui

fun interface ValueChangedListener<T> {
	fun onValueChanged(value: T)
}
