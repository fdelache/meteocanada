package dev.isalazy.meteocanada.ui.composables

import android.annotation.SuppressLint
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import coil3.compose.AsyncImage
import dev.isalazy.meteocanada.ui.MapUtils
import dev.isalazy.meteocanada.ui.MapUtils.RadarLayer
import org.locationtech.proj4j.ProjCoordinate
import kotlin.math.roundToInt

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun RadarMap(layer: RadarLayer, lat: Double, lon: Double, zoom: Int, modifier: Modifier = Modifier) {
    var currentZoom by remember { mutableIntStateOf(zoom) }
    var currentScale by remember { mutableFloatStateOf(1f) }

    // Initialize centerProjected only once when lat/lon changes significantly or on first load.
    // Using remember(lat, lon) resets the map if the user selects a new city, which is desired.
    var centerProjected by remember(lat, lon) {
        mutableStateOf(MapUtils.project(lat, lon))
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().clipToBounds()) {
        val widthPx = constraints.maxWidth
        val heightPx = constraints.maxHeight

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        val oldScale = currentScale
                        currentScale *= zoomChange

                        // 1. Update Scale & Check for Zoom Level Switch
                        val baseRes = MapUtils.getResolution(currentZoom)
                        val nextZoom = currentZoom + 1
                        val prevZoom = currentZoom - 1
                        val nextRes = MapUtils.getResolution(nextZoom)
                        val prevRes = MapUtils.getResolution(prevZoom)
                        
                        // Effective resolution is what the user "sees"
                        val effectiveRes = baseRes / currentScale

                        // Thresholds to switch zoom levels
                        if (nextRes > 0 && effectiveRes <= nextRes) {
                            // Zoom In Switch
                            currentZoom = nextZoom
                            // Adjust scale to maintain visual continuity
                            // effectiveRes = newBaseRes / newScale => newScale = newBaseRes / effectiveRes
                            currentScale = (nextRes / effectiveRes).toFloat()
                            if (currentScale < 1f) currentScale = 1f // Prevent float error underflow
                        } else if (prevRes > 0 && effectiveRes >= prevRes) {
                            // Zoom Out Switch
                            currentZoom = prevZoom
                            currentScale = (prevRes / effectiveRes).toFloat()
                            if (currentScale > 1f) currentScale = 1f 
                        }
                        
                        // Limit scale to avoiding crazy values if we hit min/max zoom
                        // Assuming max zoom is around 19 (from MapUtils)
                        // If we are at max zoom, we can still scale up a bit (digital zoom)
                        currentScale = currentScale.coerceIn(0.2f, 5.0f)

                        // 2. Update Center (Pan)
                        // Use the resolution at the CURRENT scale
                        val currentRes = baseRes / currentScale
                        
                        // Pan X: Moving finger RIGHT (+x) shifts map content RIGHT. 
                        // We see content to the LEFT.
                        // Project X increases East (Right).
                        // So Center X decreases.
                        val deltaX = -pan.x * currentRes
                        
                        // Pan Y: Moving finger DOWN (+y) shifts map content DOWN.
                        // We see content ABOVE.
                        // Project Y increases North (Up).
                        // So Center Y increases.
                        val deltaY = pan.y * currentRes
                        
                        centerProjected = ProjCoordinate(centerProjected.x + deltaX, centerProjected.y + deltaY)
                    }
                }
        ) {
            MapContent(
                layer = layer,
                centerProjected = centerProjected,
                zoom = currentZoom,
                scale = currentScale,
                widthPx = widthPx,
                heightPx = heightPx
            )
        }
    }
}

