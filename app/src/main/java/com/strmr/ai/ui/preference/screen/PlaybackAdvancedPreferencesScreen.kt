package com.strmr.ai.ui.preference.screen

import android.os.Build
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.strmr.ai.R
import com.strmr.ai.constant.getQualityProfiles
import com.strmr.ai.preference.UserPreferences
import com.strmr.ai.preference.constant.RefreshRateSwitchingBehavior
import com.strmr.ai.preference.constant.ZoomMode
import com.strmr.ai.ui.preference.custom.DurationSeekBarPreference
import com.strmr.ai.ui.preference.dsl.OptionsFragment
import com.strmr.ai.ui.preference.dsl.action
import com.strmr.ai.ui.preference.dsl.checkbox
import com.strmr.ai.ui.preference.dsl.enum
import com.strmr.ai.ui.preference.dsl.list
import com.strmr.ai.ui.preference.dsl.optionsScreen
import com.strmr.ai.ui.preference.dsl.seekbar
import com.strmr.ai.util.TimeUtils
import com.strmr.ai.util.profile.createDeviceProfileReport
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.clientLogApi
import org.koin.android.ext.android.inject
import timber.log.Timber

class PlaybackAdvancedPreferencesScreen : OptionsFragment() {
	private val api: ApiClient by inject()
	private val userPreferences: UserPreferences by inject()
	private var deviceProfileReported = false

	override val screen by optionsScreen {
		setTitle(R.string.pref_playback)

		category {
			setTitle(R.string.pref_customization)

			@Suppress("MagicNumber")
			list {
				setTitle(R.string.lbl_resume_preroll)
				entries = setOf(
					0, // Disable
					3, 5, 7, // 10<
					10, 20, 30, 60, // 100<
					120, 300
				).associate {
					val value = if (it == 0) getString(R.string.lbl_none)
					else TimeUtils.formatSeconds(context, it)

					it.toString() to value
				}
				bind(userPreferences, UserPreferences.resumeSubtractDuration)
			}

			checkbox {
				setTitle(R.string.lbl_tv_queuing)
				bind(userPreferences, UserPreferences.mediaQueuingEnabled)
			}
		}

		category {
			setTitle(R.string.pref_video)

			@Suppress("MagicNumber")
			list {
				setTitle(R.string.pref_max_bitrate_title)
				entries = getQualityProfiles(context)
				bind(userPreferences, UserPreferences.maxBitrate)
			}

			enum<RefreshRateSwitchingBehavior> {
				setTitle(R.string.lbl_refresh_switching)
				bind(userPreferences, UserPreferences.refreshRateSwitchingBehavior)
			}

			@Suppress("MagicNumber")
			seekbar {
				setTitle(R.string.video_start_delay)
				min = 0
				max = 5_000
				increment = 250
				valueFormatter = object : DurationSeekBarPreference.ValueFormatter() {
					override fun display(value: Int): String = "${value.toDouble() / 1000}s"
				}
				bind {
					get { userPreferences[UserPreferences.videoStartDelay].toInt() }
					set { value -> userPreferences[UserPreferences.videoStartDelay] = value.toLong() }
					default { UserPreferences.videoStartDelay.defaultValue.toInt() }
				}
			}

			enum<ZoomMode> {
				setTitle(R.string.default_video_zoom)
				bind(userPreferences, UserPreferences.playerZoomMode)
			}

			checkbox {
				setTitle(R.string.preference_enable_pgs)
				bind(userPreferences, UserPreferences.pgsDirectPlay)
			}

			checkbox {
				setTitle(R.string.pref_external_player)
				bind(userPreferences, UserPreferences.useExternalPlayer)
			}
		}

		category {
			setTitle(R.string.pref_live_tv_cat)

			checkbox {
				setTitle(R.string.lbl_direct_stream_live)
				bind(userPreferences, UserPreferences.liveTvDirectPlayEnabled)
				setContent(R.string.pref_direct_stream_live_on, R.string.pref_direct_stream_live_off)
			}
		}

		category {
			setTitle(R.string.pref_audio)

			checkbox {
				setTitle(R.string.pref_audio_night_mode)
				setContent(R.string.desc_audio_night_mode)
				bind(userPreferences, UserPreferences.audioNightMode)
				depends { Build.VERSION.SDK_INT >= Build.VERSION_CODES.P }
			}

			checkbox {
				setTitle(R.string.lbl_bitstream_ac3)
				setContent(R.string.desc_bitstream_ac3)
				bind(userPreferences, UserPreferences.ac3Enabled)
			}
		}

		category {
			setTitle(R.string.pref_troubleshooting)

			action {
				setTitle(R.string.pref_report_device_profile_title)
				setContent(R.string.pref_report_device_profile_summary)

				depends { !deviceProfileReported }

				onActivate = {
					deviceProfileReported = true

					lifecycleScope.launch {
						runCatching {
							withContext(Dispatchers.IO) {
								api.clientLogApi.logFile(createDeviceProfileReport(context, userPreferences)).content
							}
						}.fold(
							onSuccess = { result ->
								Toast.makeText(
									context,
									getString(R.string.pref_report_device_profile_success, result.fileName),
									Toast.LENGTH_LONG
								).show()
							},
							onFailure = { error ->
								Timber.e(error, "Failed to upload device profile")
								Toast.makeText(context, R.string.pref_report_device_profile_failure, Toast.LENGTH_LONG).show()
								deviceProfileReported = false
							}
						)
					}
				}
			}
		}
	}
}
