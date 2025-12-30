package dev.isalazy.meteocanada

import android.app.Application
import dev.isalazy.meteocanada.data.WeatherRepository

class MeteoCanadaApp : Application() {
    lateinit var repository: WeatherRepository
    lateinit var userPreferences: UserPreferences

    override fun onCreate() {
        super.onCreate()
        repository = WeatherRepository()
        userPreferences = UserPreferences(this)
    }
}
