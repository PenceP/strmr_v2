# Trakt/TMDB Migration Plan: Complete App Transformation

## Overview

This document outlines the step-by-step process to completely transform the Jellyfin AndroidTV app into a standalone movie discovery application using Trakt API for content and TMDB API for images. This is a complete replacement project that will eventually become an entirely separate app with all Jellyfin traces removed.

## Project Goals

- **Complete Transformation**: Replace all Jellyfin functionality with Trakt/TMDB APIs
- **Movie Focus**: Start with movies only (Trending, Popular) as proof of concept
- **Navigation Preservation**: Maintain existing UI patterns and navigation flow
- **Details Integration**: Movies navigate to details page (no playback functionality initially)
- **Smart Architecture**: Use industry-standard patterns and proven libraries
- **Clean Organization**: Implement proper directory structure following Android best practices
- **Efficient Caching**: Room database with pagination (page size 20, load trigger at 8 items from end)
- **Secure Configuration**: Store API credentials in `secrets.properties` file

## Architecture Philosophy

This migration follows **industry-standard Android architecture patterns**:

- **Clean Architecture**: Clear separation of data, domain, and presentation layers
- **MVVM + Repository Pattern**: Proven architectural pattern for Android apps
- **Single Responsibility**: Each component has one clear purpose
- **Dependency Injection**: Use Koin for simple, effective DI
- **Industry Libraries**: Leverage battle-tested libraries (Room, Retrofit, Paging3, Coil)
- **No Over-Engineering**: Simple, maintainable solutions over complex abstractions

## API Credentials Setup

### 1. Create secrets.properties File

Create `/secrets.properties` in project root (add to `.gitignore`):

```properties
# Trakt API Credentials
TRAKT_CLIENT_ID=a380a31ce8c76681c825decf7c03e5ef1e6cce3e2657fc9289e0c3594b1a555b
TRAKT_CLIENT_SECRET=d1096df8b09eca9acb9c6bda2593833743b8106ce437b4ae6498a7d0036300ce

# TMDB API Credentials
TMDB_API_KEY=e13565eddebc0d048ba4d5a75dcf251f
TMDB_ACCESS_TOKEN=eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJlMTM1NjVlZGRlYmMwZDA0OGJhNGQ1YTc1ZGNmMjUxZiIsIm5iZiI6MTQ2MzY2ODkwMC45NzcsInN1YiI6IjU3M2RkMGE0YzNhMzY4Mjk5ZTAwMDA0YiIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.mvvESyrWSEEeK6yq-WTitSNaIc4U6nWn8BVohEgfubk
```

### 2. Update build.gradle.kts

Add secrets loading to `/app/build.gradle.kts`:

```kotlin
// Load secrets.properties
val secretsProperties = Properties().apply {
    val secretsFile = rootProject.file("secrets.properties")
    if (secretsFile.exists()) {
        load(secretsFile.inputStream())
    }
}

android {
    defaultConfig {
        // Add BuildConfig fields for API keys
        buildConfigField("String", "TRAKT_CLIENT_ID", "\"${secretsProperties.getProperty("TRAKT_CLIENT_ID", "")}\"")
        buildConfigField("String", "TRAKT_CLIENT_SECRET", "\"${secretsProperties.getProperty("TRAKT_CLIENT_SECRET", "")}\"") 
        buildConfigField("String", "TMDB_API_KEY", "\"${secretsProperties.getProperty("TMDB_API_KEY", "")}\"")
        buildConfigField("String", "TMDB_ACCESS_TOKEN", "\"${secretsProperties.getProperty("TMDB_ACCESS_TOKEN", "")}\"")
    }
}
```

## Phase 1: Foundation Setup

### Step 1.1: Add Dependencies

Update `/gradle/libs.versions.toml`:

```toml
[versions]
room = "2.7.0"
retrofit = "2.11.0"
okhttp = "4.12.0"  
paging = "3.3.7"

[libraries]
# Database
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-paging = { group = "androidx.room", name = "room-paging", version.ref = "room" }

# Networking  
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-converter-gson = { group = "com.squareup.retrofit2", name = "converter-gson", version.ref = "retrofit" }
retrofit-converter-kotlinx-serialization = { group = "com.squareup.retrofit2", name = "converter-kotlinx-serialization", version.ref = "retrofit" }
okhttp-logging-interceptor = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }

# Pagination
androidx-paging-runtime = { group = "androidx.paging", name = "paging-runtime", version.ref = "paging" }
androidx-paging-compose = { group = "androidx.paging", name = "paging-compose", version.ref = "paging" }
```

Update `/app/build.gradle.kts` dependencies:

```kotlin
dependencies {
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
}
```

## Directory Structure & Organization

Following Android's **recommended architecture guidelines** and **Clean Architecture principles**:

```
app/src/main/java/org/jellyfin/androidtv/
├── data/                          # Data Layer
│   ├── database/                  # Room Database
│   │   ├── TraktDatabase.kt
│   │   ├── dao/                   # Data Access Objects
│   │   │   ├── TraktMovieDao.kt
│   │   │   └── TMDBImageDao.kt
│   │   └── entities/              # Database Entities
│   │       ├── TraktMovie.kt
│   │       └── TMDBMovieImages.kt
│   ├── network/                   # API Layer
│   │   ├── api/                   # API Service Interfaces
│   │   │   ├── TraktApiService.kt
│   │   │   └── TMDBApiService.kt
│   │   ├── dto/                   # Data Transfer Objects (API Response Models)
│   │   │   ├── trakt/
│   │   │   │   ├── TraktMovieResponse.kt
│   │   │   │   ├── TraktMovieDetails.kt
│   │   │   │   └── TraktIds.kt
│   │   │   └── tmdb/
│   │   │       ├── TMDBConfiguration.kt
│   │   │       ├── TMDBMovieDetails.kt
│   │   │       └── TMDBMovieImagesResponse.kt
│   │   ├── interceptors/          # Network Interceptors
│   │   │   ├── TraktAuthInterceptor.kt
│   │   │   └── TMDBAuthInterceptor.kt
│   │   └── paging/                # Paging Sources
│   │       └── TraktMoviePagingSource.kt
│   └── repository/                # Repository Implementations
│       ├── TraktRepositoryImpl.kt
│       └── TMDBRepositoryImpl.kt
├── domain/                        # Domain Layer (Business Logic)
│   ├── model/                     # Domain Models (Clean entities)
│   │   └── Movie.kt
│   ├── repository/                # Repository Interfaces
│   │   ├── TraktRepository.kt
│   │   └── TMDBRepository.kt
│   └── usecase/                   # Use Cases (Business Operations)
│       ├── GetTrendingMoviesUseCase.kt
│       ├── GetPopularMoviesUseCase.kt
│       └── GetMovieImagesUseCase.kt
├── presentation/                  # Presentation Layer (UI)
│   ├── home/                      # Home Screen Feature
│   │   ├── HomeFragment.kt
│   │   ├── HomeViewModel.kt
│   │   └── adapter/
│   │       ├── MovieRowAdapter.kt
│   │       └── MovieCardPresenter.kt
│   ├── moviedetails/              # Movie Details Feature
│   │   ├── MovieDetailsFragment.kt
│   │   └── MovieDetailsViewModel.kt
│   └── common/                    # Shared UI Components
│       ├── adapter/
│       └── viewholder/
├── di/                            # Dependency Injection
│   ├── DatabaseModule.kt
│   ├── NetworkModule.kt
│   ├── RepositoryModule.kt
│   └── ViewModelModule.kt
└── util/                          # Utilities
    ├── ImageHelper.kt
    ├── Constants.kt
    └── Extensions.kt
```

This structure provides:
- **Clear Separation**: Data, Domain, Presentation layers are distinct
- **Feature-based Organization**: UI components grouped by feature
- **Scalability**: Easy to add new features without restructuring
- **Testability**: Each layer can be tested independently
- **Industry Standard**: Follows Google's recommended app architecture

### Step 1.2: Create Data Models

Following the directory structure above:

#### TraktMovie.kt
```kotlin
@Entity(tableName = "trakt_movies")
@Serializable
data class TraktMovie(
    @PrimaryKey val id: Int,
    val title: String,
    val year: Int?,
    val overview: String?,
    @SerialName("released") val releaseDate: String?,
    val runtime: Int?,
    val rating: Double?,
    val votes: Int?,
    val genres: List<String>? = null,
    val language: String? = null,
    val certification: String? = null,
    
    // TMDB IDs for image fetching
    val tmdbId: Int?,
    val imdbId: String?,
    
    // Trakt metadata
    val watchers: Int? = null,
    val plays: Int? = null,
    
    // Cache metadata
    val listType: String, // "trending", "popular"
    val position: Int,
    val lastUpdated: Long = System.currentTimeMillis()
)
```

#### TraktIds.kt
```kotlin
@Serializable
data class TraktIds(
    val trakt: Int,
    val slug: String?,
    val tmdb: Int?,
    val imdb: String?,
)
```

#### TMDBMovieImages.kt
```kotlin
@Entity(tableName = "tmdb_movie_images") 
@Serializable
data class TMDBMovieImages(
    @PrimaryKey val tmdbId: Int,
    val posterPath: String?,
    val backdropPath: String?,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Serializable
data class TMDBConfiguration(
    val images: TMDBImageConfiguration
)

@Serializable
data class TMDBImageConfiguration(
    @SerialName("base_url") val baseUrl: String,
    @SerialName("secure_base_url") val secureBaseUrl: String,
    @SerialName("backdrop_sizes") val backdropSizes: List<String>,
    @SerialName("poster_sizes") val posterSizes: List<String>
)
```

### Step 1.3: Create Room Database

Create `/app/src/main/java/org/jellyfin/androidtv/data/database/`:

#### TraktDatabase.kt
```kotlin
@Database(
    entities = [TraktMovie::class, TMDBMovieImages::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TraktDatabase : RoomDatabase() {
    abstract fun movieDao(): TraktMovieDao
    abstract fun imageDao(): TMDBImageDao
}

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }
    
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.map { it.trim() }
    }
}
```

