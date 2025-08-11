plugins {
	id("com.android.application")
	kotlin("android")
	kotlin("kapt")  // For Room annotation processor
	alias(libs.plugins.kotlin.serialization)
	alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.aboutlibraries)
}

import java.util.Properties

// Load secrets.properties
val secretsProperties = Properties().apply {
    val secretsFile = rootProject.file("secrets.properties")
    if (secretsFile.exists()) {
        load(secretsFile.inputStream())
    }
}

android {
	namespace = "com.strmr.ai"
	compileSdk = libs.versions.android.compileSdk.get().toInt()

	defaultConfig {
		minSdk = libs.versions.android.minSdk.get().toInt()
		targetSdk = libs.versions.android.targetSdk.get().toInt()

		// Release version
		applicationId = namespace
		versionName = project.getVersionName()
		versionCode = getVersionCode(versionName!!)

		// Add BuildConfig fields for API keys
		buildConfigField("String", "TRAKT_CLIENT_ID", '"' + secretsProperties.getProperty("TRAKT_CLIENT_ID", "") + '"')
		buildConfigField("String", "TRAKT_CLIENT_SECRET", '"' + secretsProperties.getProperty("TRAKT_CLIENT_SECRET", "") + '"')
		buildConfigField("String", "TMDB_API_KEY", '"' + secretsProperties.getProperty("TMDB_API_KEY", "") + '"')
		buildConfigField("String", "TMDB_ACCESS_TOKEN", '"' + secretsProperties.getProperty("TMDB_ACCESS_TOKEN", "") + '"')
	}

	buildFeatures {
		buildConfig = true
		viewBinding = true
		compose = true
	}

	compileOptions {
		isCoreLibraryDesugaringEnabled = true
	}

	buildTypes {
		release {
			isMinifyEnabled = false

			// Set package names used in various XML files
			resValue("string", "app_id", namespace!!)
			resValue("string", "app_search_suggest_authority", "${namespace}.content")
			resValue("string", "app_search_suggest_intent_data", "content://${namespace}.content/intent")

			// Set flavored application name
			resValue("string", "app_name", "@string/app_name_release")

			buildConfigField("boolean", "DEVELOPMENT", "false")
        }

		debug {
			// Set package names used in various XML files
			resValue("string", "app_id", namespace!!)
			resValue("string", "app_search_suggest_authority", "${namespace}.content")
			resValue("string", "app_search_suggest_intent_data", "content://${namespace}.content/intent")

			// Set flavored application name
			resValue("string", "app_name", "@string/app_name_debug")

			buildConfigField("boolean", "DEVELOPMENT", (defaultConfig.versionCode!! < 100).toString())
		}
	}

	lint {
		lintConfig = file("$rootDir/android-lint.xml")
		abortOnError = false
		sarifReport = true
		checkDependencies = true
	}

	testOptions.unitTests.all {
		it.useJUnitPlatform()
	}
}

base.archivesName.set("jellyfin-androidtv-v${project.getVersionName()}")

tasks.register("versionTxt") {
	val path = layout.buildDirectory.asFile.get().resolve("version.txt")

	doLast {
		val versionString = "v${android.defaultConfig.versionName}=${android.defaultConfig.versionCode}"
		logger.info("Writing [$versionString] to $path")
		path.writeText("$versionString\n")
	}
}

dependencies {
	// Jellyfin
	implementation(projects.playback.core)
	implementation(projects.playback.jellyfin)
	implementation(projects.playback.media3.exoplayer)
	implementation(projects.playback.media3.session)
	implementation(projects.preference)
	implementation(libs.jellyfin.sdk) {
		// Change version if desired
		val sdkVersion = findProperty("sdk.version")?.toString()
		when (sdkVersion) {
			"local" -> version { strictly("latest-SNAPSHOT") }
			"snapshot" -> version { strictly("master-SNAPSHOT") }
			"unstable-snapshot" -> version { strictly("openapi-unstable-SNAPSHOT") }
		}
	}

	// Kotlin
	implementation(libs.kotlinx.coroutines)
	implementation(libs.kotlinx.serialization.json)

	// Android(x)
	implementation(libs.androidx.core)
	implementation(libs.androidx.activity)
	implementation(libs.androidx.activity.compose)
	implementation(libs.androidx.fragment)
	implementation(libs.androidx.fragment.compose)
	implementation(libs.androidx.leanback.core)
	implementation(libs.androidx.leanback.preference)
	implementation(libs.androidx.preference)
	implementation(libs.androidx.appcompat)
	implementation(libs.androidx.tvprovider)
	implementation(libs.androidx.constraintlayout)
	implementation(libs.androidx.recyclerview)
	implementation(libs.androidx.work.runtime)
	implementation(libs.bundles.androidx.lifecycle)
	implementation(libs.androidx.window)
	implementation(libs.androidx.cardview)
	implementation(libs.androidx.startup)
	implementation(libs.bundles.androidx.compose)
	implementation(libs.accompanist.permissions)

	// Dependency Injection
	implementation(libs.bundles.koin)

	// Media players
	implementation(libs.androidx.media3.exoplayer)
	implementation(libs.androidx.media3.datasource.okhttp)
	implementation(libs.androidx.media3.exoplayer.hls)
	implementation(libs.androidx.media3.ui)
	implementation(libs.jellyfin.androidx.media3.ffmpeg.decoder)
	implementation(libs.libass.media3)

	// Markdown
	implementation(libs.bundles.markwon)

	// Image utility
	implementation(libs.bundles.coil)

	// Crash Reporting
	implementation(libs.bundles.acra)

	// Licenses
	implementation(libs.aboutlibraries)

	// Logging
	implementation(libs.timber)
	implementation(libs.slf4j.timber)

	// Compatibility (desugaring)
	coreLibraryDesugaring(libs.android.desugar)

	// New dependencies for Trakt/TMDB migration
	// Room Database
	implementation(libs.androidx.room.runtime)
	implementation(libs.androidx.room.ktx)
	implementation(libs.androidx.room.paging)
	kapt(libs.androidx.room.compiler)

	// Networking
	implementation(libs.retrofit)
	implementation(libs.retrofit.converter.kotlinx.serialization)
	implementation(libs.okhttp.logging.interceptor)

	// Pagination
	implementation(libs.androidx.paging.runtime)
	implementation(libs.androidx.paging.compose)

	// Testing
	testImplementation(libs.kotest.runner.junit5)
	testImplementation(libs.kotest.assertions)
	testImplementation(libs.mockk)
	testImplementation(libs.kotlinx.coroutines.test)
	testImplementation(libs.androidx.paging.testing)
	testImplementation(libs.squareup.okhttp3.mockwebserver)

	// Integration/Android Testing
	androidTestImplementation(libs.androidx.test.ext.junit)
	androidTestImplementation(libs.androidx.test.espresso.core)
	androidTestImplementation(libs.androidx.room.testing)
	androidTestImplementation(libs.androidx.test.runner)
	androidTestImplementation(libs.androidx.test.rules)
	androidTestImplementation(libs.mockk.android)
	androidTestImplementation(libs.koin.test)
}
