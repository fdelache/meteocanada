package com.example.meteocanada.ui.composables

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import coil3.compose.AsyncImage
import com.example.meteocanada.ui.MapUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun RadarMap(lat: Double, lon: Double, zoom: Int, modifier: Modifier = Modifier) {
    val timestamp = produceState<String?>(initialValue = null) {
        value = withContext(Dispatchers.IO) {
            MapUtils.getLatestTimestamp()
        }
    }.value

    if (timestamp != null) {
        val (baseZoom, baseX, baseY) = MapUtils.getTileCoordinates(lat, lon, zoom, "base")
        val (textZoom, textX, textY) = MapUtils.getTileCoordinates(lat, lon, zoom, "text")
        val (radarZoom, radarX, radarY) = MapUtils.getTileCoordinates(lat, lon, zoom, "radar")

        val baseUrl = MapUtils.getBaseMapUrl(baseZoom, baseX, baseY)
        val cityNamesUrl = MapUtils.getCityNamesMapUrl(textZoom, textX, textY)
        val radarUrl = MapUtils.getRadarMapUrl(timestamp, radarZoom, radarX, radarY)

        Log.d("RadarMap", "Base URL: $baseUrl")
        Log.d("RadarMap", "City Names URL: $cityNamesUrl")
        Log.d("RadarMap", "Radar URL: $radarUrl")

        Box(modifier = modifier) {
            AsyncImage(
                model = baseUrl,
                contentDescription = "Base Map",
                modifier = Modifier.matchParentSize()
            )
            AsyncImage(
                model = cityNamesUrl,
                contentDescription = "City Names",
                modifier = Modifier.matchParentSize()
            )
            AsyncImage(
                model = radarUrl,
                contentDescription = "Radar Map",
                modifier = Modifier.matchParentSize()
            )
        }
    }
}
