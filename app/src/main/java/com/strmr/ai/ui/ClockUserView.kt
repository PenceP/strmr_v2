package com.strmr.ai.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.strmr.ai.R
import com.strmr.ai.auth.repository.UserRepository
import com.strmr.ai.databinding.ClockUserBugBinding
import com.strmr.ai.preference.UserPreferences
import com.strmr.ai.preference.constant.ClockBehavior
import com.strmr.ai.ui.navigation.Destinations
import com.strmr.ai.ui.navigation.NavigationRepository
import com.strmr.ai.util.ImageHelper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ClockUserView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0,
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes), KoinComponent {
	private val binding: ClockUserBugBinding = ClockUserBugBinding.inflate(LayoutInflater.from(context), this, true)
	private val userPreferences by inject<UserPreferences>()
	private val userRepository by inject<UserRepository>()
	private val navigationRepository by inject<NavigationRepository>()
	private val imageHelper by inject<ImageHelper>()

	var isVideoPlayer = false
		set(value) {
			field = value
			updateClockVisibility()
		}

	val homeButton get() = binding.home

	init {
		updateClockVisibility()

		val currentUser = userRepository.currentUser.value

		binding.clockUserImage.load(
			url = currentUser?.let(imageHelper::getPrimaryImageUrl),
			placeholder = ContextCompat.getDrawable(context, R.drawable.ic_user)
		)

		binding.clockUserImage.isVisible = currentUser != null

		binding.home.setOnClickListener {
			navigationRepository.reset(Destinations.home, clearHistory = true)
		}
	}

	private fun updateClockVisibility() {
		val showClock = userPreferences[UserPreferences.clockBehavior]

		binding.clock.isVisible = when (showClock) {
			ClockBehavior.ALWAYS -> true
			ClockBehavior.NEVER -> false
			ClockBehavior.IN_VIDEO -> isVideoPlayer
			ClockBehavior.IN_MENUS -> !isVideoPlayer
		}

		binding.home.isVisible = !isVideoPlayer
	}
}
