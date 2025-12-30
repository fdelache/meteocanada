package dev.isalazy.meteocanada.data

import android.util.Log
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import dev.isalazy.meteocanada.CitySearchResult
import dev.isalazy.meteocanada.DailyForecast
import dev.isalazy.meteocanada.HourlyForecast
import dev.isalazy.meteocanada.WeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class WeatherRepository {

    private val api: MeteoCanadaApi

    init {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://meteo.gc.ca/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        api = retrofit.create(MeteoCanadaApi::class.java)
    }

    suspend fun fetchWeather(latitude: Double, longitude: Double, lang: String): WeatherData {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getWeather(lang, latitude, longitude)
                if (response.isNotEmpty()) {
                    mapToWeatherData(response[0], latitude, longitude)
                } else {
                    throw Exception("Empty response from weather API")
                }
            } catch (e: Exception) {
                Log.e("WeatherRepository", "Error fetching weather", e)
                throw e
            }
        }
    }

    suspend fun searchCities(query: String, lang: String): List<CitySearchResult> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.searchCities(lang, query)
                response.map {
                    CitySearchResult(
                        displayName = it.displayName,
                        lat = it.lat,
                        lon = it.lon
                    )
                }
            } catch (e: Exception) {
                Log.e("CitySearch", "Failed to search for cities: ${e.message}", e)
                emptyList()
            }
        }
    }

    private fun mapToWeatherData(dto: WeatherResponseDto, latitude: Double, longitude: Double): WeatherData {
        val observation = dto.observation
        
        // Date Formatting
        val timestampString = observation.timeStamp
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        
        // Handle potentially different timestamp formats or parse errors gracefully
        val date = try {
            inputFormat.parse(timestampString)
        } catch (_: Exception) {
            null
        }
        
        val outputFormat = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, Locale.getDefault())
        outputFormat.timeZone = TimeZone.getDefault()
        val observationTime = date?.let { outputFormat.format(it) } ?: ""

        // Conditions
        val currentTemperature = observation.temperature.metric
        val currentFeelsLike = observation.feelsLike?.metric
        val displayFeelsLike = if (currentFeelsLike != currentTemperature && !currentFeelsLike.isNullOrBlank()) currentFeelsLike else null
        
        val wind = "${observation.windDirection} ${observation.windSpeed.metric} km/h"
        val currentIconUrl = "https://meteo.gc.ca/weathericons/${observation.iconCode}.gif"

        val dailyForecasts = dto.dailyFcst.daily.map { daily ->
             val temp = daily.temperature.metric
             val fl = daily.feelsLike?.metric
             DailyForecast(
                 date = daily.date,
                 summary = daily.summary,
                 temperature = temp,
                 iconCode = daily.iconCode,
                 iconUrl = "https://meteo.gc.ca/weathericons/${daily.iconCode}.gif",
                 feelsLike = if (fl != temp && !fl.isNullOrBlank()) fl else null,
                 precip = daily.precip ?: ""
             )
        }

        val hourlyForecasts = dto.hourlyFcst.hourly.map { hourly ->
            val temp = hourly.temperature.metric
            val fl = hourly.feelsLike?.metric
            HourlyForecast(
                time = hourly.time,
                condition = hourly.condition,
                temperature = temp,
                iconCode = hourly.iconCode,
                iconUrl = "https://meteo.gc.ca/weathericons/${hourly.iconCode}.gif",
                feelsLike = if (fl != temp && !fl.isNullOrBlank()) fl else null,
                precip = hourly.precip ?: ""
            )
        }

        return WeatherData(
            location = dto.displayName,
            latitude = latitude,
            longitude = longitude,
            currentCondition = observation.condition,
            currentTemperature = currentTemperature,
            wind = wind,
            currentIconUrl = currentIconUrl,
            currentFeelsLike = displayFeelsLike,
            observationTime = observationTime,
            dailyForecasts = dailyForecasts,
            hourlyForecasts = hourlyForecasts
        )
    }
}