#### TraktMovieDao.kt
```kotlin
@Dao
interface TraktMovieDao {
    @Query("SELECT * FROM trakt_movies WHERE listType = :listType ORDER BY position ASC LIMIT :limit OFFSET :offset")
    suspend fun getMoviesByType(listType: String, limit: Int, offset: Int): List<TraktMovie>
    
    @Query("SELECT * FROM trakt_movies WHERE listType = :listType ORDER BY position ASC")
    fun getMoviesByTypePaged(listType: String): PagingSource<Int, TraktMovie>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<TraktMovie>)
    
    @Query("DELETE FROM trakt_movies WHERE listType = :listType")
    suspend fun clearMoviesByType(listType: String)
    
    @Query("SELECT COUNT(*) FROM trakt_movies WHERE listType = :listType") 
    suspend fun getCountByType(listType: String): Int
    
    @Query("DELETE FROM trakt_movies WHERE lastUpdated < :timestamp")
    suspend fun deleteStaleMovies(timestamp: Long)
}

@Dao
interface TMDBImageDao {
    @Query("SELECT * FROM tmdb_movie_images WHERE tmdbId = :tmdbId")
    suspend fun getMovieImages(tmdbId: Int): TMDBMovieImages?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: TMDBMovieImages)
    
    @Query("DELETE FROM tmdb_movie_images WHERE lastUpdated < :timestamp")
    suspend fun deleteStaleImages(timestamp: Long)
}
```

## Phase 2: API Layer

### Step 2.1: Create API Interfaces

Create `/app/src/main/java/org/jellyfin/androidtv/data/api/`:

#### TraktApiService.kt
```kotlin
interface TraktApiService {
    @GET("movies/trending")
    suspend fun getTrendingMovies(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): List<TraktMovieResponse>
    
    @GET("movies/popular")
    suspend fun getPopularMovies(
        @Query("page") page: Int = 1, 
        @Query("limit") limit: Int = 20
    ): List<TraktMovieResponse>
}

@Serializable
data class TraktMovieResponse(
    val watchers: Int? = null,
    val plays: Int? = null,
    val movie: TraktMovieDetails
)

@Serializable
data class TraktMovieDetails(
    val title: String,
    val year: Int?,
    val ids: TraktIds,
    val overview: String?,
    @SerialName("released") val releaseDate: String?,
    val runtime: Int?,
    val rating: Double?,
    val votes: Int?,
    val genres: List<String>? = null,
    val language: String? = null,
    val certification: String? = null
)
```

#### TMDBApiService.kt
```kotlin
interface TMDBApiService {
    @GET("configuration")
    suspend fun getConfiguration(): TMDBConfiguration
    
    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): TMDBMovieDetails
    
    @GET("movie/{movie_id}/images")
    suspend fun getMovieImages(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): TMDBMovieImagesResponse
}

@Serializable
data class TMDBMovieDetails(
    val id: Int,
    @SerialName("poster_path") val posterPath: String?,
    @SerialName("backdrop_path") val backdropPath: String?
)

@Serializable
data class TMDBMovieImagesResponse(
    val id: Int,
    val backdrops: List<TMDBImage>,
    val posters: List<TMDBImage>
)

@Serializable
data class TMDBImage(
    @SerialName("file_path") val filePath: String,
    val width: Int,
    val height: Int,
    @SerialName("vote_average") val voteAverage: Double
)
```

### Step 2.2: Create API Client Configuration

## Industry-Standard Implementation Approach

### Core Library Choices

