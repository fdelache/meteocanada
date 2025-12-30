package dev.isalazy.meteocanada.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponseDto(
    @SerialName("displayName") val displayName: String,
    @SerialName("observation") val observation: ObservationDto,
    @SerialName("dailyFcst") val dailyFcst: DailyForecastContainerDto,
    @SerialName("hourlyFcst") val hourlyFcst: HourlyForecastContainerDto
)

@Serializable
data class ObservationDto(
    @SerialName("condition") val condition: String,
    @SerialName("temperature") val temperature: MetricValueDto,
    @SerialName("feelsLike") val feelsLike: MetricValueDto? = null,
    @SerialName("windDirection") val windDirection: String,
    @SerialName("windSpeed") val windSpeed: MetricValueDto,
    @SerialName("iconCode") val iconCode: String,
    @SerialName("timeStamp") val timeStamp: String
)

@Serializable
data class MetricValueDto(
    @SerialName("metric") val metric: String
)

@Serializable
data class DailyForecastContainerDto(
    @SerialName("daily") val daily: List<DailyForecastDto>
)

@Serializable
data class DailyForecastDto(
    @SerialName("date") val date: String,
    @SerialName("summary") val summary: String,
    @SerialName("temperature") val temperature: MetricValueDto,
    @SerialName("feelsLike") val feelsLike: MetricValueDto? = null,
    @SerialName("iconCode") val iconCode: String,
    @SerialName("precip") val precip: String? = null
)

@Serializable
data class HourlyForecastContainerDto(
    @SerialName("hourly") val hourly: List<HourlyForecastDto>
)

@Serializable
data class HourlyForecastDto(
    @SerialName("time") val time: String,
    @SerialName("condition") val condition: String,
    @SerialName("temperature") val temperature: MetricValueDto,
    @SerialName("feelsLike") val feelsLike: MetricValueDto? = null,
    @SerialName("iconCode") val iconCode: String,
    @SerialName("precip") val precip: String? = null
)

@Serializable
data class CitySearchDto(
    @SerialName("display_name") val displayName: String,
    @SerialName("lat") val lat: Double,
    @SerialName("lon") val lon: Double
)
