package dev.isalazy.meteocanada.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MeteoCanadaApi {
    @GET("api/app/v3/{lang}/Location/{lat},{lon}?type=city")
    suspend fun getWeather(
        @Path("lang") lang: String,
        @Path("lat") lat: Double,
        @Path("lon") lon: Double
    ): List<WeatherResponseDto>

    @GET("api/accesscity/{lang}")
    suspend fun searchCities(
        @Path("lang") lang: String,
        @Query("query") query: String,
        @Query("limit") limit: Int = 50
    ): List<CitySearchDto>
}
