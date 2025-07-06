package com.example.meteocanada.ui.composables

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import coil3.compose.AsyncImage
import com.example.meteocanada.ui.MapUtils
import com.example.meteocanada.ui.MapUtils.RadarLayer

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun RadarMap(layer: RadarLayer, lat: Double, lon: Double, zoom: Int, modifier: Modifier = Modifier, scale: Float = 1f) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().clipToBounds()) {
        val widthPx = constraints.maxWidth
        val heightPx = constraints.maxHeight

        val projectedBounds = MapUtils.getProjectedBounds(lat, lon, zoom, widthPx, heightPx)

        val (baseMinX, baseMinY, baseMaxX, baseMaxY) = MapUtils.getTileCoordinatesForBounds(projectedBounds, zoom, "base")
        val (textMinX, textMinY, textMaxX, textMaxY) = MapUtils.getTileCoordinatesForBounds(projectedBounds, zoom, "text")
        val (radarMinX, radarMinY, radarMaxX, radarMaxY) = MapUtils.getTileCoordinatesForBounds(projectedBounds, zoom, "radar")

        val baseLayoutParams = MapUtils.getTileLayoutParams(zoom, "base")
        val textLayoutParams = MapUtils.getTileLayoutParams(zoom, "text")
        val radarLayoutParams = MapUtils.getTileLayoutParams(zoom, "radar")

        Box(modifier = Modifier.fillMaxSize().scale(scale)) {
            if (baseLayoutParams != null) {
                for (y in baseMinY..baseMaxY) {
                    for (x in baseMinX..baseMaxX) {
                        val left = (x * baseLayoutParams.tileWidth) - (projectedBounds.first.x - baseLayoutParams.topLeftCornerX) / baseLayoutParams.resolution
                        val top = (y * baseLayoutParams.tileHeight) - (baseLayoutParams.topLeftCornerY - projectedBounds.second.y) / baseLayoutParams.resolution
                        AsyncImage(
                            model = MapUtils.getBaseMapUrl(zoom, x, y),
                            contentDescription = "Base Map Tile",
                            modifier = Modifier.offset {
                                androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt())
                            }
                        )
                    }
                }
            }

            if (textLayoutParams != null) {
                for (y in textMinY..textMaxY) {
                    for (x in textMinX..textMaxX) {
                        val left = (x * textLayoutParams.tileWidth) - (projectedBounds.first.x - textLayoutParams.topLeftCornerX) / textLayoutParams.resolution
                        val top = (y * textLayoutParams.tileHeight) - (textLayoutParams.topLeftCornerY - projectedBounds.second.y) / textLayoutParams.resolution
                        AsyncImage(
                            model = MapUtils.getCityNamesMapUrl(zoom, x, y),
                            contentDescription = "City Names Tile",
                            modifier = Modifier.offset {
                                androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt())
                            }
                        )
                    }
                }
            }

            if (radarLayoutParams != null) {
                for (y in radarMinY..radarMaxY) {
                    for (x in radarMinX..radarMaxX) {
                        val left = (x * radarLayoutParams.tileWidth) - (projectedBounds.first.x - radarLayoutParams.topLeftCornerX) / radarLayoutParams.resolution
                        val top = (y * radarLayoutParams.tileHeight) - (radarLayoutParams.topLeftCornerY - projectedBounds.second.y) / radarLayoutParams.resolution
                        AsyncImage(
                            model = MapUtils.getRadarMapUrl(layer, zoom, x, y),
                            contentDescription = "Radar Map Tile",
                            modifier = Modifier.offset {
                                androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt())
                            }.alpha(0.5f)
                        )
                    }
                }
            }
        }
    }
}