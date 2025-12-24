# Meteo Canada - Project Overview for LLMs

## Project Goal
**Meteo Canada** is a native Android application designed to provide accurate weather information for Canada. It sources data directly from **Environment and Climate Change Canada (ECCC)**. The app focuses on simplicity, modern UI/UX, and specific Canadian weather features like accurate local radar.

## Core Features
1.  **Current Conditions:** Real-time temperature, wind, "feels like", and condition icons.
2.  **Forecasts:** Detailed hourly and daily weather predictions.
3.  **Radar Map:** An interactive, animated radar map visualizing precipitation. It supports historical playback (1h or 3h history) and uses `proj4j` for correct map projections.
4.  **Location Management:**
    *   Automatic location detection (Fused Location Provider).
    *   Manual city search and management (add/remove saved cities).
5.  **Customization:**
    *   Bilingual (English/French).
    *   Dark Mode / Light Mode.
    *   Radar history duration settings.

## Tech Stack & Architecture
*   **Language:** Kotlin
*   **UI Framework:** **Jetpack Compose** (Material3 Design).
*   **Architecture Pattern:** Currently a monolithic `MainActivity` containing Navigation, UI Composables, and Data Fetching. *Note: Future refactoring to MVVM is a potential improvement.*
*   **Asynchronous Processing:** Kotlin Coroutines (`launch`, `async`, `produceState`).
*   **Networking:**
    *   `java.net.HttpURLConnection` (for API calls - *legacy style, could be upgraded to Retrofit/OkHttp*).
    *   **Coil** (for async image loading, e.g., weather icons, radar tiles).
*   **Location:** Google Play Services (`FusedLocationProviderClient`).
*   **Mapping:** Custom implementation using `Proj4j` for coordinate projection (likely Lambert Conformal Conic for Canada) to tile coordinates.
*   **Persistence:** `SharedPreferences` (wrapped in `UserPreferences`) for saving user settings and selected cities.
*   **Build System:** Gradle (Kotlin DSL).

## Key Files & Directories

### Root
*   `build.gradle.kts`: Project-level build configuration.

### App Module (`app/src/main/java/dev/isalazy/meteocanada/`)
*   **`MainActivity.kt`**: The **God Object** of this application. It currently handles:
    *   App entry point & Navigation host.
    *   `WeatherScreen`, `SettingsScreen`, `RadarScreen` composables.
    *   Fetching weather data from ECCC API (`fetchWeather`).
    *   Parsing JSON responses (`parseWeatherData`).
    *   City search logic.
*   **`UserPreferences.kt`**: Wrapper around `SharedPreferences`. Manages:
    *   Selected city & saved city list.
    *   App language (en/fr).
    *   Theme preference (Dark/Light).
    *   Radar history settings.
*   **`ui/MapUtils.kt`**: **Critical for Radar.** Contains math for converting geolocation (lat/lon) to map tile coordinates, handling the specific projections required by ECCC radar data.
*   **`ui/composables/RadarMap.kt`**: Composable responsible for rendering the map tiles.

### Resources (`app/src/main/res/`)
*   `values/strings.xml`: English strings.
*   `values-fr/strings.xml`: French strings.
*   `drawable/`: Vector assets (icons).

## External APIs (ECCC)
*   **Weather Data:** `https://meteo.gc.ca/api/app/v3/{lang}/Location/{lat},{lon}?type=city`
*   **City Search:** `https://meteo.gc.ca/api/accesscity/{lang}?query={query}&limit=50`
*   **Icons:** `https://meteo.gc.ca/weathericons/{iconCode}.gif`
*   **Radar Tiles:** Computed dynamically in `MapUtils`.

## Known Issues / specificities
*   **Architecture:** The code is heavily concentrated in `MainActivity.kt`. Logic is mixed with UI code.
*   **Permissions:** Location permission is handled manually with `ActivityResultContracts`.
*   **Concurrency:** Heavy use of Coroutines for network requests directly in the UI layer (inside `LaunchedEffect` or `lifecycleScope`).
