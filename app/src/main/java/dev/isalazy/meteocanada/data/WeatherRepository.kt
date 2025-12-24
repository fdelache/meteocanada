package dev.isalazy.meteocanada.data

import android.util.Log
import dev.isalazy.meteocanada.CitySearchResult
import dev.isalazy.meteocanada.DailyForecast
import dev.isalazy.meteocanada.HourlyForecast
import dev.isalazy.meteocanada.WeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class WeatherRepository {

    suspend fun fetchWeather(latitude: Double, longitude: Double, lang: String): WeatherData {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://meteo.gc.ca/api/app/v3/$lang/Location/$latitude,$longitude?type=city")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                Log.d("WeatherData", response)
                parseWeatherData(response, latitude, longitude)
            } catch (e: Exception) {
                // If it fails, try the fallback location (Montreal) if the original request wasn't already for it?
                // For now, let's propagate the exception or handle retry logic in the ViewModel
                throw e
            }
        }
    }

    suspend fun searchCities(query: String, lang: String): List<CitySearchResult> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://meteo.gc.ca/api/accesscity/$lang?query=$query&limit=50")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                parseCitySearchResponse(response)
            } catch (e: Exception) {
                Log.e("CitySearch", "Failed to search for cities: ${e.message}", e)
                emptyList()
            }
        }
    }

    private fun parseWeatherData(json: String, latitude: Double, longitude: Double): WeatherData {
        val jsonArray = JSONArray(json)
        val jsonObject = jsonArray.getJSONObject(0)
        val location = jsonObject.getString("displayName")
        val observation = jsonObject.getJSONObject("observation")
        val currentCondition = observation.getString("condition")
        val currentTemperature = observation.getJSONObject("temperature").getString("metric")
        val currentFeelsLike = observation.optJSONObject("feelsLike")?.getString("metric")
        val wind = "${observation.getString("windDirection")} ${observation.getJSONObject("windSpeed").getString("metric")} km/h"
        val currentIconUrl = "https://meteo.gc.ca/weathericons/${observation.getString("iconCode")}.gif"
        val timestampString = observation.getString("timeStamp")
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(timestampString)
        val outputFormat = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, Locale.getDefault())
        outputFormat.timeZone = TimeZone.getDefault()
        val observationTime = date?.let { outputFormat.format(it) } ?: ""

        val dailyForecasts = mutableListOf<DailyForecast>()
        val dailyForecastArray = jsonObject.getJSONObject("dailyFcst").getJSONArray("daily")
        for (i in 0 until dailyForecastArray.length()) {
            val daily = dailyForecastArray.getJSONObject(i)
            val temperature = daily.getJSONObject("temperature").getString("metric")
            val feelsLike = daily.optJSONObject("feelsLike")?.getString("metric")
            dailyForecasts.add(
                DailyForecast(
                    date = daily.getString("date"),
                    summary = daily.getString("summary"),
                    temperature = temperature,
                    iconCode = daily.getString("iconCode"),
                    iconUrl = "https://meteo.gc.ca/weathericons/${daily.getString("iconCode")}.gif",
                    feelsLike = if (feelsLike != temperature && !feelsLike.isNullOrBlank()) feelsLike else null,
                    precip = daily.getString("precip")
                )
            )
        }

        val hourlyForecasts = mutableListOf<HourlyForecast>()
        val hourlyForecast = jsonObject.getJSONObject("hourlyFcst").getJSONArray("hourly")
        for (i in 0 until hourlyForecast.length()) {
            val hourly = hourlyForecast.getJSONObject(i)
            val temperature = hourly.getJSONObject("temperature").getString("metric")
            val feelsLike = hourly.optJSONObject("feelsLike")?.getString("metric")
            hourlyForecasts.add(
                HourlyForecast(
                    time = hourly.getString("time"),
                    condition = hourly.getString("condition"),
                    temperature = temperature,
                    iconCode = hourly.getString("iconCode"),
                    iconUrl = "https://meteo.gc.ca/weathericons/${hourly.getString("iconCode")}.gif",
                    feelsLike = if (feelsLike != temperature && !feelsLike.isNullOrBlank()) feelsLike else null,
                    precip = hourly.getString("precip")
                )
            )
        }

        return WeatherData(location, latitude, longitude, currentCondition, currentTemperature, wind, currentIconUrl, if (currentFeelsLike != currentTemperature && !currentFeelsLike.isNullOrBlank()) currentFeelsLike else null, observationTime, dailyForecasts, hourlyForecasts)
    }

    private fun parseCitySearchResponse(json: String): List<CitySearchResult> {
        val results = mutableListOf<CitySearchResult>()
        val jsonArray = JSONArray(json)
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            results.add(
                CitySearchResult(
                    displayName = jsonObject.getString("display_name"),
                    lat = jsonObject.getDouble("lat"),
                    lon = jsonObject.getDouble("lon")
                )
            )
        }
        return results
    }
}