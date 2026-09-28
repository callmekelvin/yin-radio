# Android Learning Notes

This guide documents the fundamental concepts, architecture patterns, and implementation details used in the **Yin Radio** Android application.

---

## Table of Contents

- [1. Introduction to Modern Android Development](#1-introduction-to-modern-android-development)
- [2. The Android Build System](#2-the-android-build-system)
- [3. Dependency Injection](#3-dependency-injection)
- [4. App Architecture](#4-app-architecture)
- [5. UI with Jetpack Compose](#5-ui-with-jetpack-compose)
- [6. Kotlin Coroutines & Flows](#6-kotlin-coroutines--flows)
- [7. Data Persistence](#7-data-persistence)
- [8. Networking](#8-networking)
- [9. Media Playback](#9-media-playback)
- [10. Common Pitfalls & Compatibility](#10-common-pitfalls--compatibility)
- [11. Appendix](#11-appendix)

---

## 1. Introduction to Modern Android Development

- **Kotlin-First**: Kotlin is the preferred language. It is fully interoperable with Java but offers null-safety, coroutines, and more expressive syntax.
- **Jetpack Compose**: The modern, declarative UI toolkit. Instead of manipulating XML layouts and view references imperatively, you describe your UI as a function of state using Composable functions.
- **Architecture Components**: `ViewModel`, `Flow`, `Room`, and `Navigation` are standard tools for building robust, lifecycle-aware applications.
- **Kotlin Coroutines & Flow**: The standard for asynchronous and reactive programming, replacing callbacks and RxJava in many modern codebases.

For the **Yin Radio** app, the stack is:
- **UI**: Jetpack Compose + Material Design 3
- **Architecture**: MVVM with Repository pattern
- **DI**: Koin
- **Networking**: Ktor Client + Kotlinx Serialization
- **Persistence**: Room (SQLite) + DataStore (Preferences)
- **Playback**: Media3 (ExoPlayer) in a Foreground Service

---

## 2. The Android Build System

### 2.1 Gradle, AGP, and Kotlin

- **Gradle**: The underlying build automation tool. It manages tasks like compiling code, running tests, and packaging the APK.
- **AGP (Android Gradle Plugin)**: A Gradle plugin that adds Android-specific tasks. It bridges the gap between standard Java/Kotlin compilation and the Android SDK (handling resources, manifest merging, DEX generation, etc.).
- **Kotlin**: The programming language. The `kotlin("android")` or `alias(libs.plugins.kotlin.compose)` plugin tells Gradle how to compile Kotlin source files.

### 2.2 KSP (Kotlin Symbol Processing)

KSP is the modern replacement for **KAPT** (Kotlin Annotation Processing Tool). It is used by libraries like **Room** to generate boilerplate code at compile time.

- **Faster than KAPT**: KSP has a lighter API and avoids generating Java stubs, leading to faster build times.
- **Required for Room**: In the Yin Radio app, Room uses KSP to generate implementations of `AppDatabase`, `StationDao`, and other DAOs.

### 2.3 Project Configuration

The build configuration is split across several files:

**`settings.gradle.kts`** (Project root)
- Defines plugin repositories and dependency resolution management.
- Includes subprojects (e.g., `:app`).

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "Yin Radio"
include(":app")
```

**`app/build.gradle.kts`** (Module level)
- Applies plugins (`android.application`, `kotlin.compose`, `ksp`).
- Defines `namespace`, `compileSdk`, `minSdk`, `targetSdk`.
- Configures `buildFeatures` like `compose = true`.
- Declares dependencies.

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.yin_radio.yin_radio_android_app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.yin_radio.yin_radio_android_app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}
```

### 2.4 Version Catalogs

Instead of hardcoding version numbers in `build.gradle.kts`, the project uses a **Version Catalog** (`gradle/libs.versions.toml`). This acts like a centralized `package.json` or `requirements.txt`.

```toml
[versions]
agp = "9.4.0"
kotlin = "2.2.10"
room = "2.8.0"
ktor = "3.1.2"

[libraries]
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
ktor-client-android = { group = "io.ktor", name = "ktor-client-android", version.ref = "ktor" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

In `app/build.gradle.kts`, dependencies are referenced cleanly:
```kotlin
dependencies {
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.ktor.client.android)
}
```

### 2.5 Build Pipeline Diagram

```text
+-------------+     +----------------+     +------------------+
|   Source    |     |    Gradle      |     |   AGP Plugin     |
|  (.kt, .xml)| --> |   (Tasks)      | --> | (Android Build)  |
+-------------+     +----------------+     +------------------+
                                                  |
                                                  v
+-------------+     +----------------+     +------------------+
|     APK     | <-- |    DEX / AAPT  | <-- |   KSP / KAPT     |
|   Output    |     |  (Packaging)   |     | (Code Gen)       |
+-------------+     +----------------+     +------------------+
```

---

## 3. Dependency Injection

### 3.1 Why DI is Essential in Android

In Android, the framework instantiates core components like `Activity`, `Service`, and `BroadcastReceiver`. You do not control their constructors. This makes manual dependency construction difficult.

For example, `MainActivity` is created by the system. If it needs a `StationRepository`, you cannot simply pass it via a constructor. DI frameworks solve this by maintaining a registry of dependencies and injecting them into fields or constructors at runtime.

### 3.2 Koin vs. Dagger / Hilt

| Feature | Koin (Used in Yin Radio) | Dagger / Hilt (Google Standard) |
|---------|--------------------------|--------------------------------|
| **Type** | Service Locator pattern | Pure Dependency Injection |
| **Validation** | Runtime resolution | Compile-time validation |
| **Boilerplate** | Minimal (DSL-based) | Moderate (Annotations + Modules) |
| **Learning Curve** | Low | Steeper |
| **Best For** | Compose-first, smaller projects | Large, complex enterprise apps |

**Koin** is chosen for Yin Radio because of its simplicity and excellent Jetpack Compose integration.

### 3.3 Project Example: Koin Modules

Initalize Koin within Android Application Class (Ex. YinRadioApplication.kt)

```
class YinRadioApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            // Log Koin into Android logger
            androidLogger();

            // Reference Android context
            androidContext(this@YinRadioApplication)

            // Load modules
            modules(appModule)
        }
    }
}
```

All dependencies are declared in `di/AppModule.kt` using a Koltin Plugin DSL.
- Definitions for Dependency Injection Types (Singleton, Factory, Scoped, ViewModel): https://insert-koin.io/docs/reference/koin-core/definitions/

```kotlin
import org.koin.plugin.module.dsl.*
import org.koin.dsl.module
import org.koin.dsl.bind

fun provideDatabase(app: Application): AppDatabase =
    Room.databaseBuilder(app, AppDatabase::class.java, "yin_radio.db").build()
fun provideStationDao(db: AppDatabase) = db.stationDao()
fun provideTagDao(db: AppDatabase) = db.tagDao()
fun provideSyncMetadataDao(db: AppDatabase) = db.syncMetadataDao()

val appModule = module {
    // Singleton: Room Database
    single { create(::provideDatabase) }

    // Singleton: DAOs retrieved from the database instance
    single { create(::provideStationDao) }
    single { create(::provideTagDao) }
    single { create(::provideSyncMetadataDao) }

    // Compiler plugin auto-wires constructor parameters from the graph
    single<SettingsDataStore>()

    // Singleton: API Implementation
    single<IStationsApi>() bind StationsApi::class

    // Singleton: Repositories
    single<StationRepository>()
    single<FavoritesRepository>()

    // ViewModels (scoped to the Activity/Fragment lifecycle)
    viewModel<OnboardingViewModel>()
    viewModel<MainViewModel>()
    viewModel<HomeViewModel>()
    viewModel<DiscoverViewModel>()
    viewModel<FavoritesViewModel>()
    viewModel<PlayerViewModel>()
    viewModel<SettingsViewModel>()
}
```

In `MainActivity.kt`, dependencies are injected using the `by inject()` delegate:
```kotlin
class MainActivity : ComponentActivity() {
    private val settingsDataStore: SettingsDataStore by inject()
    private val stationRepository: StationRepository by inject()
    // ...
}
```

---

## 4. App Architecture

### 4.1 MVVM + Repository Pattern

The Yin Radio app follows a **Clean Architecture** approach with MVVM (Model-View-ViewModel).

- **Model**: Data layer. Includes `StationEntity` (Room), `StationDto` (Remote), and `Station` (Domain).
- **ViewModel**: Holds UI state and business logic. Survives configuration changes (e.g., screen rotation).
- **View**: Jetpack Compose UI. Observes the `ViewModel` and recomposes when state changes.
- **Repository**: Acts as a single source of truth, abstracting whether data comes from the network or local cache.

### 4.2 Unidirectional Data Flow

```text
+----------------+     +----------------+     +----------------+
|   UI Layer     | --> |   ViewModel    | --> |  Repository    |
|  (Compose)     |     |   (StateFlow)  |     | (Data Source)  |
+----------------+     +----------------+     +----------------+
        ^                                              |
        |                                              v
        |                                     +----------------+
        |                                     |  Room / Ktor   |
        |                                     +----------------+
        +----------------------------------------------+
                            (State Updates)
```

### 4.3 Project Example: Wiring it Together

**`StationRepository.kt`** coordinates between the remote API and local database:
```kotlin
class StationRepository(
    private val api: StationsApi,
    private val stationDao: StationDao,
    private val tagDao: TagDao,
    private val syncMetadataDao: SyncMetadataDao
) {
    suspend fun syncStations(): Result<Unit> {
        return try {
            val manifest = api.fetchManifest()
            val allStations = mutableListOf<StationEntity>()

            for (pageInfo in manifest.pages) {
                val page = api.fetchPage(pageInfo.page)
                allStations.addAll(page.map { it.toEntity() })
            }

            // Atomic write to local database
            stationDao.clearAll()
            tagDao.clearAll()
            stationDao.insertAll(allStations)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getFavorites(): Flow<List<Station>> {
        return stationDao.getFavorites().map { list -> list.map { it.toDomain() } }
    }
}
```

**`MainActivity.kt`** observes the ViewModel and UI state:
```kotlin
class MainActivity : ComponentActivity() {
    private val settingsDataStore: SettingsDataStore by inject()
    private val stationRepository: StationRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsDataStore.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            var hasData by remember { mutableStateOf<Boolean?>(null) }

            LaunchedEffect(Unit) {
                hasData = stationRepository.hasData()
            }

            YinRadioTheme(themeMode = themeMode) {
                when (hasData) {
                    null -> { /* Loading */ }
                    false -> OnboardingScreen(onComplete = { hasData = true })
                    true -> MainScreen()
                }
            }
        }
    }
}
```

---

## 5. UI with Jetpack Compose

### 5.1 The Declarative Paradigm

Traditional Android UI used XML layouts and imperative code (`findViewById`, `setText`). Compose flips this: you write **Composable functions** that describe the UI based on the current state.

If the state changes, Compose automatically recomposes (re-draws) only the affected parts.

---

### 5.2 State Management

- **`remember`**: Retains state across recompositions.
- **`mutableStateOf`**: Creates an observable state variable.
- **`collectAsState()`**: Converts a Kotlin `Flow` into a Compose state.

```kotlin
@Composable
fun Counter() {
    var count by remember { mutableStateOf(0) }
    Button(onClick = { count++ }) {
        Text("Clicked $count times")
    }
}
```

---

### 5.3 UI Lifecycle, Side Effects and Phases

#### Lifecycle of Composables

https://developer.android.com/develop/ui/compose/lifecycle

A **Composition** is a tree/ graph-structure of composables (components) that describes your UI. The lifecycle of a composable is simpler than that of Views, Activities, or Fragments. It consists of three key events:

1. **Enter the Composition** — When Jetpack Compose first runs your composables during initial composition.
2. **Recomposition** — Triggered when a `State<T>` object it reads changes. Compose re-runs the composable function (and any non-skippable children) to update the UI. This happens **0 or more times**.
3. **Leave the Composition** — When the composable is no longer called and is removed from the UI tree.

Each instance of a composable in the Composition is identified by its **call site** (the source code location where it is called). Calling the same composable from different call sites creates multiple distinct instances in the tree.

> **Key Point:** Compose avoids recomposing composables whose inputs haven't changed. This is why passing stable keys and minimizing unnecessary state reads is critical for performance.

---

#### Jetpack Compose Phases of a Frame

For every frame, Compose transforms/ renders data into UI through three main phases. It intelligently skips any phase whose inputs haven't changed:

| Phase | What happens |
|-------|--------------|
| **1. Composition** | Determines **what** UI to show. Compose runs composable functions and builds the UI tree (layout nodes). |
| **2. Layout** | Determines **where** to place UI. Consists of **measurement** (how big) and **placement** (where in 2D coordinates). |
| **3. Drawing** | Determines **how** it renders. UI elements draw into a `Canvas` (usually the device screen). |

Because Compose tracks which state is read within each phase, it can target updates precisely:
- A state read inside a `@Composable` function affects **composition** (and potentially layout/drawing).
- A state read inside a `Modifier.offset { ... }` or placement block affects **layout** (and potentially drawing).
- A state read inside a draw modifier affects **drawing** only.

> **Performance Tip:** Defer state reads to the latest possible phase. For example, passing a lambda to a modifier instead of the resolved state value can skip composition entirely and only trigger a re-draw.

--- 

#### Side Effects in Compose

Composables should ideally be **side-effect free**. However, when you need to interact with the outside world (e.g., launch a coroutine, subscribe to a listener, or update non-Compose code), you must use an **Effect API** so the work runs in a lifecycle-aware, predictable manner.

| API | When to use |
|-----|-------------|
| **`LaunchedEffect`** | One-shot suspend work tied to the composable's lifecycle. Runs on enter, **cancelled** on leave. |
| **`rememberCoroutineScope`** | Launching coroutines from event handlers (e.g., `onClick`) outside the normal composition flow. |
| **`DisposableEffect`** | Side effects that require **cleanup** (subscribe/unsubscribe, register/unregister). |
| **`SideEffect`** | Syncing final committed Compose state to non-Compose code **after every successful recomposition**. |

> **Warning:** Effects can be easily overused. Keep the work UI-related and ensure you do not break **unidirectional data flow** by sneaking business logic into composables.

**Example: Loading data on app startup**
```kotlin
// Lifecycle Event (Side Effect - Launches coroutine to load data in on app startup)
LaunchedEffect(Unit) {
    hasData = stationRepository.hasData()
}
```

---

### 5.4 State Within Compositions

#### State Lifespans

State in Compose can live for different lengths of time depending on which memoization API you use. Choose the right tool for how long the state must survive:

| Function | Survives Recomposition | Survives Config Change | Survives Process Death | Best For |
|---|---|---|---|---|
| `remember` | Yes | No | No | Caches, animation state, scroll position |
| `retain` | Yes | Yes | No | Long-lived manager objects (avoids serialization cost) |
| `rememberSaveable` | Yes | Yes | Yes | User input, toggles, scroll state (requires custom `Saver` if not `Bundle`-able) |
| `rememberSerializable` | Yes | Yes | Yes | User input for types marked with `@Serializable` |

- **Avoid** storing user input with plain `remember` — it is lost on configuration changes and process death.
- Use **`retain`** when you need to survive configuration changes but do not need to survive process death, and you want to avoid the overhead of serialization.
- Use **`rememberSaveable`** or **`rememberSerializable`** for any state that must survive both configuration changes and system-initiated process death.

#### State Callbacks

For complex objects whose lifecycle needs to be managed explicitly, Compose provides observer interfaces that receive callbacks when the object enters or leaves the Composition (or is retained).

- **`RememberObserver`** — Implement on objects stored with `remember`.
  - `onRemembered()` — called when the object enters the Composition.
  - `onForgotten()` — called when the composable leaves the Composition.
  - `onAbandoned()` — called if the composition was aborted before `onRemembered()`.
- **`RetainObserver`** — Implement on objects stored with `retain`.
  - Receives callbacks when the retained value is remembered or forgotten across configuration changes.

**When to use:** These are useful when the object itself should manage its own setup/teardown (e.g., opening/closing a resource handle), rather than wrapping every interaction in `DisposableEffect`.

---

### 5.5 Theming with Material Design 3

The app uses **Material Design 3** with custom color schemes for Light and Dark modes.

Wrap your application around the Material Theme Composable Function: https://developer.android.com/develop/ui/compose/designsystems/material3#material-theming

**`ui/theme/Theme.kt`**:
```kotlin
@Composable
fun YinRadioTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

```kotlin
YinRadioTheme(themeMode = themeMode) {
    when (hasData) {
        null -> { /* Loading state, blank */ }
        false -> OnboardingScreen(onComplete = { hasData = true })
        true -> MainScreen()
    }
}
```

### 5.6 Project Example: MainActivity & Theme

`MainActivity` ties together theme observation and navigation logic:
```kotlin
class MainActivity : ComponentActivity() {
    // ... (see Section 4.3)
}
```

---

## 6. Kotlin Coroutines & Flows

Kotlin Coroutines and Flow are the backbone of asynchronous and reactive programming in the Yin Radio app. They replace callback-heavy code with sequential, readable syntax and provide a robust **Publisher/Subscriber (Pub-Sub)** mechanism for delivering data updates that automatically drive Jetpack Compose UI recomposition.

---

### 6.1 Kotlin Coroutines Fundamentals

Coroutines are lightweight threads that allow you to write asynchronous code in a sequential style.

- **`suspend` functions**: Can pause execution without blocking the underlying thread. They can only be called from another suspend function or a coroutine.
- **`CoroutineScope`**: Defines the lifecycle of coroutines. When the scope is cancelled, all coroutines launched within it are cancelled.
- **`Dispatchers`**: Determine which thread pool a coroutine runs on.
  - **`Dispatchers.Main`**: The UI thread. Use for UI updates and short, non-blocking work.
  - **`Dispatchers.IO`**: Optimized for disk and network I/O (e.g., database queries, API calls).
  - **`Dispatchers.Default`**: Optimized for CPU-intensive work (e.g., sorting lists, image processing).

#### Coroutine Builders

| Builder | Purpose |
|---------|---------|
| **`launch`** | Fires and forgets. Starts a new coroutine and returns a `Job`. Used when you don't need a result back. |
| **`async`** | Starts a coroutine and returns a `Deferred<T>`. Use `.await()` to get the result. Useful for parallelizing work. |
| **`withContext`** | Switches the dispatcher for a block of code and resumes on the original dispatcher. Commonly used to offload work to `IO` and return to `Main`. |

**Example: Syncing stations in a ViewModel**
```kotlin
// Starts on Main Thread (viewModelScope uses Dispatchers.Main.immediate by default)
viewModelScope.launch {

    // withContext(Dispatchers.IO) — Wwitches to an IO thread for this code block, so stationRepository.syncStations() runs on IO.
    val result = withContext(Dispatchers.IO) {
        stationRepository.syncStations()
    }

    // Execution resumes on Main Thread automatically after return
    if (result.isSuccess) {
        _uiState.value = UiState.Success
    } else {
        _uiState.value = UiState.Error(result.exceptionOrNull()?.message)
    }
}
```

---

### 6.2 Kotlin Flow Fundamentals

While coroutines handle one-shot asynchronous tasks, **Flow** handles streams of data over time.

- **`Flow<T>`**: A cold stream. It does not emit values until a collector is attached. Each collector gets its own independent stream.
- **`StateFlow<T>`**: A hot stream that always holds a current value. It is stateful and ideal for representing UI state. New subscribers immediately receive the latest value.
- **`SharedFlow<T>`**: A hot stream that emits events to all active subscribers. Unlike `StateFlow`, it does not require an initial value and can be configured to replay a buffer of past emissions.

#### Common Operators

| Operator | Purpose |
|----------|---------|
| **`map`** | Transforms each emitted value. |
| **`filter`** | Emits only values matching a predicate. |
| **`combine`** | Merges two or more flows by combining their latest values. |
| **`flatMapLatest`** | Switches to a new inner flow when the outer flow emits. Cancels the previous inner flow. |
| **`flowOn`** | Changes the dispatcher upstream. |
| **`catch`** | Handles exceptions in the flow pipeline gracefully. |

---

### 6.3 The Pub-Sub Pattern in Android

The Yin Radio app uses a **Publisher/Subscriber** architecture built on top of Kotlin Flow to propagate data from the data layer to the UI layer.

**How it works:**
1. **Publisher (Data Layer)**: Room DAOs and DataStore expose reactive APIs that return `Flow`. Whenever the underlying data changes (e.g., a station is marked as favorite), the publisher emits a new value.
2. **Intermediary (ViewModel)**: The `ViewModel` collects from repository flows, transforms them if needed, and exposes them as `StateFlow` or `SharedFlow`. The `ViewModel` acts as a data transformer and lifecycle boundary.
3. **Subscriber (UI Layer)**: Composable functions collect from the `ViewModel`'s flows and convert them into Compose `State`. When a new value arrives, Compose triggers recomposition for the affected parts of the UI.

```text
+----------------+        +----------------+        +----------------+
|  Data Layer    |        |   ViewModel    |        |   UI Layer     |
|  (Publisher)   | -----> | (Intermediary) | -----> |  (Subscriber)  |
| Room / Ktor    |  Flow  |  StateFlow     |  Flow  |   @Composable  |
+----------------+        +----------------+        +----------------+
       ^                                                    |
       |                                                    v
       |                                            +----------------+
       |                                            | Recomposition  |
       +--------------------------------------------| (UI Updates)   |
                                                    +----------------+
```

---

### 6.4 Driving UI Recomposition with Flow

Jetpack Compose is inherently reactive. To bridge Kotlin Flow with Compose, you collect the flow inside a Composable and convert it into a `State<T>` object.

- **`collectAsState()`**: Collects a `Flow` and returns a `State<T>`. Recomposes the Composable on every emission. Does not respect the UI lifecycle directly; use with care to avoid collecting when the app is in the background.
- **`collectAsStateWithLifecycle()`**: **Recommended** for UI flows. It respects the lifecycle of the composable's owner (e.g., `Activity` or `Fragment`), automatically pauses collection when the UI is not visible and resumes when it returns, saving resources.

**Why `StateFlow` is ideal for UI state:**
- It always has a current value, so the UI never has to handle a `null` or missing initial state.
- It survives configuration changes (like screen rotation) when held inside a `ViewModel`.
- When a new value is emitted, any Composable reading the corresponding `State` object is automatically recomposed.

> **Key Point:** Compose's recomposition is triggered specifically when a `State<T>` object is read during the Composition phase. By converting a `Flow` to `State` via `collectAsState`, each emission becomes a state change that drives the UI.

---

### 6.5 Project Examples

#### Publisher: Room DAO Emitting a Flow

The `StationDao` acts as a publisher. Because Room supports coroutines, a `@Query` annotated to return `Flow` will automatically re-emit whenever the queried table changes.

```kotlin
@Dao
interface StationDao {
    @Query("SELECT * FROM stations WHERE isFavorite = 1")
    fun getFavorites(): Flow<List<StationEntity>>
}
```

The `StationRepository` maps this database-specific flow into a domain-friendly flow:

```kotlin
class StationRepository(
    private val api: StationsApi,
    private val stationDao: StationDao,
    // ...
) {
    fun getFavorites(): Flow<List<Station>> {
        // Publisher: emits new list every time the 'stations' table changes
        return stationDao.getFavorites()
            .map { list -> list.map { it.toDomain() } }
    }
}
```

#### Intermediary: ViewModel Exposing StateFlow

The `FavoritesViewModel` collects from the repository and exposes a `StateFlow`. This decouples the UI from the repository and allows the `ViewModel` to manage loading, error, and success states.

```kotlin
class FavoritesViewModel(
    private val stationRepository: StationRepository
) : ViewModel() {

    // Backing property
    private val _favorites = MutableStateFlow<List<Station>>(emptyList())
    // Public read-only StateFlow for the UI to collect
    val favorites: StateFlow<List<Station>> = _favorites.asStateFlow()

    init {
        // Subscribe to the publisher (Repository)
        viewModelScope.launch {
            stationRepository.getFavorites()
                .flowOn(Dispatchers.IO)
                .catch { e ->
                    // Handle error gracefully
                    _favorites.value = emptyList()
                }
                .collect { stations ->
                    // Emit new state -> triggers subscriber recomposition
                    _favorites.value = stations
                }
        }
    }

    fun toggleFavorite(stationUuid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            stationRepository.toggleFavorite(stationUuid)
        }
    }
}
```

#### Subscriber: Compose UI Collecting Flow

The `FavoritesScreen` subscribes to the `ViewModel`'s `StateFlow`. It uses `collectAsStateWithLifecycle` to ensure collection respects the screen's lifecycle.

```kotlin
@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel = koinViewModel()
) {
    // Subscriber: converts Flow into Compose State
    val favoriteStations by viewModel.favorites.collectAsStateWithLifecycle()

    LazyColumn {
        items(favoriteStations, key = { it.stationuuid }) { station ->
            StationCard(
                station = station,
                onFavoriteToggle = { viewModel.toggleFavorite(station.stationuuid) }
            )
        }
    }
}
```

**What happens when the user toggles a favorite?**
1. `onFavoriteToggle` triggers `viewModel.toggleFavorite()`.
2. The `ViewModel` launches a coroutine on `Dispatchers.IO` to update the database via the `Repository`.
3. Room detects the table change and re-emits a new list from `stationDao.getFavorites()`.
4. The `FavoritesViewModel` collects the new list and updates `_favorites.value`.
5. The Compose `State` wrapping `_favorites` changes.
6. Compose identifies that `FavoritesScreen` reads this state and **recomposes** it, updating the list automatically without manual view manipulation.

---

## 7. Data Persistence

### 7.1 Room (SQLite Abstraction)

**Room** is an abstraction layer over SQLite. It allows you to work with a relational database using Kotlin objects and annotations instead of writing raw SQL and manual cursor management.

Room is built around three core components that work together:

- **Data Entity**: Think of this as your **table schema**. It is a Kotlin data class annotated with `@Entity` where each property maps to a database column. Room uses this to generate the `CREATE TABLE` SQL.
- **Data Access Object (DAO)**: Think of this as your **query interface**. It is a Kotlin interface annotated with `@Dao` where you define SQL operations (`@Query`, `@Insert`, `@Update`, `@Delete`). Room generates the implementation at compile time.
- **Database**: An abstract class extending `RoomDatabase` that links all entities and DAOs into a single database instance.

Database Inspector: https://developer.android.com/studio/inspect/database
- View -> Tool Windows -> App Inspection -> Data Inspector Tab

#### Gradle Setup

Add the Room dependencies to your Version Catalog (`gradle/libs.versions.toml`):

```toml
[versions]
room = "2.8.0"

[libraries]
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
```

Then reference them in `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
}
```

- `room-runtime`: Core Room library for database operations.
- `room-ktx`: Kotlin extensions that add coroutines and `Flow` support to Room (e.g., suspend functions and `Flow<T>` return types in DAOs).
- `room-compiler`: The annotation processor (used via KSP) that generates the boilerplate implementation for entities, DAOs, and the database class.

#### Data Entity (Table Schema)

A **Data Entity** is like a **table schema**. It defines the structure of a table: the columns, their types, primary keys, and constraints.

```kotlin
@Entity(tableName = "stations")
data class StationEntity(
    @PrimaryKey val stationuuid: String,
    val name: String,
    val urlResolved: String,
    val favicon: String?,
    val tags: String,
    val country: String,
    val countrycode: String,
    val bitrate: Int,
    val codec: String,
    val votes: Int,
    val language: String,
    val languagecodes: String,
    val hls: Int,
    val geoLat: Double?,
    val geoLong: Double?,
    val isFavorite: Boolean = false
)
```

Key annotations:
- `@Entity(tableName = "stations")`: Declares this class as a database table with the given name.
- `@PrimaryKey`: Marks the property as the primary key for the table.
- `@ColumnInfo(name = "...")`: Optional. Explicitly sets the column name (defaults to the property name).

#### Data Access Object (Query Interface)

A **DAO** is like a **custom SQL query interface** that hooks into your table schema. It defines the operations you can perform on the table. Room generates the actual implementation at compile time.

```kotlin
@Dao
interface StationDao {
    @Query("SELECT * FROM stations WHERE isFavorite = 1")
    fun getFavorites(): Flow<List<StationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stations: List<StationEntity>)

    @Query("DELETE FROM stations")
    suspend fun clearAll()

    @Query("UPDATE stations SET isFavorite = :isFavorite WHERE stationuuid = :stationUuid")
    suspend fun updateFavorite(stationUuid: String, isFavorite: Boolean)
}
```

Key annotations:
- `@Dao`: Marks the interface as a Data Access Object.
- `@Query`: Runs a raw SQL query. Room validates SQL at compile time.
- `@Insert`: Inserts one or more entities. `onConflict` defines how to handle duplicates.
- `@Update` / `@Delete`: Convenience annotations for updating or deleting entities.
- `suspend` functions: Run database operations on a background thread (requires `room-ktx`).
- `Flow<T>` return type: Emits a new value automatically whenever the underlying table changes (reactive queries).

#### Database Class

The **Database** class is the glue that ties entities and DAOs together. It is an abstract class extending `RoomDatabase`, and Room generates the full implementation.

```kotlin
@Database(
    entities = [StationEntity::class, TagEntity::class, SyncMetadataEntity::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao
    abstract fun tagDao(): TagDao
    abstract fun syncMetadataDao(): SyncMetadataDao
}
```

Key points:
- `@Database`: Registers all entities and sets the schema version.
- `version`: Increment this when you change the schema and provide a migration.
- Abstract methods: Each returns a DAO instance. Room generates the implementation.

#### How They Interact

```text
+----------------+     +------------------+     +------------------+
|  Data Entity   |     |       DAO        |     |    Database      |
| (Table Schema) |     | (Query Interface)|     |   (Glue Layer)   |
|   @Entity      |     |     @Dao         |     |   @Database      |
| StationEntity  |<--->|  StationDao      |<--->|  AppDatabase     |
|  - stationuuid |     | - getFavorites() |     | - stationDao()   |
|  - name        |     | - insertAll()    |     | - tagDao()       |
|  - isFavorite  |     | - clearAll()     |     | - syncMetadata() |
+----------------+     +------------------+     +------------------+
         ^                                               |
         |                                               v
         |                                      +------------------+
         |                                      |   Repository     |
         |                                      | StationRepository|
         |                                      | - syncStations() |
         |                                      | - getFavorites() |
         |                                      +------------------+
         |                                               |
         |                                               v
         |                                      +------------------+
         +--------------------------------------|    ViewModel     |
                                                |FavoritesViewModel|
                                                |  - StateFlow     |
                                                +------------------+
                                                         |
                                                         v
                                                +------------------+
                                                |    UI Layer      |
                                                |   (Compose)      |
                                                +------------------+
```

**The flow:**
1. **Entity** (`StationEntity`) defines the table structure.
2. **DAO** (`StationDao`) defines the SQL operations on that table.
3. **Database** (`AppDatabase`) registers entities and exposes DAO instances.
4. **Repository** uses the DAO to read/write data and maps entities to domain models.
5. **ViewModel** collects reactive `Flow` from the repository and exposes it as `StateFlow`.
6. **UI Layer** observes the `StateFlow` and recomposes automatically when data changes.

> **Key Point:** Because `StationDao.getFavorites()` returns a `Flow`, Room acts as a **publisher**. Whenever the `stations` table changes, Room emits a new list, which propagates up through Repository -> ViewModel -> UI, triggering recomposition automatically.

---

### 7.2 Jetbrains DataStore (Modern SharedPreferences)

**DataStore** replaces `SharedPreferences`. 

DataStore is a data storage solution that lets you store key-value pairs or typed objects with protocol buffers
- **Type-safe**: Uses Kotlin types and `PreferencesKeys`.
- **Storing data asynchronously**: Uses Kotlin Coroutines and `Flow`.
- **Transaction-safe**: Prevents partial writes.

Adding Dependency to `build.gradle.kts`
```kotlin
    dependencies {
        // Preferences DataStore (SharedPreferences like APIs)
        implementation("androidx.datastore:datastore-preferences:1.2.1")
    }
```
Instantiating a new DataStore Instance
- Preferences DataStore implementation uses the DataStore and Preferences classes to persist key-value pairs to dis
- Use the property delegate created by preferencesDataStore to create an instance of DataStore<Preferences>
- Call it once at the top level of your Kotlin file
- Access DataStore through this property throughout the rest of your application. This makes it easier to keep your DataStore as a singleton
- The mandatory name parameter is the name of the Preferences DataStore
```kotlin
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
```

Creating a Key in DataStore
```kotlin
private val THEME_MODE = stringPreferencesKey("theme_mode")
```

Read Key Value Pair from DataStore
- Reading a Key Value Pair from DataStore is best described as a Publisher/ Subscriber Read System
- DataStore exposes data as a Flow<Preferences>, which is a stream that emits a new value every time the key value pair data changes
- To access the data, you will need to subscribe (collectAsState/ collectAsStateWithLifecycle) to this stream

```kotlin
// Publishing Stream from DataStore - Emitting updates to the key value pair
val themeMode: Flow<ThemeMode> = context.dataStore.data
    .map { prefs ->
        ThemeMode.valueOf(prefs[THEME_MODE] ?: ThemeMode.SYSTEM.name)
    }

// Subscribing to the Stream - Reading in the changes to the key value pair
// This converts the DataStore Flow into Compose State which can trigger recomposition of the UI if in a @Composable function
val themeMode by settingsDataStore.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
```

Edit Key Value Pair in DataStore
1. Preferences Datastore -> Use `edit` function
2. Proto Datastore -> Use `updateData` function
3. Custom Serializers -> Use `updateData` function

Edit Syntax is a blocking transaction (suspend)
```kotlin
suspend fun setThemeMode(mode: ThemeMode) {
    context.dataStore.edit { it[THEME_MODE] = mode.name }
}
```

updateData Syntax
```kotlin
suspend fun setThemeMode(mode: ThemeMode) {
    context.dataStore.updateData {
        it.toMutablePreferences().also { preferences ->
            preferences[THEME_MODE] = mode.name
        }
    }
}
```

---

### 7.3 Project Examples

**`data/local/db/StationEntity.kt`**:
```kotlin
@Entity(tableName = "stations")
data class StationEntity(
    @PrimaryKey val stationuuid: String,
    val name: String,
    val urlResolved: String,
    val favicon: String?,
    val tags: String,
    val country: String,
    val countrycode: String,
    val bitrate: Int,
    val codec: String,
    val votes: Int,
    val language: String,
    val languagecodes: String,
    val hls: Int,
    val geoLat: Double?,
    val geoLong: Double?,
    val isFavorite: Boolean = false
)
```

**`data/local/prefs/SettingsDataStore.kt`**:
```kotlin
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    private val THEME_MODE = stringPreferencesKey("theme_mode")
    private val ALLOW_HTTP = booleanPreferencesKey("allow_http_streams")
    private val DEFAULT_VOLUME = floatPreferencesKey("default_volume")

    val themeMode: Flow<ThemeMode> = context.dataStore.data
        .map { prefs ->
            ThemeMode.valueOf(prefs[THEME_MODE] ?: ThemeMode.SYSTEM.name)
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[THEME_MODE] = mode.name }
    }
}

enum class ThemeMode { LIGHT, DARK, SYSTEM }
```

---

## 8. Networking

### 8.1 Ktor Client

**Ktor** is a Kotlin-native HTTP client. It is lightweight, coroutine-based, and highly configurable via plugins.

### 8.2 Kotlinx Serialization

**Kotlinx Serialization** is a compiler plugin that generates serialization code at compile time. Unlike Gson or Moshi, it does not use reflection, making it faster and more compatible with Kotlin features (like default values and null-safety).

### 8.3 Mapping Data Layers

The app uses a **Mapper** pattern to convert between layers:
- **DTO** (Data Transfer Object): Raw JSON structure (`StationDto`).
- **Entity**: Room database table representation (`StationEntity`).
- **Domain Model**: UI-friendly representation (`Station`).

### 8.4 Project Examples

**`data/remote/api/IStationsApi.kt`**:
```kotlin
interface IStationsApi {
    suspend fun fetchManifest(): IndexManifestDto
    suspend fun fetchPage(pageNumber: Int): List<StationDto>
}
```

**`data/remote/api/StationsApi.kt`**:
```kotlin
class StationsApi : IStationsApi {

    private val baseUrl: String = BuildConfig.STATIONS_BASE_URL.removeSuffix("/")

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(Logging) {
            level = LogLevel.ALL
        }
    }

    override suspend fun fetchManifest(): IndexManifestDto {
        return client.get("$baseUrl/index.json").body()
    }

    override suspend fun fetchPage(pageNumber: Int): List<StationDto> {
        return client.get("$baseUrl/$pageNumber/stations.json").body()
    }
}
```

**`data/remote/dto/StationDto.kt`**:
```kotlin
@Serializable
data class StationDto(
    val stationuuid: String,
    val name: String,
    val url_resolved: String,
    val favicon: String? = null,
    val tags: List<String> = emptyList(),
    val country: String = "",
    val countrycode: String = "",
    val bitrate: Int = 0,
    val codec: String = "",
    val votes: Int = 0,
    val language: String = "",
    val languagecodes: String = "",
    val hls: Int = 0,
    val geo_lat: Double? = null,
    val geo_long: Double? = null
)
```

**`data/remote/mapper/StationMapper.kt`**:
```kotlin
fun StationDto.toEntity(): StationEntity {
    return StationEntity(
        stationuuid = stationuuid,
        name = name,
        urlResolved = url_resolved,
        favicon = favicon,
        tags = tags.joinToString(","),
        country = country,
        // ... other fields
    )
}

fun StationEntity.toDomain(): Station {
    return Station(
        stationuuid = stationuuid,
        name = name,
        // ... other fields
        isFavorite = isFavorite
    )
}
```

---

## 9. Media Playback

### 9.1 Media3 and ExoPlayer

**Media3** is the latest generation of Android media libraries. It unifies APIs for playback, media sessions, and UI components.

**ExoPlayer** is the core playback engine within Media3. It is highly customizable and supports adaptive streaming, DRM, and various media formats.

<!-- Instance creation error : could not create instance for '[Factory: 'com.yin_radio.yin_radio_android_app.ui.main.MainViewModel',binds:androidx.lifecycle.ViewModel]': java.lang.IllegalArgumentException: Failed to resolve SessionToken for ComponentInfo{com.yin_radio.yin_radio_android_app/com.yin_radio.yin_radio_android_app.service.RadioPlaybackService}. Manifest doesn't declare one of either MediaSessionService, MediaLibraryService, MediaBrowserService or MediaBrowserServiceCompat. Use service's full name  -->
 <!-- <action android:name="androidx.media3.session.MediaSessionService" /> -->

### 9.2 Foreground Service

Audio playback must continue even when the user leaves the app. This requires a **Foreground Service**.
- A foreground service shows a persistent notification.
- It requires the `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permissions.
- It is bound to a `MediaSession`, allowing lock-screen controls and Bluetooth integration.

### 9.3 Project Example: RadioPlaybackService

**`service/RadioPlaybackService.kt`**:
```kotlin
@OptIn(UnstableApi::class)
class RadioPlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true // handleAudioFocus
            )
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        mediaSession = MediaSession.Builder(this, player!!).build()
        createNotificationChannel()

        player!!.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING) {
                    if (player!!.isPlaying) startForeground()
                }
            }
        })
    }

    private fun startForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val mediaItem = player?.currentMediaItem
        val title = mediaItem?.mediaMetadata?.title ?: getString(R.string.app_name)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setStyle(
                androidx.media3.session.MediaStyleNotificationHelper.MediaStyle(mediaSession!!)
            )
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }
}
```

### 9.4 Supporting Multiple Languages (Localization)

For an overview of Android's localization framework, see the official guide:
https://developer.android.com/training/basics/supporting-devices/languages

#### String Resource Files

All user-facing text is externalized into XML string resources. The default English catalog lives in `res/values/strings.xml`. Translations are provided by placing locale-qualified `strings.xml` files in parallel directories:

| Locale | Directory |
|---|---|
| English (default) | `res/values/` |
| Chinese (Simplified) | `res/values-zh-rCN/` |
| Hindi | `res/values-hi/` |
| Spanish | `res/values-es/` |
| Arabic | `res/values-ar/` |
| French | `res/values-fr/` |
| Bengali | `res/values-bn/` |
| Portuguese | `res/values-pt/` |
| Russian | `res/values-ru/` |

At runtime, Android selects the folder that best matches the device's system language and falls back to `res/values/` (English) when a translation is missing.

#### Screen Models — Using `stringResource()` in Compose

Inside `@Composable` functions, hardcoded strings are replaced with `stringResource(...)` so Compose automatically resolves the correct translation.

**Before (hardcoded):**
```kotlin
Text(text = "Settings")
```

**After (localized):**
```kotlin
Text(text = stringResource(R.string.settings_title))
```

For strings that contain dynamic values, use the overload that accepts format arguments:

```kotlin
Text(text = stringResource(R.string.last_updated, relativeTimeString))
```

#### View Models — Emitting `@StringRes` IDs

`stringResource()` is a Composable function; it cannot be called from a `ViewModel` because ViewModels exist outside the Composition and have no access to `Context`. To keep error messages translatable, the ViewModel emits a **string resource ID** (`Int?`) instead of a raw `String`.

**Before:** ViewModel holds a raw English string.
```kotlin
data class MainUiState(
    val errorMessage: String? = null
)

