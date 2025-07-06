package com.example.meteocanada.ui

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateReferenceSystem
import org.locationtech.proj4j.CoordinateTransform
import org.locationtech.proj4j.CoordinateTransformFactory
import org.locationtech.proj4j.ProjCoordinate
import org.xmlpull.v1.XmlPullParser
import java.net.URL

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

object MapUtils {

    private val crsFactory = CRSFactory()
    private val ctFactory = CoordinateTransformFactory()

    private const val EPSG_4326 = "EPSG:4326" // WGS84
    private const val EPSG_3978 = "EPSG:3978" // NAD83 / Canada Atlas Lambert

    private val wgs84: CoordinateReferenceSystem = crsFactory.createFromName(EPSG_4326)
    private val nad83: CoordinateReferenceSystem = crsFactory.createFromName(EPSG_3978)

    private val transform: CoordinateTransform = ctFactory.createTransform(wgs84, nad83)

    data class TileMatrix(
        val identifier: String,
        val scaleDenominator: Double,
        val topLeftCornerX: Double,
        val topLeftCornerY: Double,
        val tileWidth: Int,
        val tileHeight: Int,
        val matrixWidth: Int,
        val matrixHeight: Int
    )

    private val radarTileMatrixSet = listOf(
        TileMatrix("00", 1.370166430809052E8, -7959420.0, 4646500.0, 512, 512, 1, 1),
        TileMatrix("01", 8.03201011163927E7, -7959420.0, 4646500.0, 512, 512, 2, 1),
        TileMatrix("02", 4.724711830376042E7, -7959420.0, 4646500.0, 512, 512, 2, 2),
        TileMatrix("03", 2.834827098225625E7, -7959420.0, 4646500.0, 512, 512, 4, 3),
        TileMatrix("04", 1.6536491406316146E7, -7959420.0, 4646500.0, 512, 512, 6, 4),
        TileMatrix("05", 9449423.660752084, -7959420.0, 4646500.0, 512, 512, 10, 7),
        TileMatrix("06", 5669654.1964512495, -7959420.0, 4646500.0, 512, 512, 16, 11),
        TileMatrix("07", 3307298.281263229, -7959420.0, 4646500.0, 512, 512, 27, 19),
        TileMatrix("08", 1889884.7321504168, -7959420.0, 4646500.0, 512, 512, 46, 32),
        TileMatrix("09", 1133930.83929025, -7959420.0, 4646500.0, 512, 512, 77, 53),
        TileMatrix("10", 661459.6562526459, -7959420.0, 4646500.0, 512, 512, 131, 91),
        TileMatrix("11", 396875.7937515875, -7959420.0, 4646500.0, 512, 512, 218, 151),
        TileMatrix("12", 236235.5915188021, -7959420.0, 4646500.0, 512, 512, 365, 254)
    )

    private val baseMapTileMatrixSet = listOf(
        TileMatrix("0", 1.37016643080888E8, -3.46558E7, 3.931E7, 256, 256, 4, 5),
        TileMatrix("1", 8.032010111638261E7, -3.46558E7, 3.931E7, 256, 256, 7, 7),
        TileMatrix("2", 4.724711830375448E7, -3.46558E7, 3.931E7, 256, 256, 12, 12),
        TileMatrix("3", 2.8348270982252687E7, -3.46558E7, 3.931E7, 256, 256, 19, 20),
        TileMatrix("4", 1.653649140631407E7, -3.46558E7, 3.931E7, 256, 256, 33, 34),
        TileMatrix("5", 9449423.660750896, -3.46558E7, 3.931E7, 256, 256, 57, 60),
        TileMatrix("6", 5669654.196450537, -3.46558E7, 3.931E7, 256, 256, 95, 100),
        TileMatrix("7", 3307298.2812628136, -3.46558E7, 3.931E7, 256, 256, 162, 170),
        TileMatrix("8", 1889884.7321501793, -3.46558E7, 3.931E7, 256, 256, 283, 298),
        TileMatrix("9", 1133930.8392901076, -3.46558E7, 3.931E7, 256, 256, 471, 496),
        TileMatrix("10", 661459.6562525628, -3.46558E7, 3.931E7, 256, 256, 807, 849),
        TileMatrix("11", 396875.7937515376, -3.46558E7, 3.931E7, 256, 256, 1345, 1415),
        TileMatrix("12", 236235.59151877242, -3.46558E7, 3.931E7, 256, 256, 2259, 2377),
        TileMatrix("13", 137016.643080888, -3.46558E7, 3.931E7, 256, 256, 3894, 4098),
        TileMatrix("14", 80320.10111638262, -3.46558E7, 3.931E7, 256, 256, 6642, 6991),
        TileMatrix("15", 47247.118303754476, -3.46558E7, 3.931E7, 256, 256, 11292, 11884),
        TileMatrix("16", 28348.27098225269, -3.46558E7, 3.931E7, 256, 256, 18819, 19807),
        TileMatrix("17", 16536.491406314068, -3.46558E7, 3.931E7, 256, 256, 32261, 33954),
        TileMatrix("18", 9449.423660750896, -3.46558E7, 3.931E7, 256, 256, 56457, 59420),
        TileMatrix("19", 5669.654196450538, -3.46558E7, 3.931E7, 256, 256, 94094, 99032)
    )