**Database**: Room (Google's recommended SQLite wrapper)
- Well-documented, battle-tested
- Built-in coroutine support
- Excellent debugging tools

**Network**: Retrofit + OkHttp (Industry standard)
- Mature, reliable networking stack
- Extensive documentation and community support
- Built-in error handling and interceptors

**Async**: Kotlin Coroutines + Flow (Modern Android standard)
- Google's recommended approach for async operations
- Clean, readable code
- Built-in lifecycle awareness

**Pagination**: Paging 3 Library (Google's official solution)
- Handles complex pagination scenarios automatically
- Built-in loading states and error handling
- Integrates seamlessly with Room and Retrofit

**Dependency Injection**: Koin (Lightweight, Kotlin-first)
- Simple setup compared to Dagger/Hilt
- Clear, readable syntax
- Perfect for medium-sized apps

**Image Loading**: Coil (Modern, Kotlin-first image library)
- Already integrated in the project
- Excellent performance and memory management
- Supports modern image formats

### Key Implementation Principles

1. **Leverage Library Features**: Use built-in capabilities rather than custom solutions
2. **Follow Documentation**: Stick to official guides and best practices
3. **Minimal Custom Code**: Prefer configuration over implementation
4. **Standard Patterns**: Use widely-recognized Android patterns (MVVM, Repository, etc.)
5. **Proven Solutions**: Choose libraries with strong community adoption

#### NetworkModule.kt (`/app/src/main/java/org/jellyfin/androidtv/di/NetworkModule.kt`)

Using **standard Retrofit + OkHttp configuration**:

```kotlin
val networkModule = module {
    
    // Standard OkHttp configuration
    single {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY 
                       else HttpLoggingInterceptor.Level.NONE
            })
            .addInterceptor(get<TraktAuthInterceptor>())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    // Separate OkHttp client for TMDB (different auth)
    single(named("tmdb")) {
        get<OkHttpClient>().newBuilder()
            .addInterceptor(get<TMDBAuthInterceptor>())
            .build()
    }
    
    // Standard Retrofit setup for Trakt
    single {
        Retrofit.Builder()
            .baseUrl("https://api.trakt.tv/")
            .client(get())
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
    
    // Standard Retrofit setup for TMDB
    single(named("tmdb")) {
        Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(get<OkHttpClient>(named("tmdb")))
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
    
    // Standard service creation
    single<TraktApiService> { get<Retrofit>().create(TraktApiService::class.java) }
    single<TMDBApiService> { get<Retrofit>(named("tmdb")).create(TMDBApiService::class.java) }
    
    // Clean auth interceptors (separate concerns)
    single<TraktAuthInterceptor> { TraktAuthInterceptor() }
    single<TMDBAuthInterceptor> { TMDBAuthInterceptor() }
}

// Standard auth interceptor implementation
class TraktAuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val request = original.newBuilder()
            .header("Content-Type", "application/json")
            .header("trakt-api-version", "2")
            .header("trakt-api-key", BuildConfig.TRAKT_CLIENT_ID)
            .build()
        return chain.proceed(request)
    }
}

class TMDBAuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val request = original.newBuilder()
            .header("Authorization", "Bearer ${BuildConfig.TMDB_ACCESS_TOKEN}")
            .build()
        return chain.proceed(request)
    }
}
```

## Phase 3: Repository Layer

### Step 3.1: Create Repository Interfaces

#### TraktRepository.kt
```kotlin
interface TraktRepository {
    suspend fun getTrendingMovies(page: Int, pageSize: Int): Result<List<TraktMovie>>
    suspend fun getPopularMovies(page: Int, pageSize: Int): Result<List<TraktMovie>>
    fun getTrendingMoviesPaged(): Flow<PagingData<TraktMovie>>
    fun getPopularMoviesPaged(): Flow<PagingData<TraktMovie>>
    suspend fun refreshTrendingMovies(): Result<Unit>
    suspend fun refreshPopularMovies(): Result<Unit>
}

interface TMDBRepository {
    suspend fun getMovieImages(tmdbId: Int): Result<TMDBMovieImages>
    suspend fun getConfiguration(): Result<TMDBConfiguration>
    fun getImageUrl(path: String?, size: String = "w500"): String?
}
```

### Step 3.2: Implement Repositories

#### TraktRepositoryImpl.kt (`/app/src/main/java/org/jellyfin/androidtv/data/repository/TraktRepositoryImpl.kt`)

**Simplified Repository using standard patterns**:

```kotlin
@Singleton
class TraktRepositoryImpl @Inject constructor(
    private val apiService: TraktApiService,
    private val movieDao: TraktMovieDao,
    private val tmdbRepository: TMDBRepository
) : TraktRepository {
    
    // Use standard library constants
    private val cacheTimeout = Duration.ofHours(1).toMillis()
    
    override fun getTrendingMoviesPaged(): Flow<PagingData<TraktMovie>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = false,
                initialLoadSize = PAGE_SIZE
            ),
            // Standard Room + Retrofit integration pattern
            remoteMediator = TraktRemoteMediator(
                listType = "trending",
                database = database,
                apiService = apiService,
                tmdbRepository = tmdbRepository
            )
        ) { movieDao.getMoviesByTypePaged("trending") }.flow
    }
    
    override fun getPopularMoviesPaged(): Flow<PagingData<TraktMovie>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = false,
                initialLoadSize = PAGE_SIZE
            ),
            remoteMediator = TraktRemoteMediator(
                listType = "popular",
                database = database,
                apiService = apiService,
                tmdbRepository = tmdbRepository
            )
        ) { movieDao.getMoviesByTypePaged("popular") }.flow
    }
    
    companion object {
        const val PAGE_SIZE = 20
        const val PREFETCH_DISTANCE = 8 // Load more when 8 items from end
    }
}

// Extension function to convert API response to entity
private fun TraktMovieResponse.toTraktMovie(listType: String, position: Int): TraktMovie {
    return TraktMovie(
        id = movie.ids.trakt,
        title = movie.title,
        year = movie.year,
        overview = movie.overview,
        releaseDate = movie.releaseDate,
        runtime = movie.runtime,
        rating = movie.rating,
        votes = movie.votes,
        genres = movie.genres,
        language = movie.language,
        certification = movie.certification,
        tmdbId = movie.ids.tmdb,
        imdbId = movie.ids.imdb,
        watchers = watchers,
        plays = plays,
        listType = listType,
        position = position
    )
}
```

### Step 3.3: Use RemoteMediator (Recommended Pattern)

#### TraktRemoteMediator.kt (`/app/src/main/java/org/jellyfin/androidtv/data/network/paging/TraktRemoteMediator.kt`)

**Using Paging 3's RemoteMediator** (Google's recommended approach for network + database):

```kotlin
@OptIn(ExperimentalPagingApi::class)
class TraktRemoteMediator(
    private val listType: String,
    private val database: TraktDatabase,
    private val apiService: TraktApiService,
    private val tmdbRepository: TMDBRepository
) : RemoteMediator<Int, TraktMovie>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, TraktMovie>
    ): MediatorResult {
        return try {
            // Standard RemoteMediator pattern
            val page = when (loadType) {
                LoadType.REFRESH -> 1
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    val lastItem = state.lastItemOrNull()
                    if (lastItem == null) {
                        1
                    } else {
                        (lastItem.position / PAGE_SIZE) + 2
                    }
                }
            }

            // Fetch from API
            val response = when (listType) {
                "trending" -> apiService.getTrendingMovies(page, PAGE_SIZE)
                "popular" -> apiService.getPopularMovies(page, PAGE_SIZE)
                else -> throw IllegalArgumentException("Unknown list type: $listType")
            }

            val endOfPaginationReached = response.isEmpty()

            // Database transaction (standard Room pattern)
            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    database.movieDao().clearMoviesByType(listType)
                }

                val movies = response.mapIndexed { index, item ->
                    item.toTraktMovie(listType, ((page - 1) * PAGE_SIZE) + index)
                }

                database.movieDao().insertMovies(movies)

                // Fetch images asynchronously (don't block pagination)
                movies.forEach { movie ->
                    movie.tmdbId?.let { tmdbId ->
                        // Launch in background coroutine
                        CoroutineScope(Dispatchers.IO).launch {
                            tmdbRepository.getMovieImages(tmdbId)
                        }
                    }
                }
            }

            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        }
    }

    companion object {
        const val PAGE_SIZE = 20
    }
}
```

## Phase 4: UI Integration

### Step 4.1: Create Data Adapters

#### TraktMovieAdapter.kt (Replace ItemRowAdapter)
```kotlin
class TraktMovieAdapter(
    private val imageHelper: TraktImageHelper,
    private val onItemClick: (TraktMovie) -> Unit
) : PagingDataAdapter<TraktMovie, TraktMovieAdapter.MovieViewHolder>(MovieDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_card_movie, parent, false)
        return MovieViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = getItem(position)
        if (movie != null) {
            holder.bind(movie, imageHelper, onItemClick)
        }
    }
    
    class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val poster: AsyncImageView = itemView.findViewById(R.id.card_image)
        private val title: TextView = itemView.findViewById(R.id.card_title)
        private val year: TextView = itemView.findViewById(R.id.card_year)
        
        fun bind(
            movie: TraktMovie,
            imageHelper: TraktImageHelper,
            onItemClick: (TraktMovie) -> Unit
        ) {
            title.text = movie.title
            year.text = movie.year?.toString() ?: ""
            
            // Load poster image
            val posterUrl = imageHelper.getPosterUrl(movie.tmdbId, "w342")
            poster.load(posterUrl)
            
            itemView.setOnClickListener { onItemClick(movie) }
        }
    }
    
    private class MovieDiffCallback : DiffUtil.ItemCallback<TraktMovie>() {
        override fun areItemsTheSame(oldItem: TraktMovie, newItem: TraktMovie): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: TraktMovie, newItem: TraktMovie): Boolean {
            return oldItem == newItem
        }
    }
}
```

### Step 4.2: Create Image Helper

#### TraktImageHelper.kt (Replace ImageHelper)
```kotlin
class TraktImageHelper(
    private val tmdbRepository: TMDBRepository
) {
    
    fun getPosterUrl(tmdbId: Int?, size: String = "w342"): String? {
        return tmdbId?.let { id ->
            // This would need to be cached/async in real implementation
            runBlocking {
                tmdbRepository.getMovieImages(id).getOrNull()?.let { images ->
                    images.posterPath?.let { path ->
                        tmdbRepository.getImageUrl(path, size)
                    }
                }
            }
        }
    }
    
    fun getBackdropUrl(tmdbId: Int?, size: String = "w1280"): String? {
        return tmdbId?.let { id ->
            runBlocking {
                tmdbRepository.getMovieImages(id).getOrNull()?.let { images ->
                    images.backdropPath?.let { path ->
                        tmdbRepository.getImageUrl(path, size)
                    }
                }
            }
        }
    }
}
```

### Step 4.3: Update Home Fragment

#### TraktHomeRowsFragment.kt (Replace HomeRowsFragment)
```kotlin
class TraktHomeRowsFragment : Fragment() {
    
    private val traktRepository: TraktRepository by inject()
    private val imageHelper: TraktImageHelper by inject()
    
    private lateinit var rowsAdapter: MutableObjectAdapter<Row>
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_home_rows, container, false)
        
        setupRowsAdapter()
        setupRows()
        
        return root
    }
    
    private fun setupRowsAdapter() {
        rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        
        val rowsSupportFragment = childFragmentManager.findFragmentById(R.id.rows_fragment) as RowsSupportFragment
        rowsSupportFragment.adapter = rowsAdapter
        rowsSupportFragment.onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            if (item is TraktMovie) {
                navigateToMovieDetails(item)
            }
        }
    }
    
    private fun setupRows() {
        // Add trending movies row
        addTrendingMoviesRow()
        
        // Add popular movies row  
        addPopularMoviesRow()
    }
    
    private fun addTrendingMoviesRow() {
        lifecycleScope.launch {
            traktRepository.getTrendingMovies(1, 20).fold(
                onSuccess = { movies ->
                    val cardPresenter = MovieCardPresenter(imageHelper)
                    val moviesAdapter = ArrayObjectAdapter(cardPresenter)
                    movies.forEach { movie -> moviesAdapter.add(movie) }
                    
                    val headerItem = HeaderItem(0, "Trending Movies")
                    val listRow = ListRow(headerItem, moviesAdapter)
                    rowsAdapter.add(listRow)
                },
                onFailure = { error ->
                    // Handle error
                    Timber.e(error, "Failed to load trending movies")
                }
            )
        }
    }
    
    private fun addPopularMoviesRow() {
        lifecycleScope.launch {
            traktRepository.getPopularMovies(1, 20).fold(
                onSuccess = { movies ->
                    val cardPresenter = MovieCardPresenter(imageHelper)
                    val moviesAdapter = ArrayObjectAdapter(cardPresenter)
                    movies.forEach { movie -> moviesAdapter.add(movie) }
                    
                    val headerItem = HeaderItem(1, "Popular Movies")
                    val listRow = ListRow(headerItem, moviesAdapter)
                    rowsAdapter.add(listRow)
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to load popular movies")
                }
            )
        }
    }
    
    private fun navigateToMovieDetails(movie: TraktMovie) {
        // Navigate to movie details - preserve existing navigation pattern
        val intent = Intent(requireContext(), DetailsActivity::class.java).apply {
            putExtra("movie", movie)
        }
        startActivity(intent)
    }
}
```

### Step 4.4: Create Movie Card Presenter

#### MovieCardPresenter.kt (Replace existing presenters)
```kotlin
class MovieCardPresenter(
    private val imageHelper: TraktImageHelper
) : Presenter() {
    
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = ImageCardView(parent.context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
        }
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val movie = item as TraktMovie
        val cardView = viewHolder.view as ImageCardView
        
        cardView.titleText = movie.title
        cardView.contentText = movie.year?.toString() ?: ""
        
        // Set dimensions matching current Jellyfin cards
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        
        // Load poster image
        val posterUrl = imageHelper.getPosterUrl(movie.tmdbId, "w342")
        Glide.with(cardView.context)
            .load(posterUrl)
            .placeholder(R.drawable.ic_movie_placeholder)
            .error(R.drawable.ic_movie_error)
            .into(cardView.mainImageView)
    }
    
    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as ImageCardView
        cardView.badgeImage = null
        cardView.mainImage = null
    }
    
    companion object {
        const val CARD_WIDTH = 185
        const val CARD_HEIGHT = 278
    }
}
```

## Phase 5: Dependency Injection Updates

### Step 5.1: Update DI Modules

#### Update AppModule.kt
```kotlin
val appModule = module {
    // ... existing dependencies ...
    
    // Database
    single {
        Room.databaseBuilder(
            androidContext(),
            TraktDatabase::class.java,
            "trakt_database"
        ).build()
    }
    
    single { get<TraktDatabase>().movieDao() }
    single { get<TraktDatabase>().imageDao() }
    
    // Repositories
    single<TraktRepository> { TraktRepositoryImpl(get(), get(), get()) }
    single<TMDBRepository> { TMDBRepositoryImpl(get(), get()) }
    
    // Helpers
    single { TraktImageHelper(get()) }
}
```

## Phase 6: Testing & Validation

### Step 6.1: Comprehensive Unit Tests

#### API Service Tests

**TraktApiServiceTest.kt** (`/app/src/test/java/org/jellyfin/androidtv/data/network/api/TraktApiServiceTest.kt`):
```kotlin
class TraktApiServiceTest : StringSpec({
    
    lateinit var mockWebServer: MockWebServer
    lateinit var traktApiService: TraktApiService
    
    beforeEach {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()
            
        traktApiService = retrofit.create(TraktApiService::class.java)
    }
    
    afterEach {
        mockWebServer.shutdown()
    }
    
    "getTrendingMovies should return correct data" {
        // Given
        val mockResponse = """
            [
                {
                    "watchers": 1234,
                    "movie": {
                        "title": "Test Movie",
                        "year": 2023,
                        "ids": {
                            "trakt": 12345,
                            "tmdb": 67890
                        },
                        "overview": "Test overview",
                        "rating": 8.5,
                        "votes": 1000
                    }
                }
            ]
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setBody(mockResponse)
                .setResponseCode(200)
        )
        
        // When
        val result = traktApiService.getTrendingMovies(1, 20)
        
        // Then
        result shouldHaveSize 1
        result[0].movie.title shouldBe "Test Movie"
        result[0].movie.year shouldBe 2023
        result[0].movie.ids.trakt shouldBe 12345
        result[0].movie.ids.tmdb shouldBe 67890
        result[0].watchers shouldBe 1234
        
        val request = mockWebServer.takeRequest()
        request.path shouldBe "/movies/trending?page=1&limit=20"
        request.getHeader("trakt-api-version") shouldBe "2"
        request.getHeader("trakt-api-key") shouldBe BuildConfig.TRAKT_CLIENT_ID
    }
    
    "getPopularMovies should handle API errors" {
        // Given
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error")
        )
        
        // When & Then
        shouldThrow<HttpException> {
            traktApiService.getPopularMovies(1, 20)
        }
    }
    
    "API should include correct headers" {
        // Given
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        
        // When
        traktApiService.getTrendingMovies()
        
        // Then
        val request = mockWebServer.takeRequest()
        request.getHeader("Content-Type") shouldBe "application/json"
        request.getHeader("trakt-api-version") shouldBe "2"
        request.getHeader("trakt-api-key") shouldNotBeNull()
    }
})
```

**TMDBApiServiceTest.kt** (`/app/src/test/java/org/jellyfin/androidtv/data/network/api/TMDBApiServiceTest.kt`):
```kotlin
class TMDBApiServiceTest : StringSpec({
    
    lateinit var mockWebServer: MockWebServer
    lateinit var tmdbApiService: TMDBApiService
    
    beforeEach {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()
            
        tmdbApiService = retrofit.create(TMDBApiService::class.java)
    }
    
    afterEach {
        mockWebServer.shutdown()
    }
    
    "getMovieDetails should return movie with images" {
        // Given
        val mockResponse = """
            {
                "id": 12345,
                "poster_path": "/path/to/poster.jpg",
                "backdrop_path": "/path/to/backdrop.jpg"
            }
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setBody(mockResponse)
                .setResponseCode(200)
        )
        
        // When
        val result = tmdbApiService.getMovieDetails(12345, "test-api-key")
        
        // Then
        result.id shouldBe 12345
        result.posterPath shouldBe "/path/to/poster.jpg"
        result.backdropPath shouldBe "/path/to/backdrop.jpg"
        
        val request = mockWebServer.takeRequest()
        request.path shouldBe "/movie/12345?api_key=test-api-key"
    }
    
    "getConfiguration should return image configuration" {
        // Given
        val mockResponse = """
            {
                "images": {
                    "base_url": "https://image.tmdb.org/t/p/",
                    "secure_base_url": "https://image.tmdb.org/t/p/",
                    "backdrop_sizes": ["w300", "w780", "w1280", "original"],
                    "poster_sizes": ["w185", "w342", "w500", "w780", "original"]
                }
            }
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setBody(mockResponse)
                .setResponseCode(200)
        )
        
        // When
        val result = tmdbApiService.getConfiguration()
        
        // Then
        result.images.baseUrl shouldBe "https://image.tmdb.org/t/p/"
        result.images.posterSizes shouldContain "w342"
        result.images.backdropSizes shouldContain "w1280"
    }
})
```

#### Database Tests

**TraktDatabaseTest.kt** (`/app/src/test/java/org/jellyfin/androidtv/data/database/TraktDatabaseTest.kt`):
```kotlin
@RunWith(AndroidJUnit4::class)
class TraktDatabaseTest {
    
    private lateinit var database: TraktDatabase
    private lateinit var movieDao: TraktMovieDao
    private lateinit var imageDao: TMDBImageDao
    
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TraktDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        movieDao = database.movieDao()
        imageDao = database.imageDao()
    }
    
    @After
    fun closeDb() {
        database.close()
    }
    
    @Test
    fun insertAndRetrieveTraktMovies() = runTest {
        // Given
        val movies = listOf(
            TraktMovie(
                id = 1, title = "Movie 1", year = 2023, listType = "trending", 
                position = 0, tmdbId = 101, overview = "Test overview 1"
            ),
            TraktMovie(
                id = 2, title = "Movie 2", year = 2022, listType = "trending", 
                position = 1, tmdbId = 102, overview = "Test overview 2"
            )
        )
        
        // When
        movieDao.insertMovies(movies)
        val retrievedMovies = movieDao.getMoviesByType("trending", 10, 0)
        
        // Then
        retrievedMovies shouldHaveSize 2
        retrievedMovies[0].title shouldBe "Movie 1"
        retrievedMovies[1].title shouldBe "Movie 2"
        retrievedMovies.forAll { it.listType shouldBe "trending" }
    }
    
    @Test
    fun clearMoviesByType() = runTest {
        // Given
        val trendingMovies = listOf(
            TraktMovie(id = 1, title = "Trending", listType = "trending", position = 0),
            TraktMovie(id = 2, title = "Trending 2", listType = "trending", position = 1)
        )
        val popularMovies = listOf(
            TraktMovie(id = 3, title = "Popular", listType = "popular", position = 0)
        )
        
        movieDao.insertMovies(trendingMovies + popularMovies)
        
        // When
        movieDao.clearMoviesByType("trending")
        
        // Then
        movieDao.getMoviesByType("trending", 10, 0) shouldHaveSize 0
        movieDao.getMoviesByType("popular", 10, 0) shouldHaveSize 1
    }
    
    @Test
    fun pagingSourceReturnsCorrectData() = runTest {
        // Given
        val movies = (1..50).map {
            TraktMovie(
                id = it, title = "Movie $it", listType = "trending", 
                position = it - 1, year = 2023
            )
        }
        movieDao.insertMovies(movies)
        
        // When
        val pagingSource = movieDao.getMoviesByTypePaged("trending")
        val loadResult = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )
        
        // Then
        loadResult shouldBe instanceOf<PagingSource.LoadResult.Page<*, *>>()
        val pageResult = loadResult as PagingSource.LoadResult.Page
        pageResult.data shouldHaveSize 20
        pageResult.data.first().title shouldBe "Movie 1"
        pageResult.data.last().title shouldBe "Movie 20"
    }
    
    @Test
    fun insertAndRetrieveTMDBImages() = runTest {
        // Given
        val images = TMDBMovieImages(
            tmdbId = 12345,
            posterPath = "/poster.jpg",
            backdropPath = "/backdrop.jpg"
        )
        
        // When
        imageDao.insertImages(images)
        val retrieved = imageDao.getMovieImages(12345)
        
        // Then
        retrieved shouldNotBe null
        retrieved!!.posterPath shouldBe "/poster.jpg"
        retrieved.backdropPath shouldBe "/backdrop.jpg"
    }
    
    @Test
    fun deleteStaleMovies() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val oldTime = currentTime - TimeUnit.HOURS.toMillis(2)
        
        val freshMovies = listOf(
            TraktMovie(id = 1, title = "Fresh", listType = "trending", position = 0, lastUpdated = currentTime)
        )
        val staleMovies = listOf(
            TraktMovie(id = 2, title = "Stale", listType = "trending", position = 1, lastUpdated = oldTime)
        )
        
        movieDao.insertMovies(freshMovies + staleMovies)
        
        // When
        movieDao.deleteStaleMovies(currentTime - TimeUnit.HOURS.toMillis(1))
        
        // Then
        val remainingMovies = movieDao.getMoviesByType("trending", 10, 0)
        remainingMovies shouldHaveSize 1
        remainingMovies[0].title shouldBe "Fresh"
    }
}
```

#### Repository Tests

**TraktRepositoryImplTest.kt** (`/app/src/test/java/org/jellyfin/androidtv/data/repository/TraktRepositoryImplTest.kt`):
```kotlin
class TraktRepositoryImplTest : StringSpec({
    
    val mockApiService = mockk<TraktApiService>()
    val mockDatabase = mockk<TraktDatabase>()
    val mockMovieDao = mockk<TraktMovieDao>()
    val mockTmdbRepository = mockk<TMDBRepository>()
    
    val repository = TraktRepositoryImpl(mockApiService, mockMovieDao, mockTmdbRepository)
    
    beforeEach {
        clearAllMocks()
        every { mockDatabase.movieDao() } returns mockMovieDao
        every { mockDatabase.withTransaction(any<suspend () -> Any>()) } coAnswers {
            firstArg<suspend () -> Any>().invoke()
        }
    }
    
    "getTrendingMoviesPaged should return paging data" {
        // Given
        val mockMovies = listOf(
            TraktMovie(id = 1, title = "Movie 1", listType = "trending", position = 0),
            TraktMovie(id = 2, title = "Movie 2", listType = "trending", position = 1)
        )
        
        every { mockMovieDao.getMoviesByTypePaged("trending") } returns TestPagingSource(mockMovies)
        
        // When
        val flow = repository.getTrendingMoviesPaged()
        
        // Then - This would need a test collector to verify PagingData
        // For now, verify that the method returns a Flow
        flow shouldBe instanceOf<Flow<PagingData<TraktMovie>>>()
    }
    
    "repository should use correct page size and prefetch distance" {
        // Given/When
        val flow = repository.getTrendingMoviesPaged()
        
        // Then - Verify constants are correct
        TraktRepositoryImpl.PAGE_SIZE shouldBe 20
        TraktRepositoryImpl.PREFETCH_DISTANCE shouldBe 8
    }
})

// Test helper class for PagingSource
class TestPagingSource(
    private val data: List<TraktMovie>
) : PagingSource<Int, TraktMovie>() {
    
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TraktMovie> {
        val page = params.key ?: 0
        val pageSize = params.loadSize
        val startIndex = page * pageSize
        val endIndex = minOf(startIndex + pageSize, data.size)
        
        if (startIndex >= data.size) {
            return LoadResult.Page(emptyList(), null, null)
        }
        
        return LoadResult.Page(
            data = data.subList(startIndex, endIndex),
            prevKey = if (page == 0) null else page - 1,
            nextKey = if (endIndex >= data.size) null else page + 1
        )
    }
    
    override fun getRefreshKey(state: PagingState<Int, TraktMovie>): Int? = null
}
```

**TMDBRepositoryImplTest.kt** (`/app/src/test/java/org/jellyfin/androidtv/data/repository/TMDBRepositoryImplTest.kt`):
```kotlin
class TMDBRepositoryImplTest : StringSpec({
    
    val mockApiService = mockk<TMDBApiService>()
    val mockImageDao = mockk<TMDBImageDao>()
    
    val repository = TMDBRepositoryImpl(mockApiService, mockImageDao)
    
    beforeEach {
        clearAllMocks()
    }
    
    "getMovieImages should return cached data when available" {
        // Given
        val cachedImages = TMDBMovieImages(
            tmdbId = 12345,
            posterPath = "/cached-poster.jpg",
            backdropPath = "/cached-backdrop.jpg"
        )
        coEvery { mockImageDao.getMovieImages(12345) } returns cachedImages
        
        // When
        val result = repository.getMovieImages(12345)
        
        // Then
        result.isSuccess shouldBe true
        result.getOrNull() shouldBe cachedImages
        coVerify(exactly = 0) { mockApiService.getMovieDetails(any(), any()) }
    }
    
    "getMovieImages should fetch from API when cache is empty" {
        // Given
        val apiResponse = TMDBMovieDetails(
            id = 12345,
            posterPath = "/api-poster.jpg",
            backdropPath = "/api-backdrop.jpg"
        )
        
        coEvery { mockImageDao.getMovieImages(12345) } returns null
        coEvery { mockApiService.getMovieDetails(12345, any()) } returns apiResponse
        coEvery { mockImageDao.insertImages(any()) } just Runs
        
        // When
        val result = repository.getMovieImages(12345)
        
        // Then
        result.isSuccess shouldBe true
        val images = result.getOrNull()!!
        images.posterPath shouldBe "/api-poster.jpg"
        images.backdropPath shouldBe "/api-backdrop.jpg"
        
        coVerify { mockApiService.getMovieDetails(12345, BuildConfig.TMDB_API_KEY) }
        coVerify { mockImageDao.insertImages(any()) }
    }
    
    "getImageUrl should construct correct URL" {
        // Given
        val mockConfig = TMDBConfiguration(
            images = TMDBImageConfiguration(
                baseUrl = "https://image.tmdb.org/t/p/",
                secureBaseUrl = "https://image.tmdb.org/t/p/",
                posterSizes = listOf("w185", "w342", "w500"),
                backdropSizes = listOf("w300", "w780", "w1280")
            )
        )
        
        // Mock the configuration
        every { repository.getCachedConfiguration() } returns mockConfig
        
        // When
        val url = repository.getImageUrl("/poster.jpg", "w342")
        
        // Then
        url shouldBe "https://image.tmdb.org/t/p/w342/poster.jpg"
    }
    
    "getImageUrl should return null for null path" {
        // When
        val url = repository.getImageUrl(null, "w342")
        
        // Then
        url shouldBe null
    }
})
```

#### Paging Tests

**TraktRemoteMediatorTest.kt** (`/app/src/test/java/org/jellyfin/androidtv/data/network/paging/TraktRemoteMediatorTest.kt`):
```kotlin
@OptIn(ExperimentalPagingApi::class)
class TraktRemoteMediatorTest : StringSpec({
    
    val mockDatabase = mockk<TraktDatabase>()
    val mockMovieDao = mockk<TraktMovieDao>()
    val mockApiService = mockk<TraktApiService>()
    val mockTmdbRepository = mockk<TMDBRepository>()
    
    val mediator = TraktRemoteMediator(
        listType = "trending",
        database = mockDatabase,
        apiService = mockApiService,
        tmdbRepository = mockTmdbRepository
    )
    
    beforeEach {
        clearAllMocks()
        every { mockDatabase.movieDao() } returns mockMovieDao
        every { mockDatabase.withTransaction(any<suspend () -> Any>()) } coAnswers {
            firstArg<suspend () -> Any>().invoke()
        }
    }
    
    "load should clear cache and fetch first page on REFRESH" {
        // Given
        val mockResponse = listOf(
            TraktMovieResponse(
                watchers = 100,
                movie = TraktMovieDetails(
                    title = "Test Movie",
                    year = 2023,
                    ids = TraktIds(trakt = 1, slug = "test", tmdb = 101, imdb = "tt123")
                )
            )
        )
        
        val pagingState = PagingState<Int, TraktMovie>(
            pages = emptyList(),
            anchorPosition = null,
            config = PagingConfig(pageSize = 20),
            leadingPlaceholderCount = 0
        )
        
        coEvery { mockApiService.getTrendingMovies(1, 20) } returns mockResponse
        coEvery { mockMovieDao.clearMoviesByType("trending") } just Runs
        coEvery { mockMovieDao.insertMovies(any()) } just Runs
        coEvery { mockTmdbRepository.getMovieImages(any()) } returns Result.success(
            TMDBMovieImages(tmdbId = 101, posterPath = "/test.jpg", backdropPath = "/test-bg.jpg")
        )
        
        // When
        val result = mediator.load(LoadType.REFRESH, pagingState)
        
        // Then
        result shouldBe instanceOf<RemoteMediator.MediatorResult.Success>()
        val success = result as RemoteMediator.MediatorResult.Success
        success.endOfPaginationReached shouldBe false
        
        coVerify { mockMovieDao.clearMoviesByType("trending") }
        coVerify { mockApiService.getTrendingMovies(1, 20) }
        coVerify { mockMovieDao.insertMovies(any()) }
    }
    
    "load should append data on APPEND" {
        // Given
        val lastMovie = TraktMovie(
            id = 19, title = "Last Movie", listType = "trending", 
            position = 19, year = 2023
        )
        
        val pagingState = PagingState(
            pages = listOf(
                PagingSource.LoadResult.Page(
                    data = listOf(lastMovie),
                    prevKey = null,
                    nextKey = 2
                )
            ),
            anchorPosition = 0,
            config = PagingConfig(pageSize = 20),
            leadingPlaceholderCount = 0
        )
        
        val mockResponse = listOf(
            TraktMovieResponse(
                watchers = 50,
                movie = TraktMovieDetails(
                    title = "Next Page Movie",
                    year = 2023,
                    ids = TraktIds(trakt = 20, slug = "next", tmdb = 201, imdb = "tt456")
                )
            )
        )
        
        coEvery { mockApiService.getTrendingMovies(2, 20) } returns mockResponse
        coEvery { mockMovieDao.insertMovies(any()) } just Runs
        
        // When
        val result = mediator.load(LoadType.APPEND, pagingState)
        
        // Then
        result shouldBe instanceOf<RemoteMediator.MediatorResult.Success>()
        coVerify(exactly = 0) { mockMovieDao.clearMoviesByType(any()) }
        coVerify { mockApiService.getTrendingMovies(2, 20) }
        coVerify { mockMovieDao.insertMovies(any()) }
    }
    
    "load should return error on API failure" {
        // Given
        val pagingState = PagingState<Int, TraktMovie>(
            pages = emptyList(),
            anchorPosition = null,
            config = PagingConfig(pageSize = 20),
            leadingPlaceholderCount = 0
        )
        
        val exception = IOException("Network error")
        coEvery { mockApiService.getTrendingMovies(any(), any()) } throws exception
        
        // When
        val result = mediator.load(LoadType.REFRESH, pagingState)
        
        // Then
        result shouldBe instanceOf<RemoteMediator.MediatorResult.Error>()
        val error = result as RemoteMediator.MediatorResult.Error
        error.throwable shouldBe exception
    }
    
    "load should handle empty response" {
        // Given
        val pagingState = PagingState<Int, TraktMovie>(
            pages = emptyList(),
            anchorPosition = null,
            config = PagingConfig(pageSize = 20),
            leadingPlaceholderCount = 0
        )
        
        coEvery { mockApiService.getTrendingMovies(1, 20) } returns emptyList()
        coEvery { mockMovieDao.clearMoviesByType("trending") } just Runs
        coEvery { mockMovieDao.insertMovies(any()) } just Runs
        
        // When
        val result = mediator.load(LoadType.REFRESH, pagingState)
        
        // Then
        result shouldBe instanceOf<RemoteMediator.MediatorResult.Success>()
        val success = result as RemoteMediator.MediatorResult.Success
        success.endOfPaginationReached shouldBe true
    }
})
```

#### ViewModel Tests

**TraktHomeViewModelTest.kt** (`/app/src/test/java/org/jellyfin/androidtv/presentation/home/TraktHomeViewModelTest.kt`):
```kotlin
class TraktHomeViewModelTest : StringSpec({
    
    val mockTraktRepository = mockk<TraktRepository>()
    val testDispatcher = UnconfinedTestDispatcher()
    
    lateinit var viewModel: TraktHomeViewModel
    
    beforeEach {
        Dispatchers.setMain(testDispatcher)
        clearAllMocks()
        viewModel = TraktHomeViewModel(mockTraktRepository)
    }
    
    afterEach {
        Dispatchers.resetMain()
    }
    
    "viewModel should expose trending movies flow" {
        // Given
        val testPagingData = PagingData.from(
            listOf(
                TraktMovie(id = 1, title = "Trending 1", listType = "trending", position = 0),
                TraktMovie(id = 2, title = "Trending 2", listType = "trending", position = 1)
            )
        )
        
        every { mockTraktRepository.getTrendingMoviesPaged() } returns flowOf(testPagingData)
        
        // When
        val flow = viewModel.trendingMovies
        
        // Then
        flow shouldBe instanceOf<Flow<PagingData<TraktMovie>>>()
        verify { mockTraktRepository.getTrendingMoviesPaged() }
    }
    
    "viewModel should expose popular movies flow" {
        // Given
        val testPagingData = PagingData.from(
            listOf(
                TraktMovie(id = 3, title = "Popular 1", listType = "popular", position = 0),
                TraktMovie(id = 4, title = "Popular 2", listType = "popular", position = 1)
            )
        )
        
        every { mockTraktRepository.getPopularMoviesPaged() } returns flowOf(testPagingData)
        
        // When
        val flow = viewModel.popularMovies
        
        // Then
        flow shouldBe instanceOf<Flow<PagingData<TraktMovie>>>()
        verify { mockTraktRepository.getPopularMoviesPaged() }
    }
})
```

### Step 6.2: Integration Tests

#### End-to-End API Integration

**TraktApiIntegrationTest.kt** (`/app/src/androidTest/java/org/jellyfin/androidtv/integration/TraktApiIntegrationTest.kt`):
```kotlin
@RunWith(AndroidJUnit4::class)
class TraktApiIntegrationTest {
    
    private lateinit var traktApiService: TraktApiService
    private lateinit var tmdbApiService: TMDBApiService
    
    @Before
    fun setup() {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(TraktAuthInterceptor())
            .build()
            
        val traktRetrofit = Retrofit.Builder()
            .baseUrl("https://api.trakt.tv/")
            .client(okHttpClient)
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()
            
        val tmdbOkHttpClient = OkHttpClient.Builder()
            .addInterceptor(TMDBAuthInterceptor())
            .build()
            
        val tmdbRetrofit = Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(tmdbOkHttpClient)
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()
            
        traktApiService = traktRetrofit.create(TraktApiService::class.java)
        tmdbApiService = tmdbRetrofit.create(TMDBApiService::class.java)
    }
    
    @Test
    fun traktApiReturnsValidTrendingMovies() = runTest {
        // When
        val movies = traktApiService.getTrendingMovies(1, 5)
        
        // Then
        assertThat(movies).isNotEmpty()
        movies.forEach { movieResponse ->
            assertThat(movieResponse.movie.title).isNotEmpty()
            assertThat(movieResponse.movie.ids.trakt).isGreaterThan(0)
            assertThat(movieResponse.watchers).isAtLeast(0)
        }
    }
    
    @Test
    fun tmdbApiReturnsValidMovieImages() = runTest {
        // Given - Use a well-known movie ID
        val tmdbId = 550 // Fight Club
        
        // When
        val movieDetails = tmdbApiService.getMovieDetails(tmdbId, BuildConfig.TMDB_API_KEY)
        
        // Then
        assertThat(movieDetails.id).isEqualTo(tmdbId)
        assertThat(movieDetails.posterPath).isNotNull()
        assertThat(movieDetails.backdropPath).isNotNull()
    }
    
    @Test
    fun tmdbConfigurationReturnsValidData() = runTest {
        // When
        val config = tmdbApiService.getConfiguration()
        
        // Then
        assertThat(config.images.baseUrl).isNotEmpty()
        assertThat(config.images.posterSizes).contains("w342")
        assertThat(config.images.backdropSizes).contains("w1280")
    }
}
```

#### Database Integration Tests

**DatabaseIntegrationTest.kt** (`/app/src/androidTest/java/org/jellyfin/androidtv/integration/DatabaseIntegrationTest.kt`):
```kotlin
@RunWith(AndroidJUnit4::class)
class DatabaseIntegrationTest {
    
    private lateinit var database: TraktDatabase
    
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TraktDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }
    
    @After
    fun closeDb() {
        database.close()
    }
    
    @Test
    fun fullMovieWorkflow() = runTest {
        val movieDao = database.movieDao()
        val imageDao = database.imageDao()
        
        // 1. Insert movies
        val movies = listOf(
            TraktMovie(
                id = 1, title = "Integration Test Movie", year = 2023, 
                listType = "trending", position = 0, tmdbId = 12345,
                overview = "Test overview", rating = 8.5
            )
        )
        movieDao.insertMovies(movies)
        
        // 2. Insert corresponding images
        val images = TMDBMovieImages(
            tmdbId = 12345,
            posterPath = "/test-poster.jpg",
            backdropPath = "/test-backdrop.jpg"
        )
        imageDao.insertImages(images)
        
        // 3. Retrieve and verify complete data
        val retrievedMovies = movieDao.getMoviesByType("trending", 10, 0)
        val retrievedImages = imageDao.getMovieImages(12345)
        
        assertThat(retrievedMovies).hasSize(1)
        assertThat(retrievedMovies[0].title).isEqualTo("Integration Test Movie")
        assertThat(retrievedMovies[0].tmdbId).isEqualTo(12345)
        
        assertThat(retrievedImages).isNotNull()
        assertThat(retrievedImages!!.posterPath).isEqualTo("/test-poster.jpg")
    }
    
    @Test
    fun pagingIntegrationTest() = runTest {
        val movieDao = database.movieDao()
        
        // Insert 100 test movies
        val movies = (1..100).map { index ->
            TraktMovie(
                id = index, title = "Movie $index", year = 2023,
                listType = "trending", position = index - 1
            )
        }
        movieDao.insertMovies(movies)
        
        // Test paging source
        val pagingSource = movieDao.getMoviesByTypePaged("trending")
        
        // Load first page
        val firstPageResult = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null, loadSize = 20, placeholdersEnabled = false
            )
        )
        
        assertThat(firstPageResult).isInstanceOf(PagingSource.LoadResult.Page::class.java)
        val firstPage = firstPageResult as PagingSource.LoadResult.Page
        assertThat(firstPage.data).hasSize(20)
        assertThat(firstPage.data.first().title).isEqualTo("Movie 1")
        
        // Load second page
        val secondPageResult = pagingSource.load(
            PagingSource.LoadParams.Append(
                key = firstPage.nextKey!!, loadSize = 20, placeholdersEnabled = false
            )
        )
        
        assertThat(secondPageResult).isInstanceOf(PagingSource.LoadResult.Page::class.java)
        val secondPage = secondPageResult as PagingSource.LoadResult.Page
        assertThat(secondPage.data).hasSize(20)
        assertThat(secondPage.data.first().title).isEqualTo("Movie 21")
    }
}
```

#### UI Integration Tests

**TraktHomeFragmentTest.kt** (`/app/src/androidTest/java/org/jellyfin/androidtv/presentation/home/TraktHomeFragmentTest.kt`):
```kotlin
@RunWith(AndroidJUnit4::class)
class TraktHomeFragmentTest {
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    
    private val mockRepository = mockk<TraktRepository>()
    
    @Before
    fun setup() {
        // Setup test DI
        startKoin {
            modules(
                module {
                    single<TraktRepository> { mockRepository }
                }
            )
        }
    }
    
    @After
    fun teardown() {
        stopKoin()
    }
    
    @Test
    fun homeFragmentDisplaysMovieRows() {
        // Given
        val testMovies = listOf(
            TraktMovie(id = 1, title = "Test Movie 1", year = 2023, listType = "trending", position = 0),
            TraktMovie(id = 2, title = "Test Movie 2", year = 2023, listType = "popular", position = 0)
        )
        
        every { mockRepository.getTrendingMoviesPaged() } returns flowOf(
            PagingData.from(testMovies.filter { it.listType == "trending" })
        )
        every { mockRepository.getPopularMoviesPaged() } returns flowOf(
            PagingData.from(testMovies.filter { it.listType == "popular" })
        )
        
        // When
        val scenario = launchFragment<TraktHomeFragment>()
        
        // Then
        onView(withText("Trending Movies")).check(matches(isDisplayed()))
        onView(withText("Popular Movies")).check(matches(isDisplayed()))
        onView(withText("Test Movie 1")).check(matches(isDisplayed()))
        onView(withText("Test Movie 2")).check(matches(isDisplayed()))
    }
    
    @Test
    fun clickingMovieNavigatesToDetails() {
        // Given
        val testMovie = TraktMovie(
            id = 1, title = "Clickable Movie", year = 2023, 
            listType = "trending", position = 0, tmdbId = 12345
        )
        
        every { mockRepository.getTrendingMoviesPaged() } returns flowOf(
            PagingData.from(listOf(testMovie))
        )
        every { mockRepository.getPopularMoviesPaged() } returns flowOf(PagingData.empty())
        
        // When
        val scenario = launchFragment<TraktHomeFragment>()
        onView(withText("Clickable Movie")).perform(click())
        
        // Then
        intended(hasComponent(MovieDetailsActivity::class.java.name))
        intended(hasExtra("movie_id", 1))
        intended(hasExtra("tmdb_id", 12345))
    }
}
```

### Step 6.3: Test Configuration

**Add to `/app/build.gradle.kts`**:
```kotlin
dependencies {
    // Unit Testing
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
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
```

**Add to `/gradle/libs.versions.toml`**:
```toml
[versions]
mockwebserver = "4.12.0"
androidx-test = "1.6.1"
espresso = "3.6.1"

[libraries]
# Testing
squareup-okhttp3-mockwebserver = { group = "com.squareup.okhttp3", name = "mockwebserver", version.ref = "mockwebserver" }
androidx-test-ext-junit = { group = "androidx.test.ext", name = "junit", version.ref = "androidx-test" }
androidx-test-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espresso" }
androidx-test-runner = { group = "androidx.test", name = "runner", version.ref = "androidx-test" }
androidx-test-rules = { group = "androidx.test", name = "rules", version.ref = "androidx-test" }
androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
androidx-paging-testing = { group = "androidx.paging", name = "paging-testing", version.ref = "paging" }
mockk-android = { group = "io.mockk", name = "mockk-android", version.ref = "mockk" }
koin-test = { group = "io.insert-koin", name = "koin-test", version.ref = "koin" }
```

## Phase 7: Remove Jellyfin UI Components

### Step 7.1: Remove Startup/Welcome Screens

The current app shows "Welcome to Jellyfin!" and "Connect to your server to get started." on launch. These need to be completely removed:

#### Identify and Remove Startup Components

**Find startup/welcome screens**:
```bash
# Search for welcome/startup related files
find app/src -name "*.kt" -o -name "*.java" | xargs grep -l -i "welcome\|connect.*server\|startup"
find app/src -name "*.xml" | xargs grep -l -i "welcome\|connect.*server"
```

**Typical files to modify/remove**:
- `/app/src/main/java/org/jellyfin/androidtv/ui/startup/` (entire directory)
- `/app/src/main/java/org/jellyfin/androidtv/ui/SelectUserFragment.*`
- `/app/src/main/java/org/jellyfin/androidtv/ui/ConnectFragment.*` 
- Any `StartupActivity` or `WelcomeActivity` classes

#### Remove Server Connection Logic

**In MainActivity.kt** - Replace server connection checks:
```kotlin
// REMOVE: Server connection logic
// if (SessionRepository.currentSession.value == null) {
//     navigateToStartup()
//     return
// }

// REPLACE WITH: Direct navigation to home
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    
    // Skip all server connection logic - go straight to home
    navigateToHome()
}

private fun navigateToHome() {
    supportFragmentManager.beginTransaction()
        .replace(R.id.fragment_container, TraktHomeFragment())
        .commit()
}
```

**Remove from AndroidManifest.xml**:
```xml
<!-- REMOVE startup/welcome activities -->
<!-- <activity android:name=".ui.startup.StartupActivity" /> -->
<!-- <activity android:name=".ui.startup.ConnectActivity" /> -->
<!-- Any other startup-related activities -->
```

#### Bypass Authentication/Session Logic

**Create stub session for UI compatibility**:
```kotlin
// In TraktSessionManager.kt (new file)
class TraktSessionManager {
    // Create a stub session to satisfy any remaining session checks
    val currentSession = MutableStateFlow(
        Session(
            userId = "trakt_user",
            serverInfo = ServerInfo(
                name = "Trakt Movies",
                version = "1.0.0"
            )
        )
    )
}
```

**Update DI module**:
```kotlin
// In AppModule.kt - replace Jellyfin session with stub
single<SessionRepository> { TraktSessionManager() }
```

### Step 7.2: Create Direct Home Navigation

#### Update App Launch Flow

**New App Flow**:
```
App Launch → MainActivity → TraktHomeFragment (immediately)
```

**Remove these navigation steps**:
- Server selection screens
- User authentication screens  
- Connection status checks
- Any "setup" or "configuration" screens

#### Create New Home Structure

**TraktHomeFragment.kt** (`/app/src/main/java/org/jellyfin/androidtv/presentation/home/TraktHomeFragment.kt`):
```kotlin
class TraktHomeFragment : Fragment() {
    
    private val viewModel: TraktHomeViewModel by viewModel()
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_trakt_home, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupRows()
    }
    
    private fun setupRows() {
        // Create exactly 2 rows as specified:
        // 1. Trending Movies
        // 2. Popular Movies
        
        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        
        // Row 1: Trending Movies
        addTrendingMoviesRow(rowsAdapter)
        
        // Row 2: Popular Movies  
        addPopularMoviesRow(rowsAdapter)
        
        val rowsFragment = childFragmentManager.findFragmentById(R.id.rows_fragment) as RowsSupportFragment
        rowsFragment.adapter = rowsAdapter
        rowsFragment.onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            when (item) {
                is TraktMovie -> navigateToMovieDetails(item)
            }
        }
    }
    
    private fun addTrendingMoviesRow(adapter: ArrayObjectAdapter<Row>) {
        lifecycleScope.launch {
            viewModel.trendingMovies.collectLatest { pagingData ->
                val cardPresenter = MovieCardPresenter()
                val moviesAdapter = ArrayObjectAdapter(cardPresenter)
                
                // Convert PagingData to list for Leanback adapter
                // (This is a simplified approach - you may want to create a custom adapter)
                pagingData.map { movie -> 
                    moviesAdapter.add(movie)
                }
                
                val headerItem = HeaderItem(0L, "Trending Movies")
                val listRow = ListRow(headerItem, moviesAdapter)
                adapter.add(listRow)
            }
        }
    }
    
    private fun addPopularMoviesRow(adapter: ArrayObjectAdapter<Row>) {
        lifecycleScope.launch {
            viewModel.popularMovies.collectLatest { pagingData ->
                val cardPresenter = MovieCardPresenter()
                val moviesAdapter = ArrayObjectAdapter(cardPresenter)
                
                pagingData.map { movie -> 
                    moviesAdapter.add(movie)
                }
                
                val headerItem = HeaderItem(1L, "Popular Movies")
                val listRow = ListRow(headerItem, moviesAdapter)
                adapter.add(listRow)
            }
        }
    }
    
    private fun navigateToMovieDetails(movie: TraktMovie) {
        val intent = Intent(requireContext(), MovieDetailsActivity::class.java).apply {
            putExtra("movie_id", movie.id)
            putExtra("tmdb_id", movie.tmdbId)
        }
        startActivity(intent)
    }
}
```

#### Simple Home Layout

**fragment_trakt_home.xml**:
```xml
<?xml version="1.0" encoding="utf-8"?>
<fragment xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/rows_fragment"
    android:name="androidx.leanback.app.RowsSupportFragment"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

### Step 7.3: Future Navigation Structure

**Planned App Structure**:
```
MainActivity
├── TraktHomeFragment (current - with 2 movie rows)
├── MoviesFragment (future - all movie categories)  
├── TVShowsFragment (future - all TV categories)
└── MovieDetailsActivity (current - for movie details)
```

**Navigation will eventually be**:
- **Home**: Overview with highlights from movies and TV
- **Movies**: All movie categories (trending, popular, top rated, etc.)
- **TV Shows**: All TV show categories (trending, popular, airing today, etc.)
- **Details**: Movie/show details page

### Step 7.4: Remove Server-Related Strings

**Update `/app/src/main/res/values/strings.xml`**:
```xml
<!-- REMOVE Jellyfin-specific strings -->
<!-- <string name="lbl_welcome">Welcome to Jellyfin!</string> -->
<!-- <string name="lbl_connect_server">Connect to your server to get started.</string> -->
<!-- <string name="lbl_server_connection">Server Connection</string> -->

<!-- ADD new app strings -->
<string name="lbl_trending_movies">Trending Movies</string>
<string name="lbl_popular_movies">Popular Movies</string>
<string name="lbl_movie_details">Movie Details</string>
```

## Phase 8: Complete App Transformation Strategy

Since this will become a **completely separate app** with all Jellyfin traces removed:

### App Identity Changes

#### Step 7.1: Update Application Identity

**Application ID**: Change in `/app/build.gradle.kts`:
```kotlin
android {
    namespace = "org.yourcompany.moviebrowser" // New package name
    defaultConfig {
        applicationId = "org.yourcompany.moviebrowser"
        // ... other config
    }
}
```

**App Name**: Update in `/app/src/main/res/values/strings.xml`:
```xml
<string name="app_name">Movie Browser</string>
<string name="app_name_debug">Movie Browser (Debug)</string>
<string name="app_name_release">Movie Browser</string>
```

**Package Refactoring**: Use Android Studio's refactor tool:
1. Right-click on `org.jellyfin.androidtv` package
2. Select "Refactor" → "Rename"
3. Change to `org.yourcompany.moviebrowser`
4. Android Studio will update all imports automatically

#### Step 7.2: Remove Jellyfin Dependencies

**Remove from `/gradle/libs.versions.toml`**:
```toml
# Remove these entries:
jellyfin-sdk = "1.7.0-beta.3"
jellyfin-androidx-media = "1.8.0+1"
```

**Remove from `/app/build.gradle.kts`**:
```kotlin
// Remove these dependencies:
implementation(libs.jellyfin.sdk)
implementation(projects.playback.jellyfin)
// Keep only what's needed for the new app
```

#### Step 7.3: Clean Module Structure

**Keep only necessary modules**:
- `app/` - Main application
- `preference/` - Can be kept if useful
- **Remove**: All `playback/` modules (not needed for browsing-only app)

**Update `/settings.gradle.kts`**:
```kotlin
rootProject.name = "movie-browser"

include(":app")
include(":preference") // Only if needed
// Remove all playback modules
```

### Implementation Strategy

**Phase-by-Phase Replacement**:
1. **Phase 1-6**: Implement Trakt/TMDB alongside existing Jellyfin code
2. **Phase 7**: Remove all Jellyfin code and dependencies
3. **Phase 8**: Rebrand and finalize as new app

**Benefits of this approach**:
- Can test Trakt implementation while keeping app functional
- Easy rollback if issues arise
- Clean separation of concerns
- Final product has no legacy code

## Implementation Checklist

### Phase 1: Foundation Setup
- [ ] **Step 1.1: Add Dependencies**
  - [ ] Update `/gradle/libs.versions.toml` with new library versions
  - [ ] Add Room database dependencies to app module
  - [ ] Add Retrofit and networking dependencies
  - [ ] Add Paging 3 library dependencies
  - [ ] Sync project and verify all dependencies resolve

- [ ] **Step 1.2: Create Data Models**
  - [ ] Create `/app/src/main/java/org/jellyfin/androidtv/data/database/entities/TraktMovie.kt`
  - [ ] Create `/app/src/main/java/org/jellyfin/androidtv/data/database/entities/TMDBMovieImages.kt`
  - [ ] Create `/app/src/main/java/org/jellyfin/androidtv/data/network/dto/trakt/TraktIds.kt`
  - [ ] Create `/app/src/main/java/org/jellyfin/androidtv/data/network/dto/trakt/TraktMovieResponse.kt`
  - [ ] Create `/app/src/main/java/org/jellyfin/androidtv/data/network/dto/trakt/TraktMovieDetails.kt`
  - [ ] Create `/app/src/main/java/org/jellyfin/androidtv/data/network/dto/tmdb/TMDBConfiguration.kt`
  - [ ] Create `/app/src/main/java/org/jellyfin/androidtv/data/network/dto/tmdb/TMDBMovieDetails.kt`
  - [ ] Create `/app/src/main/java/org/jellyfin/androidtv/data/network/dto/tmdb/TMDBMovieImagesResponse.kt`

- [ ] **Step 1.3: Create Room Database**
  - [ ] Create `/app/src/main/java/org/jellyfin/androidtv/data/database/TraktDatabase.kt`
  - [ ] Create `/app/src/main/java/org/jellyfin/androidtv/data/database/dao/TraktMovieDao.kt`
  - [ ] Create `/app/src/main/java/org/jellyfin/androidtv/data/database/dao/TMDBImageDao.kt`
  - [ ] Create `/app/src/main/java/org/jellyfin/androidtv/data/database/Converters.kt`
  - [ ] Test database creation and basic CRUD operations

### Phase 2: API Layer
- [ ] **Step 2.1: Create API Interfaces**
  - [ ] Create `/app/src/main/java/org/jellyfin/androidtv/data/network/api/TraktApiService.kt`
  - [ ] Create `/app/src/main/java/org/jellyfin/androidtv/data/network/api/TMDBApiService.kt`
  - [ ] Define trending movies endpoint
  - [ ] Define popular movies endpoint
  - [ ] Define TMDB movie details endpoint
  - [ ] Define TMDB configuration endpoint

- [ ] **Step 2.2: Create API Client Configuration**
  - [ ] Create `/app/src/main/java/org/jellyfin/androidtv/data/network/interceptors/TraktAuthInterceptor.kt`
  - [ ] Create `/app/src/main/java/org/jellyfin/androidtv/data/network/interceptors/TMDBAuthInterceptor.kt`
  - [ ] Create `/app/src/main/java/org/jellyfin/androidtv/di/NetworkModule.kt`
  - [ ] Configure OkHttp clients for both APIs
  - [ ] Configure Retrofit instances
  - [ ] Set up authentication interceptors
  - [ ] Add logging interceptor for debugging

- [ ] **Step 2.3: API Credentials Setup**
  - [ ] Create `/secrets.properties` file in project root
  - [ ] Add Trakt API credentials to secrets file
  - [ ] Add TMDB API credentials to secrets file
  - [ ] Update `/app/build.gradle.kts` to load secrets
  - [ ] Add BuildConfig fields for API keys
  - [ ] Add `/secrets.properties` to `.gitignore`

### Phase 3: Repository Layer
- [ ] **Step 3.1: Create Repository Interfaces**
  - [ ] Create `/app/src/main/java/org/jellyfin/androidtv/domain/repository/TraktRepository.kt`
  - [ ] Create `/app/src/main/java/org/jellyfin/androidtv/domain/repository/TMDBRepository.kt`
  - [ ] Define trending movies paged method
  - [ ] Define popular movies paged method
  - [ ] Define movie images retrieval method
  - [ ] Define image URL construction method

- [ ] **Step 3.2: Implement Repositories**
  - [ ] Create `/app/src/main/java/org/jellyfin/androidtv/data/repository/TraktRepositoryImpl.kt`
  - [ ] Create `/app/src/main/java/org/jellyfin/androidtv/data/repository/TMDBRepositoryImpl.kt`
  - [ ] Implement caching strategy with Room database
  - [ ] Implement pagination with proper page size (20) and prefetch distance (8)
  - [ ] Add error handling and offline support
  - [ ] Create extension functions for data mapping

- [ ] **Step 3.3: Create Paging Implementation**
  - [ ] Create `/app/src/main/java/org/jellyfin/androidtv/data/network/paging/TraktRemoteMediator.kt`
  - [ ] Implement RemoteMediator for trending movies
  - [ ] Implement RemoteMediator for popular movies
  - [ ] Handle REFRESH, APPEND, and PREPEND load types
  - [ ] Add image fetching logic in background
  - [ ] Handle API errors and empty responses

### Phase 4: UI Integration
- [ ] **Step 4.1: Create Data Adapters**
  - [ ] Create `/app/src/main/java/org/jellyfin/androidtv/presentation/home/adapter/TraktMovieAdapter.kt`
  - [ ] Implement PagingDataAdapter for efficient scrolling
  - [ ] Handle click events for navigation to details
  - [ ] Add loading and error states
  - [ ] Integrate with existing card layouts

- [ ] **Step 4.2: Create Image Helper**
  - [ ] Create `/app/src/main/java/org/jellyfin/androidtv/util/TraktImageHelper.kt`
  - [ ] Implement poster URL construction
  - [ ] Implement backdrop URL construction
  - [ ] Add image size optimization logic
  - [ ] Integrate with Coil image loading

- [ ] **Step 4.3: Update Home Fragment**
  - [ ] Create `/app/src/main/java/org/jellyfin/androidtv/presentation/home/TraktHomeFragment.kt`
  - [ ] Create `/app/src/main/java/org/jellyfin/androidtv/presentation/home/TraktHomeViewModel.kt`
  - [ ] Set up RowsSupportFragment integration
  - [ ] Add trending movies row
  - [ ] Add popular movies row
  - [ ] Handle movie selection navigation to details
  - [ ] Create simple layout file

- [ ] **Step 4.4: Create Movie Card Presenter**
  - [ ] Create `/app/src/main/java/org/jellyfin/androidtv/presentation/home/adapter/MovieCardPresenter.kt`
  - [ ] Implement Leanback ImageCardView binding
  - [ ] Set proper card dimensions (185x278)
  - [ ] Add poster image loading with placeholders
  - [ ] Add title and year display

### Phase 5: Dependency Injection Updates
- [ ] **Step 5.1: Update DI Modules**
  - [ ] Update `/app/src/main/java/org/jellyfin/androidtv/di/AppModule.kt`
  - [ ] Add database instance to DI
  - [ ] Add DAO instances to DI
  - [ ] Add repository implementations to DI
  - [ ] Add image helper to DI
  - [ ] Include NetworkModule in DI setup

### Phase 6: Testing & Validation
- [ ] **Step 6.1: Unit Tests**
  - [ ] **API Service Tests**
    - [ ] Create `TraktApiServiceTest.kt`
    - [ ] Create `TMDBApiServiceTest.kt`
    - [ ] Test API endpoints with MockWebServer
    - [ ] Test authentication headers
    - [ ] Test error handling
  - [ ] **Database Tests**
    - [ ] Create `TraktDatabaseTest.kt`
    - [ ] Test CRUD operations
    - [ ] Test pagination queries
    - [ ] Test stale data cleanup
    - [ ] Test type converters
  - [ ] **Repository Tests**
    - [ ] Create `TraktRepositoryImplTest.kt`
    - [ ] Create `TMDBRepositoryImplTest.kt`
    - [ ] Test caching logic
    - [ ] Test data mapping
    - [ ] Test error scenarios
  - [ ] **Paging Tests**
    - [ ] Create `TraktRemoteMediatorTest.kt`
    - [ ] Test REFRESH load type
    - [ ] Test APPEND load type
    - [ ] Test error handling
    - [ ] Test empty response handling
  - [ ] **ViewModel Tests**
    - [ ] Create `TraktHomeViewModelTest.kt`
    - [ ] Test Flow exposures
    - [ ] Test data transformation

- [ ] **Step 6.2: Integration Tests**
  - [ ] **API Integration**
    - [ ] Create `TraktApiIntegrationTest.kt`
    - [ ] Test real API calls with actual credentials
    - [ ] Verify response data structure
    - [ ] Test rate limiting behavior
  - [ ] **Database Integration**
    - [ ] Create `DatabaseIntegrationTest.kt`
    - [ ] Test full workflow (API → Database → UI)
    - [ ] Test pagination with large datasets
    - [ ] Test cross-table relationships
  - [ ] **UI Integration**
    - [ ] Create `TraktHomeFragmentTest.kt`
    - [ ] Test fragment display
    - [ ] Test navigation clicks
    - [ ] Test row loading states

- [ ] **Step 6.3: Test Configuration**
  - [ ] Add testing dependencies to `build.gradle.kts`
  - [ ] Update `libs.versions.toml` with test library versions
  - [ ] Configure test runners
  - [ ] Set up CI-friendly test execution

### Phase 7: Remove Jellyfin UI Components
- [ ] **Step 7.1: Remove Startup/Welcome Screens**
  - [ ] **Identify Startup Components**
    - [ ] Run search commands to find welcome/startup files
    - [ ] List all files to modify or remove
    - [ ] Identify server connection logic locations
  - [ ] **Remove Server Connection Logic**
    - [ ] Update `MainActivity.kt` to skip server checks
    - [ ] Remove startup activity references from `AndroidManifest.xml`
    - [ ] Create stub session manager for UI compatibility
    - [ ] Update DI to use stub session instead of real one

- [ ] **Step 7.2: Create Direct Home Navigation**
  - [ ] **Update App Launch Flow**
    - [ ] Modify `MainActivity.onCreate()` for direct home navigation
    - [ ] Remove server selection screens
    - [ ] Remove authentication screens
    - [ ] Remove connection status checks
  - [ ] **Create New Home Structure**
    - [ ] Create `TraktHomeFragment.kt` with two movie rows
    - [ ] Create simple layout `fragment_trakt_home.xml`
    - [ ] Implement row setup with trending and popular movies
    - [ ] Add click navigation to movie details

- [ ] **Step 7.3: Future Navigation Structure**
  - [ ] Document planned app structure (Home/Movies/TV Shows)
  - [ ] Plan navigation between sections
  - [ ] Design details page integration

- [ ] **Step 7.4: Remove Server-Related Strings**
  - [ ] Remove Jellyfin-specific strings from `strings.xml`
  - [ ] Add new movie-specific strings
  - [ ] Update app name strings for new identity

### Phase 8: Complete App Transformation
- [ ] **Step 8.1: Update Application Identity**
  - [ ] Change namespace in `build.gradle.kts`
  - [ ] Update application ID
  - [ ] Update app name in `strings.xml`
  - [ ] Use Android Studio refactor to rename packages

- [ ] **Step 8.2: Remove Jellyfin Dependencies**
  - [ ] Remove Jellyfin SDK from `libs.versions.toml`
  - [ ] Remove Jellyfin dependencies from `build.gradle.kts`
  - [ ] Clean up unused imports and references

- [ ] **Step 8.3: Clean Module Structure**
  - [ ] Remove unnecessary playback modules
  - [ ] Update `settings.gradle.kts` with new module structure
  - [ ] Keep only app and preference modules
  - [ ] Clean up project references

### Phase 9: Final Testing & Deployment
- [ ] **Step 9.1: End-to-End Testing**
  - [ ] Test complete app flow from launch to details
  - [ ] Verify all Jellyfin references removed
  - [ ] Test pagination with real data
  - [ ] Test offline behavior
  - [ ] Test error scenarios and recovery

- [ ] **Step 9.2: Performance Testing**
  - [ ] Test app startup time
  - [ ] Test scrolling performance with large datasets
  - [ ] Test memory usage during heavy scrolling
  - [ ] Test image loading performance
  - [ ] Profile database query performance

- [ ] **Step 9.3: Final Validation**
  - [ ] Run all unit tests and verify 100% pass
  - [ ] Run all integration tests
  - [ ] Test on different screen sizes and densities
  - [ ] Test on different Android versions (API 21+)
  - [ ] Verify no Jellyfin branding or references remain

- [ ] **Step 9.4: Deployment Preparation**
  - [ ] Generate signed APK for testing
  - [ ] Test installation and upgrade scenarios
  - [ ] Verify all API keys work in release build
  - [ ] Test ProGuard/R8 compatibility
  - [ ] Prepare release notes and documentation

## Progress Tracking

**Overall Progress: 0/9 Phases Complete**

- [ ] Phase 1: Foundation Setup (0/3 steps)
- [ ] Phase 2: API Layer (0/3 steps)
- [ ] Phase 3: Repository Layer (0/3 steps)
- [ ] Phase 4: UI Integration (0/4 steps)
- [ ] Phase 5: Dependency Injection (0/1 steps)
- [ ] Phase 6: Testing & Validation (0/3 steps)
- [ ] Phase 7: Remove Jellyfin UI (0/4 steps)
- [ ] Phase 8: App Transformation (0/3 steps)
- [ ] Phase 9: Final Testing (0/4 steps)

## Implementation Timeline

1. **Week 1**: Phase 1-2 (Foundation + API Layer)
2. **Week 2**: Phase 3-4 (Repository + UI Integration)
3. **Week 3**: Phase 5-6 (DI Updates + Testing)
4. **Week 4**: Phase 7-8 (Remove Jellyfin + Transform App)
5. **Week 5**: Phase 9 (Final Testing + Deployment)

## Technical Considerations

### API Limitations & Best Practices

1. **Rate Limits**: 
   - Trakt: 1,000 requests per 5 minutes per IP
   - TMDB: 40 requests per 10 seconds per IP
   - **Solution**: Use standard HTTP caching headers and implement exponential backoff

2. **Image Optimization**:
   - TMDB provides multiple image sizes (w185, w342, w500, w780, original)
   - **Solution**: Use appropriate size for each UI component (w342 for cards, w780 for details)

3. **Network Resilience**:
   - **Solution**: Use Retrofit's built-in error handling + Room caching for offline support

4. **Performance**:
   - **Solution**: Leverage Paging 3's built-in loading states and error handling
   - Use Coil's memory and disk caching for images

### App Functionality Scope

**Current Phase (Movies Only)**:
- Browse trending and popular movies
- View movie details (title, year, overview, rating, etc.)
- Smooth infinite scrolling with pagination
- Offline browsing via Room cache

**Future Expansion Possibilities**:
- TV Shows support
- Search functionality 
- User authentication and watchlists
- Movie trailers integration
- Streaming service availability

## Security Notes

- Never commit `secrets.properties` file
- Use environment variables in CI/CD for API keys
- Consider using encrypted SharedPreferences for sensitive data
- Implement certificate pinning for API calls

## Next Steps

1. Review and approve this plan
2. Set up the development environment with API keys
3. Create a new branch for the Trakt migration
4. Start with Phase 1 implementation
5. Test each phase thoroughly before proceeding to the next

This plan maintains the existing UI/UX while completely replacing the data source, ensuring a seamless transition that preserves the user experience users are familiar with.