// Inside ViewModel
_uiState.update { it.copy(errorMessage = "Station unavailable.") }
```

**After:** ViewModel holds a string resource reference.
```kotlin
data class MainUiState(
    val errorMessageRes: Int? = null
)

// Inside ViewModel
_uiState.update { it.copy(errorMessageRes = R.string.error_station_unavailable) }
```

The Compose screen then resolves the ID at composition time:

**Before:**
```kotlin
Text(text = uiState.errorMessage ?: "")
```

**After:**
```kotlin
uiState.errorMessageRes?.let { res ->
    Text(text = stringResource(res))
}
```

> **Non-Compose contexts:** Classes like `Service` that have access to `Context` can call `getString(R.string.xxx)` directly.

#### What is NOT translated

Only hardcoded UI text is extracted. Dynamic data returned by the Radio Browser API—such as station names, country names, language names, and tags—remains untranslated because it originates from the remote database, not the app's source code.

---

## 10. Common Pitfalls & Compatibility

### 10.1 AGP 9+ and KSP

AGP 9 introduced significant changes, including stricter handling of Kotlin source sets. There is a known compatibility issue with KSP:

- **Problem**: KSP internally uses the `kotlin.sourceSets` DSL to register generated sources. AGP 9's built-in Kotlin plugin forbids using `kotlin.sourceSets`, causing `:app:kspDebugKotlin` to fail.
- **Status**: This is a known issue tracked at [google/ksp#2729](https://github.com/google/ksp/issues/2729).
- **Workaround**: Downgrade AGP to a stable 8.x version or apply the documented workaround from the issue tracker until an official fix is released.

---

## 11. Appendix

### 11.1 Glossary

| Term | Definition |
|------|------------|
| **AGP** | Android Gradle Plugin. Adds Android-specific build logic to Gradle. |
| **APK** | Android Package. The distributable file format for Android apps. |
| **BOM** | Bill of Materials. A Gradle feature to manage versions of related libraries (e.g., Compose BOM). |
| **Compose** | Jetpack Compose. Google's modern declarative UI toolkit for Android. |
| **DAO** | Data Access Object. An interface defining database operations in Room. |
| **DataStore** | A modern, type-safe replacement for SharedPreferences. |
| **DEX** | Dalvik Executable. The bytecode format Android uses. |
| **DI** | Dependency Injection. A pattern for supplying objects with their dependencies. |
| **DTO** | Data Transfer Object. A plain object used to transfer data between processes. |
| **Flow** | A cold stream from Kotlin Coroutines, used for reactive programming. |
| **KSP** | Kotlin Symbol Processing. A compile-time API for processing Kotlin code. |
| **Ktor** | A Kotlin-native framework for building asynchronous client and server applications. |
| **Media3** | The latest generation of Android media libraries, including ExoPlayer. |
| **MVVM** | Model-View-ViewModel. An architectural pattern separating UI logic from business logic. |
| **Room** | An abstraction layer over SQLite, providing compile-time SQL verification. |
| **SDK** | Software Development Kit. The tools and libraries required to build Android apps. |
| **ViewModel** | An architecture component designed to store and manage UI-related data in a lifecycle-conscious way. |

### 11.2 Useful Gradle Commands

```bash
# Build a debug APK
./gradlew :app:assembleDebug

# Run unit tests
./gradlew :app:testDebugUnitTest

# Run instrumentation tests on a connected device
./gradlew :app:connectedAndroidTest

# Clean build artifacts
./gradlew clean

# Refresh dependencies (useful after updating versions)
./gradlew --refresh-dependencies

# Check for dependency updates
./gradlew dependencyUpdates
```