    private val cityTextTileMatrixSet = listOf(
        TileMatrix("0", 1.37016643080888E8, -34655800.0, 39310000.0, 256, 256, 5, 5),
        TileMatrix("1", 8.032010111638261E7, -34655800.0, 39310000.0, 256, 256, 7, 8),
        TileMatrix("2", 4.724711830375448E7, -34655800.0, 39310000.0, 256, 256, 12, 14),
        TileMatrix("3", 2.8348270982252687E7, -34655800.0, 39310000.0, 256, 256, 20, 22),
        TileMatrix("4", 1.653649140631407E7, -34655800.0, 39310000.0, 256, 256, 34, 38),
        TileMatrix("5", 9449423.660750896, -34655800.0, 39310000.0, 256, 256, 59, 66),
        TileMatrix("6", 5669654.196450537, -34655800.0, 39310000.0, 256, 256, 98, 110),
        TileMatrix("7", 3307298.2812628136, -34655800.0, 39310000.0, 256, 256, 167, 188),
        TileMatrix("8", 1889884.7321501793, -34655800.0, 39310000.0, 256, 256, 292, 329),
        TileMatrix("9", 1133930.8392901076, -34655800.0, 39310000.0, 256, 256, 487, 548),
        TileMatrix("10", 661459.6562525628, -34655800.0, 39310000.0, 256, 256, 834, 938),
        TileMatrix("11", 396875.7937515376, -34655800.0, 39310000.0, 256, 256, 1389, 1563),
        TileMatrix("12", 236235.59151877242, -34655800.0, 39310000.0, 256, 256, 2334, 2626),
        TileMatrix("13", 137016.643080888, -34655800.0, 39310000.0, 256, 256, 4023, 4528),
        TileMatrix("14", 80320.10111638262, -34655800.0, 39310000.0, 256, 256, 6863, 7723),
        TileMatrix("15", 47247.118303754476, -34655800.0, 39310000.0, 256, 256, 11666, 13130),
        TileMatrix("16", 28348.27098225269, -34655800.0, 39310000.0, 256, 256, 19443, 21882),
        TileMatrix("17", 16536.491406314068, -34655800.0, 39310000.0, 256, 256, 33331, 37512)
    )

    data class RadarLayer(
        val identifier: String,
        val style: String,
        val tileMatrixSet: String,
        val resourceUrlTemplate: String
    )

    suspend fun getRadarLayers(): List<RadarLayer> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://meteo.gc.ca/api/map/radar.3978/wmts/1.0.0/WMTSCapabilities.xml")
                val inputStream = url.openStream()
                val parser: XmlPullParser = Xml.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(inputStream, null)

                val layers = mutableListOf<RadarLayer>()
                var eventType = parser.eventType

                var inLayer = false
                var inStyle = false
                var inTileMatrixSetLink = false
                var currentTag: String? = null

