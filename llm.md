# Meteo Canada - Project Overview for LLMs

## Project Goal
**Meteo Canada** is a native Android application designed to provide accurate weather information for Canada. It sources data directly from **Environment and Climate Change Canada (ECCC)**. The app focuses on simplicity, modern UI/UX, and specific Canadian weather features like accurate local radar.

## Core Features
1.  **Current Conditions:** Real-time temperature, wind, "feels like", and condition icons.
2.  **Forecasts:** Detailed hourly and daily weather predictions.
3.  **Interactive Radar Map:** A fully interactive (pan & zoom) animated radar map visualizing precipitation. It supports historical playback (1h or 3h history) and uses `proj4j` for correct map projections (Lambert Conformal Conic).
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
*   **Architecture Pattern:** **MVVM (Model-View-ViewModel)** with Repository pattern.
    *   **UI:** Activities and Composables (`MainActivity`, `WeatherScreen`).
    *   **ViewModel:** `WeatherViewModel` (manages UI state, survives configuration changes).
    *   **Data:** `WeatherRepository` (abstracts API calls).
*   **Asynchronous Processing:** Kotlin Coroutines & Flow (`StateFlow` for UI state).
*   **Networking:**
    *   `java.net.HttpURLConnection` (inside `WeatherRepository` - specific low-level implementation).
    *   **Coil** (for async image loading, e.g., weather icons, radar tiles).
*   **Location:** Google Play Services (`FusedLocationProviderClient`).
*   **Mapping:** Custom implementation using `Proj4j` to project WGS84 coordinates to Canada Atlas Lambert (EPSG:3978) for tile mapping.
*   **Persistence:** `SharedPreferences` (wrapped in `UserPreferences`) for saving user settings and selected cities.

## Key Files & Directories

### Root
*   `build.gradle.kts`: Project-level build configuration.

### App Module (`app/src/main/java/dev/isalazy/meteocanada/`)

#### Core
*   **`MainActivity.kt`**: App entry point. Sets up the Theme, Navigation Host, and initializes the `WeatherViewModel`. It contains the main UI Composables (`WeatherScreen`, `SettingsScreen`, `RadarScreen`).
*   **`Models.kt`**: Contains data classes: `WeatherData`, `HourlyForecast`, `DailyForecast`, `CitySearchResult`.
*   **`UserPreferences.kt`**: Wrapper around `SharedPreferences`. Manages selected city, language, theme, and radar settings.

#### ViewModel (`/viewmodel`)
*   **`WeatherViewModel.kt`**: Holds the logic for the app. Exposes `weatherData` and `citySearchResults` via `StateFlow`. Handles errors and loading states.
*   `WeatherViewModelFactory`: Manual factory for injecting the Repository and Preferences into the ViewModel.

#### Data (`/data`)
*   **`WeatherRepository.kt`**: The single source of truth for data. Handles:
    *   Fetching weather data from ECCC API.
    *   Searching cities.
    *   Parsing raw JSON responses into Model objects.

#### UI (`/ui`)
*   **`composables/RadarMap.kt`**: **Interactive Map Component.** Uses `detectTransformGestures` to handle pan and zoom. Renders Base, Text, and Radar layers.
*   **`MapUtils.kt`**: **Critical Mapping Logic.**
    *   Handles Coordinate Reference System (CRS) transformations (EPSG:4326 <-> EPSG:3978).
    *   Calculates Tile Matrix logic (resolution, tile indices) for the custom ECCC tile server.
    *   Includes logic to fetch specific tile layers (Base, City Names, Radar).

## External APIs (ECCC)
*   **Weather Data:** `https://meteo.gc.ca/api/app/v3/{lang}/Location/{lat},{lon}?type=city`
*   **City Search:** `https://meteo.gc.ca/api/accesscity/{lang}?query={query}&limit=50`
*   **Icons:** `https://meteo.gc.ca/weathericons/{iconCode}.gif`
*   **Radar Tiles:** Computed dynamically via WMTS templates in `MapUtils`.

## Specificities & Constraints
*   **Dependency Injection:** Currently uses manual injection (Factories) instead of a DI framework like Hilt.
*   **Permissions:** Location permission is handled via `ActivityResultContracts` in `MainActivity` and passed down via callbacks.
*   **JSON Parsing:** Uses `org.json` (built-in Android) instead of Moshi/Gson. Logic is isolated in `WeatherRepository`.
*   **Radar Rendering:** The map is *not* Google Maps. It is a custom `Box` based implementation rendering image tiles using Coil, aligned via complex projection math in `MapUtils`.