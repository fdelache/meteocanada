package dev.isalazy.meteocanada

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONObject

class UserPreferences(context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences("meteo_canada_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_LATITUDE = "latitude"
        const val KEY_LONGITUDE = "longitude"
        const val KEY_APP_LANGUAGE = "app_language"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_CITIES = "cities"
        const val KEY_SELECTED_CITY_JSON = "selected_city_json"
        const val KEY_RADAR_HISTORY = "radar_history"

        // Montreal's coordinates
        const val MONTREAL_LAT = 45.5017f
        const val MONTREAL_LON = -73.5673f
    }

    data class SavedCity(val displayName: String, val lat: Double, val lon: Double) {
        fun toJson(): String {
            return JSONObject().apply {
                put("displayName", displayName)
                put("lat", lat)
                put("lon", lon)
            }.toString()
        }

        companion object {
            fun fromJson(json: String): SavedCity {
                val jsonObject = JSONObject(json)
                return SavedCity(
                    displayName = jsonObject.getString("displayName"),
                    lat = jsonObject.getDouble("lat"),
                    lon = jsonObject.getDouble("lon")
                )
            }
        }
    }

    fun getCities(): List<SavedCity> {
        val citySet = preferences.getStringSet(KEY_CITIES, emptySet()) ?: emptySet()
        return citySet.map { SavedCity.fromJson(it) }
    }

    fun addCity(city: SavedCity) {
        val cities = getCities().toMutableList()
        if (!cities.any { it.displayName == city.displayName }) {
            cities.add(city)
            preferences.edit {
                putStringSet(KEY_CITIES, cities.map { it.toJson() }.toSet())
            }
        }
    }

    fun removeCity(city: SavedCity) {
        val cities = getCities().toMutableList()
        if (cities.remove(city)) {
            preferences.edit {
                putStringSet(KEY_CITIES, cities.map { it.toJson() }.toSet())
            }
        }
    }

    var selectedCity: SavedCity?
        get() {
            val json = preferences.getString(KEY_SELECTED_CITY_JSON, null)
            return json?.let { SavedCity.fromJson(it) }
        }
        set(value) {
            preferences.edit {
                if (value == null) {
                    remove(KEY_SELECTED_CITY_JSON)
                } else {
                    putString(KEY_SELECTED_CITY_JSON, value.toJson())
                }
            }
        }

    fun getLocation(): Pair<Float, Float> {
        val city = selectedCity
        return if (city != null) {
            Pair(city.lat.toFloat(), city.lon.toFloat())
        } else {
            val lat = preferences.getFloat(KEY_LATITUDE, MONTREAL_LAT)
            val lon = preferences.getFloat(KEY_LONGITUDE, MONTREAL_LON)
            Pair(lat, lon)
        }
    }

    fun setLocation(latitude: Float, longitude: Float) {
        preferences.edit {
            putFloat(KEY_LATITUDE, latitude)
            putFloat(KEY_LONGITUDE, longitude)
        }
    }

    fun isLocationSet(): Boolean {
        return selectedCity != null || (preferences.contains(KEY_LATITUDE) && preferences.contains(KEY_LONGITUDE))
    }

    var appLanguage: String
        get() = preferences.getString(KEY_APP_LANGUAGE, "en") ?: "en"
        set(value) {
            preferences.edit { putString(KEY_APP_LANGUAGE, value) }
        }

    var isDarkMode: Boolean
        get() = preferences.getBoolean(KEY_DARK_MODE, false)
        set(value) {
            preferences.edit { putBoolean(KEY_DARK_MODE, value) }
        }

    var locationMode: String
        get() = if (selectedCity == null) "detect" else "manual"
        set(value) {
            if (value == "detect") {
                selectedCity = null
            }
        }

    val locationName: String?
        get() = selectedCity?.displayName


    var radarHistory: Int
        get() = preferences.getInt(KEY_RADAR_HISTORY, 1)
        set(value) {
            preferences.edit { putInt(KEY_RADAR_HISTORY, value) }
        }
}