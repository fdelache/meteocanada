package dev.isalazy.meteocanada

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class UserPreferences(context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences("meteo_canada_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_LATITUDE = "latitude"
        const val KEY_LONGITUDE = "longitude"
        const val KEY_APP_LANGUAGE = "app_language"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_LOCATION_MODE = "location_mode"
        const val KEY_LOCATION_NAME = "location_name"
        const val KEY_RADAR_HISTORY = "radar_history"

        // Montreal's coordinates
        const val MONTREAL_LAT = 45.5017f
        const val MONTREAL_LON = -73.5673f
    }

    fun getLocation(): Pair<Float, Float> {
        val lat = preferences.getFloat(KEY_LATITUDE, MONTREAL_LAT)
        val lon = preferences.getFloat(KEY_LONGITUDE, MONTREAL_LON)
        return Pair(lat, lon)
    }

    fun setLocation(latitude: Float, longitude: Float, name: String? = null) {
        preferences.edit {
            putFloat(KEY_LATITUDE, latitude)
            putFloat(KEY_LONGITUDE, longitude)
            name?.let { putString(KEY_LOCATION_NAME, it) }
        }
    }

    fun isLocationSet(): Boolean {
        return preferences.contains(KEY_LATITUDE) && preferences.contains(KEY_LONGITUDE)
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
        get() = preferences.getString(KEY_LOCATION_MODE, "detect") ?: "detect"
        set(value) {
            preferences.edit {
                putString(KEY_LOCATION_MODE, value)
                if (value == "detect") {
                    remove(KEY_LOCATION_NAME)
                }
            }
        }

    var locationName: String?
        get() = preferences.getString(KEY_LOCATION_NAME, null)
        set(value) {
            preferences.edit {
                if (value == null) {
                    remove(KEY_LOCATION_NAME)
                } else {
                    putString(KEY_LOCATION_NAME, value)
                }
            }
        }

    var radarHistory: Int
        get() = preferences.getInt(KEY_RADAR_HISTORY, 1)
        set(value) {
            preferences.edit { putInt(KEY_RADAR_HISTORY, value) }
        }
}