@Composable
fun MapContent(
    layer: RadarLayer,
    centerProjected: ProjCoordinate,
    zoom: Int,
    scale: Float,
    widthPx: Int,
    heightPx: Int
) {
    val tileMatrix = MapUtils.getTileLayoutParams(zoom, "radar")
    
    // If invalid zoom, just return (or show error)
    if (tileMatrix == null) return

    val resolution = tileMatrix.resolution

    // Calculate the bounds of the area we need to render TILES for.
    // This is the area corresponding to the screen size AT SCALE 1.0.
    // We render this, and then scale it visually.
    val halfWidthMeters = widthPx / 2.0 * resolution
    val halfHeightMeters = heightPx / 2.0 * resolution

    val minX = centerProjected.x - halfWidthMeters
    val minY = centerProjected.y - halfHeightMeters
    val maxX = centerProjected.x + halfWidthMeters
    val maxY = centerProjected.y + halfHeightMeters

    val bounds = Pair(ProjCoordinate(minX, minY), ProjCoordinate(maxX, maxY))

    val (baseMinX, baseMinY, baseMaxX, baseMaxY) = MapUtils.getTileCoordinatesForBounds(bounds, zoom, "base")
    val (textMinX, textMinY, textMaxX, textMaxY) = MapUtils.getTileCoordinatesForBounds(bounds, zoom, "text")
    val (radarMinX, radarMinY, radarMaxX, radarMaxY) = MapUtils.getTileCoordinatesForBounds(bounds, zoom, "radar")

    val baseLayoutParams = MapUtils.getTileLayoutParams(zoom, "base")
    val textLayoutParams = MapUtils.getTileLayoutParams(zoom, "text")
    val radarLayoutParams = MapUtils.getTileLayoutParams(zoom, "radar")

    Box(modifier = Modifier
        .fillMaxSize()
        .graphicsLayer(scaleX = scale, scaleY = scale)
    ) {
        if (baseLayoutParams != null) {
            RenderTiles(baseMinX, baseMaxX, baseMinY, baseMaxY, baseLayoutParams, bounds) { x, y ->
                MapUtils.getBaseMapUrl(zoom, x, y)
            }
        }

        if (radarLayoutParams != null) {
            RenderTiles(radarMinX, radarMaxX, radarMinY, radarMaxY, radarLayoutParams, bounds, alpha = 0.5f) { x, y ->
                MapUtils.getRadarMapUrl(layer, zoom, x, y)
            }
        }

        if (textLayoutParams != null) {
            RenderTiles(textMinX, textMaxX, textMinY, textMaxY, textLayoutParams, bounds) { x, y ->
                MapUtils.getCityNamesMapUrl(zoom, x, y)
            }
        }
    }
}

@Composable
fun RenderTiles(
    minX: Int, maxX: Int, minY: Int, maxY: Int,
    layoutParams: MapUtils.TileLayoutParams,
    bounds: Pair<ProjCoordinate, ProjCoordinate>,
    alpha: Float = 1f,
    urlProvider: (Int, Int) -> String
) {
    for (y in minY..maxY) {
        for (x in minX..maxX) {
            // Calculate offset relative to the bounds Top-Left
            // bounds.first.x is the Projected X of the Left edge of the Box.
            // layoutParams.topLeftCornerX is the Projected X of the Tile Grid Origin.
            // (x * tileWidth) is the pixel offset of the tile in the Grid.
            // We need to subtract the pixel offset of the Box Left edge from the Grid Origin.
            
            // Formula from original RadarMap:
            // val left = (x * baseLayoutParams.tileWidth) - (projectedBounds.first.x - baseLayoutParams.topLeftCornerX) / baseLayoutParams.resolution
            // This is correct.
            
            val left = (x * layoutParams.tileWidth) - (bounds.first.x - layoutParams.topLeftCornerX) / layoutParams.resolution
            val top = (y * layoutParams.tileHeight) - (layoutParams.topLeftCornerY - bounds.second.y) / layoutParams.resolution
            
            AsyncImage(
                model = urlProvider(x, y),
                contentDescription = null,
                modifier = Modifier
                    .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                    .alpha(alpha)
            )
        }
    }
}