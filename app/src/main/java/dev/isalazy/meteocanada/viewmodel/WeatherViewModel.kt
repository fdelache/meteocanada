package dev.isalazy.meteocanada.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.isalazy.meteocanada.CitySearchResult
import dev.isalazy.meteocanada.UserPreferences
import dev.isalazy.meteocanada.WeatherData
import dev.isalazy.meteocanada.data.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val repository: WeatherRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _weatherData = MutableStateFlow<WeatherData?>(null)
    val weatherData: StateFlow<WeatherData?> = _weatherData.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _citySearchResults = MutableStateFlow<List<CitySearchResult>>(emptyList())
    val citySearchResults: StateFlow<List<CitySearchResult>> = _citySearchResults.asStateFlow()

    private var searchJob: Job? = null

    // Helper to get fallback location if needed
    private val FALLBACK_LAT = 45.529
    private val FALLBACK_LON = -73.562

    fun loadWeather(lat: Double, lon: Double, forceRefresh: Boolean = false) {
        if (_isRefreshing.value && !forceRefresh) return

        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val lang = userPreferences.appLanguage
                val data = repository.fetchWeather(lat, lon, lang)
                _weatherData.value = data
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error fetching weather", e)
                // Fallback logic
                 try {
                     val lang = userPreferences.appLanguage
                     Log.d("WeatherViewModel", "Retrying with fallback location")
                     val fallbackData = repository.fetchWeather(FALLBACK_LAT, FALLBACK_LON, lang)
                     _weatherData.value = fallbackData
                 } catch (e2: Exception) {
                     Log.e("WeatherViewModel", "Fallback failed too", e2)
                 }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun searchCities(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            if (query.length > 2) {
                val lang = userPreferences.appLanguage
                val results = repository.searchCities(query, lang)
                _citySearchResults.value = results
            } else {
                _citySearchResults.value = emptyList()
            }
        }
    }

    fun clearSearchResults() {
        _citySearchResults.value = emptyList()
    }
}

class WeatherViewModelFactory(
    private val repository: WeatherRepository,
    private val userPreferences: UserPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WeatherViewModel(repository, userPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}