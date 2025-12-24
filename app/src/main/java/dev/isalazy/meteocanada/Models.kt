package dev.isalazy.meteocanada

data class CitySearchResult(val displayName: String, val lat: Double, val lon: Double)

data class WeatherData(
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val currentCondition: String,
    val currentTemperature: String,
    val wind: String,
    val currentIconUrl: String,
    val currentFeelsLike: String?,
    val observationTime: String,
    val dailyForecasts: List<DailyForecast>,
    val hourlyForecasts: List<HourlyForecast>
)

data class DailyForecast(
    val date: String,
    val summary: String,
    val temperature: String,
    val iconCode: String,
    val iconUrl: String,
    val feelsLike: String?,
    val precip: String
)

data class HourlyForecast(
    val time: String,
    val condition: String,
    val temperature: String,
    val iconCode: String,
    val iconUrl: String,
    val feelsLike: String?,
    val precip: String
)