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

        // Montreal's coordinates
        const val MONTREAL_LAT = 45.5017f
        const val MONTREAL_LON = -73.5673f

        // Toronto's coordinates
        const val TORONTO_LAT = 43.6532f
        const val TORONTO_LON = -79.3832f
    }

    fun getLocation(): Pair<Float, Float> {
        val lat = preferences.getFloat(KEY_LATITUDE, MONTREAL_LAT)
        val lon = preferences.getFloat(KEY_LONGITUDE, MONTREAL_LON)
        return Pair(lat, lon)
    }

    fun setLocation(latitude: Float, longitude: Float) {
        preferences.edit {
            putFloat(KEY_LATITUDE, latitude)
                .putFloat(KEY_LONGITUDE, longitude)
        }
    }

    fun isLocationSet(): Boolean {
        return preferences.contains(KEY_LATITUDE) && preferences.contains(KEY_LONGITUDE)
    }
}