                var identifier: String? = null
                var style: String? = null
                var tileMatrixSet: String? = null
                var resourceUrlTemplate: String? = null

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            currentTag = parser.name
                            when (currentTag) {
                                "Layer" -> {
                                    inLayer = true
                                    identifier = null
                                    style = null
                                    tileMatrixSet = null
                                    resourceUrlTemplate = null
                                }
                                "Style" -> if (inLayer) inStyle = true
                                "TileMatrixSetLink" -> if (inLayer) inTileMatrixSetLink = true
                                "ResourceURL" -> {
                                    if (inLayer) {
                                        resourceUrlTemplate = parser.getAttributeValue(null, "template")
                                    }
                                }
                            }
                        }
                        XmlPullParser.TEXT -> {
                            if (inLayer && parser.text.isNotBlank()) {
                                val text = parser.text
                                when (currentTag) {
                                    "ows:Identifier" -> {
                                        if (inStyle) {
                                            style = text
                                        } else {
                                            identifier = text
                                        }
                                    }
                                    "TileMatrixSet" -> {
                                        if (inTileMatrixSetLink) {
                                            tileMatrixSet = text
                                        }
                                    }
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            when (parser.name) {
                                "Layer" -> {
                                    if (identifier != null && identifier.startsWith("RADAR_1KM_RRAI_14_") && style != null && tileMatrixSet != null && resourceUrlTemplate != null) {
                                        layers.add(RadarLayer(identifier, style, tileMatrixSet, resourceUrlTemplate))
                                    }
                                    inLayer = false
                                }
                                "Style" -> inStyle = false
                                "TileMatrixSetLink" -> inTileMatrixSetLink = false
                            }
                            currentTag = null
                        }
                    }
                    eventType = parser.next()
                }
                inputStream.close()
                layers.sortedBy { it.identifier }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    fun getTileCoordinatesForBounds(bounds: Pair<ProjCoordinate, ProjCoordinate>, zoom: Int, layer: String): Quadruple<Int, Int, Int, Int> {
        val (min, max) = bounds

        val tileMatrixSet = when (layer) {
            "radar" -> radarTileMatrixSet
            "base" -> baseMapTileMatrixSet
            "text" -> cityTextTileMatrixSet
            else -> return Quadruple(-1, -1, -1, -1)
        }

        if (zoom >= tileMatrixSet.size) {
            return Quadruple(-1, -1, -1, -1) // Invalid zoom
        }
        val tileMatrix = tileMatrixSet[zoom]

        val pixelSize = 0.00028
        val resolution = tileMatrix.scaleDenominator * pixelSize

        val minTileX = ((min.x - tileMatrix.topLeftCornerX) / resolution).toInt() / tileMatrix.tileWidth
        val maxTileX = ((max.x - tileMatrix.topLeftCornerX) / resolution).toInt() / tileMatrix.tileWidth
        val minTileY = ((tileMatrix.topLeftCornerY - max.y) / resolution).toInt() / tileMatrix.tileHeight
        val maxTileY = ((tileMatrix.topLeftCornerY - min.y) / resolution).toInt() / tileMatrix.tileHeight

        return Quadruple(minTileX, minTileY, maxTileX, maxTileY)
    }

    fun getProjectedBounds(lat: Double, lon: Double, zoom: Int, width: Int, height: Int): Pair<ProjCoordinate, ProjCoordinate> {
        val projCoordinate = ProjCoordinate(lon, lat)
        val centerProjected = ProjCoordinate()
        transform.transform(projCoordinate, centerProjected)

        val tileMatrix = radarTileMatrixSet[zoom]
        val pixelSize = 0.00028
        val resolution = tileMatrix.scaleDenominator * pixelSize

        val halfWidthMeters = width / 2.0 * resolution
        val halfHeightMeters = height / 2.0 * resolution

        val minX = centerProjected.x - halfWidthMeters
        val minY = centerProjected.y - halfHeightMeters
        val maxX = centerProjected.x + halfWidthMeters
        val maxY = centerProjected.y + halfHeightMeters

        return Pair(ProjCoordinate(minX, minY), ProjCoordinate(maxX, maxY))
    }

    fun getBaseMapUrl(zoom: Int, x: Int, y: Int): String {
        return "https://geoappext.nrcan.gc.ca/arcgis/rest/services/BaseMaps/CBMT_CBCT_GEOM_3978/MapServer/WMTS/tile/1.0.0/BaseMaps_CBMT_CBCT_GEOM_3978/default/default028mm/$zoom/$y/$x.jpg"
    }

    fun getCityNamesMapUrl(zoom: Int, x: Int, y: Int): String {
        return "https://geoappext.nrcan.gc.ca/arcgis/rest/services/BaseMaps/CBCT_TXT_3978/MapServer/WMTS/tile/1.0.0/BaseMaps_CBCT_TXT_3978/default/default028mm/$zoom/$y/$x.png"
    }

    fun getRadarMapUrl(layer: RadarLayer, zoom: Int, x: Int, y: Int): String {
        val zoomStr = zoom.toString().padStart(2, '0')
        return layer.resourceUrlTemplate
            .replace("{Style}", layer.style)
            .replace("{TileMatrixSet}", layer.tileMatrixSet)
            .replace("{TileMatrix}", zoomStr)
            .replace("{TileCol}", x.toString())
            .replace("{TileRow}", y.toString())
    }

    data class TileLayoutParams(
        val tileWidth: Int,
        val tileHeight: Int,
        val resolution: Double,
        val topLeftCornerX: Double,
        val topLeftCornerY: Double
    )

    fun getTileLayoutParams(zoom: Int, layer: String): TileLayoutParams? {
        val tileMatrixSet = when (layer) {
            "radar" -> radarTileMatrixSet
            "base" -> baseMapTileMatrixSet
            "text" -> cityTextTileMatrixSet
            else -> return null
        }

        if (zoom >= tileMatrixSet.size) {
            return null // Invalid zoom
        }
        val tileMatrix = tileMatrixSet[zoom]

        // Standard pixel size in meters, as defined by OGC.
        val pixelSize = 0.00028
        val resolution = tileMatrix.scaleDenominator * pixelSize

        return TileLayoutParams(
            tileWidth = tileMatrix.tileWidth,
            tileHeight = tileMatrix.tileHeight,
            resolution = resolution,
            topLeftCornerX = tileMatrix.topLeftCornerX,
            topLeftCornerY = tileMatrix.topLeftCornerY
        )
    }
}