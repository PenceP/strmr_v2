# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Strmr is a Kotlin-based Android TV streaming client. The project follows a multi-module architecture with clean separation between UI, playback, and preferences functionality.

## Common Development Commands

### Building
```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew installDebug           # Install debug build to connected device
./gradlew clean                  # Clean build artifacts
```

### Testing
```bash
./gradlew test                   # Run all unit tests
./gradlew testDebugUnitTest     # Run debug unit tests only
./gradlew :app:testDebugUnitTest # Run tests for app module only
```

### Code Quality
```bash
./gradlew detekt                 # Run Detekt static analysis
./gradlew lintDebug             # Run Android lint on debug build
```

### SDK Version Variants
The project supports multiple SDK versions via gradle properties:
```bash
./gradlew assembleDebug -Psdk.version=local     # Use local SNAPSHOT
./gradlew assembleDebug -Psdk.version=snapshot  # Use master-SNAPSHOT
./gradlew assembleDebug -Psdk.version=unstable  # Use openapi-unstable-SNAPSHOT
```

## Architecture Overview

### Module Structure
- **app/**: Main application module with UI, activities, and dependency injection
- **playback/core/**: Abstract playback interfaces and contracts
- **playback/jellyfin/**: Server-specific playback implementation
- **playback/media3/exoplayer/**: ExoPlayer backend integration
- **playback/media3/session/**: Media3 MediaSession integration
- **preference/**: Shared preferences abstraction layer

### Key Architectural Patterns

**Dependency Injection**: Uses Koin framework with modules organized by feature:
- `androidModule`: Android-specific components
- `appModule`: Core services, SDK, image loading (Coil)
- `authModule`: Authentication and server management
- `playbackModule`: Media playback services
- `preferenceModule`: Settings management
- `utilsModule`: Utility classes

**Repository Pattern**: Data access through interfaces like `UserRepository`, `ServerRepository`, `ItemRepository`, `NavigationRepository`, `NotificationsRepository`

**MVVM**: ViewModels handle business logic for major features (`StartupViewModel`, `PlaybackControllerContainer`, `SearchViewModel`, etc.)

### Build Configuration
- **Target API**: Android 36 (API 36) with minimum API 21
- **Java**: Targets Java 8 bytecode, uses JDK 21 toolchain
- **Kotlin**: Version 2.2.0 with Compose compiler plugin
- **Build Variants**: Debug uses separate application ID suffix `.debug` for parallel installation

### Testing Setup
- **Framework**: Kotest with JUnit 5 platform (`useJUnitPlatform()`)
- **Mocking**: MockK for Kotlin-friendly mocking
- **Configuration**: Full stack traces on test failures

### Code Quality Tools
- **Detekt**: Static analysis with custom rules allowing 15 functions per class, 140 char line length
- **Android Lint**: Custom configuration with SARIF reporting enabled
- **Line Ending**: Uses Unix line endings (LF)

## Important Technical Details

### Version Management
Custom semantic versioning in `buildSrc/` handles:
- Environment variable resolution (`STRMR_VERSION`)
- Pre-release encoding (e.g., "2.0.0-rc.3" becomes version code 2000003)
- Development builds have version codes < 100

### Playback Architecture
Multi-backend system with plugin architecture:
- Core interfaces define playback contracts
- Server module implements streaming-specific logic
- Media3/ExoPlayer provides actual media playback
- Session module handles MediaSession integration for Android Auto/TV

### UI Framework
- **Primary**: AndroidX Leanback for 10-foot TV experience
- **Modern**: Jetpack Compose enabled for new UI components
- **Image Loading**: Coil 3.x for efficient image caching and loading
- **Navigation**: Custom navigation repository pattern

### Key Dependencies
- **Streaming SDK**: `org.jellyfin.sdk:jellyfin-core` (1.7.0-beta.3)
- **Media**: AndroidX Media3, custom FFmpeg with libass subtitle support
- **Async**: Kotlin Coroutines (1.10.2)
- **Serialization**: kotlinx-serialization for JSON handling
- **Crash Reporting**: ACRA for error reporting
- **Logging**: Timber with SLF4J bridge

## Development Notes

### Module Dependencies
The dependency graph flows: `app` → `playback/*` + `preference` → `playback/core`
Each playback module is self-contained and can be swapped independently.

### Build Performance
- Configuration cache is recommended for faster builds
- Parallel execution enabled in Detekt
- Build uses type-safe project accessors

### Internationalization
Supports 48 languages via Weblate translation management. Translation updates are automated via CI/CD.