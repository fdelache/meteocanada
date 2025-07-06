package com.example.meteocanada

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.meteocanada.ui.MapUtils
import com.example.meteocanada.ui.composables.RadarMap
import com.example.meteocanada.ui.theme.MeteoCanadaTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale


data class WeatherData(
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val currentCondition: String,
    val currentTemperature: String,
    val wind: String,
    val currentIconUrl: String,
    val currentFeelsLike: String?,
    val dailyForecasts: List<DailyForecast>,
    val hourlyForecasts: List<HourlyForecast>
)

data class DailyForecast(
    val date: String,
    val summary: String,
    val temperature: String,
    val iconCode: String,
    val iconUrl: String,
    val feelsLike: String?
)

data class HourlyForecast(
    val time: String,
    val condition: String,
    val temperature: String,
    val iconCode: String,
    val iconUrl: String,
    val feelsLike: String?
)

class ImageCache(context: Context) {
    private val inMemoryCache = mutableMapOf<String, Bitmap>()
    private val diskCacheDir = File(context.cacheDir, "image_cache")

    init {
        if (!diskCacheDir.exists()) {
            diskCacheDir.mkdirs()
        }
    }

    fun get(url: String): Bitmap? {
        inMemoryCache[url]?.let {
            return it
        }

        val file = File(diskCacheDir, url.hashCode().toString())
        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                inMemoryCache[url] = bitmap
                return bitmap
            }
        }

        return null
    }

    fun put(url: String, bitmap: Bitmap) {
        inMemoryCache[url] = bitmap
        val file = File(diskCacheDir, url.hashCode().toString())
        try {
            FileOutputStream(file).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val weatherDataState = mutableStateOf<WeatherData?>(null)
    private lateinit var imageCache: ImageCache

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                getLocation()
            } else {
                Log.e("LocationData", "Permission denied for ACCESS_COARSE_LOCATION")
                // Handle permission denial
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        imageCache = ImageCache(this)

        // Set initial locale based on saved preference
        val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val lang = sharedPrefs.getString("app_language", "en") ?: "en"
        val isDarkMode = sharedPrefs.getBoolean("dark_mode", false)
        setLocale(lang)

        enableEdgeToEdge()
        setContent {
            MeteoCanadaTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "weather") {
                        composable("weather") {
                            WeatherScreen(
                                weatherData = weatherDataState.value,
                                navController = navController,
                                imageCache = imageCache
                            )
                        }
                        composable("settings") {
                            SettingsScreen(navController = navController) {
                                recreate()
                            }
                        }
                        composable("radar") {
                            weatherDataState.value?.let {
                                RadarScreen(navController = navController, lat = it.latitude, lon = it.longitude)
                            }
                        }
                    }
                }
            }
        }

        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getLocation()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        val sharedPrefs = newBase?.getSharedPreferences("app_prefs", MODE_PRIVATE)
        val lang = sharedPrefs?.getString("app_language", "en") ?: "en"
        val locale = LocaleListCompat.forLanguageTags(lang)
        val config = Configuration(newBase?.resources?.configuration)
        ConfigurationCompat.setLocales(config, locale)
        val context = newBase?.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    private fun setLocale(languageCode: String) {
        val locale = Locale.Builder().setLanguage(languageCode).build()
        Locale.setDefault(locale)
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    Log.d("LocationData", "Location received: ${location.latitude}, ${location.longitude}")
                    fetchWeather(location.latitude, location.longitude)
                } else {
                    Log.e("LocationData", "Location not found: FusedLocationProviderClient returned null")
                    fetchWeather(45.529, -73.562)
                }
            }
            .addOnFailureListener { e ->
                Log.e("LocationData", "Failed to get location: ${e.message}", e)
                fetchWeather(45.529, -73.562)
            }
    }

    private fun fetchWeather(latitude: Double, longitude: Double, isRetry: Boolean = false) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                val lang = sharedPrefs.getString("app_language", "en") ?: "en"
                val url = URL("https://meteo.gc.ca/api/app/v3/$lang/Location/$latitude,$longitude?type=city")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                Log.d("WeatherData", response)
                val weatherData = parseWeatherData(response, latitude, longitude)
                weatherDataState.value = weatherData
            } catch (e: Exception) {
                Log.e("WeatherData", "Failed to fetch weather data for $latitude,$longitude: ${e::class.simpleName}: ${e.message}", e)
                if (!isRetry) {
                    Log.d("WeatherData", "Retrying with fallback location 45.529, -73.562")
                    fetchWeather(45.529, -73.562, true)
                }
            }
        }
    }

    private fun parseWeatherData(json: String, latitude: Double, longitude: Double): WeatherData {
        val jsonArray = org.json.JSONArray(json)
        val jsonObject = jsonArray.getJSONObject(0)
        val location = jsonObject.getString("displayName")
        val observation = jsonObject.getJSONObject("observation")
        val currentCondition = observation.getString("condition")
        val currentTemperature = observation.getJSONObject("temperature").getString("metric")
        val currentFeelsLike = observation.optJSONObject("feelsLike")?.getString("metric")
        val wind = "${observation.getString("windDirection")} ${observation.getJSONObject("windSpeed").getString("metric")} km/h"
        val currentIconUrl = "https://meteo.gc.ca/weathericons/${observation.getString("iconCode")}.gif"

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
                    feelsLike = if (feelsLike != temperature) feelsLike else null
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
                    feelsLike = if (feelsLike != temperature) feelsLike else null
                )
            )
        }

        return WeatherData(location, latitude, longitude, currentCondition, currentTemperature, wind, currentIconUrl, if (currentFeelsLike != currentTemperature) currentFeelsLike else null, dailyForecasts, hourlyForecasts)
    }
}


@Composable
fun WeatherScreen(weatherData: WeatherData?, navController: NavController, imageCache: ImageCache, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.padding(16.dp).windowInsetsPadding(WindowInsets.statusBars)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                weatherData?.let {
                    Text(text = stringResource(R.string.location, it.location), style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                } ?: run {
                    Text(text = stringResource(id = R.string.loading_weather_data), style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                }
                IconButton(onClick = { navController.navigate("settings") }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (weatherData != null) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    val imageBitmap: Bitmap? = loadImageBitmap(imageUrl = weatherData.currentIconUrl, imageCache = imageCache)
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap.asImageBitmap(),
                            contentDescription = weatherData.currentCondition,
                            modifier = Modifier.size(72.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.current_conditions), style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
                        Text(text = "${weatherData.currentCondition}, ${weatherData.currentTemperature}°C${weatherData.currentFeelsLike?.let { " ($it°C)" } ?: ""}")
                        Text(text = stringResource(R.string.wind, weatherData.wind))
                    }
                    IconButton(onClick = { navController.navigate("radar") }) {
                        Icon(
                            painterResource(
                                id = R.drawable.map_24px
                            ), contentDescription = stringResource(R.string.radar)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Text(text = stringResource(R.string.daily_forecast), style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
            }
            items(weatherData.dailyForecasts) { forecast ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val imageBitmap: Bitmap? = loadImageBitmap(imageUrl = forecast.iconUrl, imageCache = imageCache)
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap.asImageBitmap(),
                                contentDescription = forecast.summary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = forecast.date, modifier = Modifier.weight(1f))
                        Text(text = forecast.summary, modifier = Modifier.weight(2f))
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text(text = "${forecast.temperature}°C")
                            forecast.feelsLike?.let {
                                Text(text = "($it°C)", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = stringResource(R.string.hourly_forecast), style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
            }
            items(weatherData.hourlyForecasts) { forecast ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val imageBitmap: Bitmap? = loadImageBitmap(imageUrl = forecast.iconUrl, imageCache = imageCache)
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap.asImageBitmap(),
                                contentDescription = forecast.condition,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = forecast.time, modifier = Modifier.weight(1f))
                        Text(text = forecast.condition, modifier = Modifier.weight(2f))
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text(text = "${forecast.temperature}°C")
                            forecast.feelsLike?.let {
                                Text(text = "($it°C)", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun loadImageBitmap(imageUrl: String, imageCache: ImageCache): Bitmap? {
    val bitmapState: State<Bitmap?> = produceState(initialValue = imageCache.get(imageUrl), imageUrl) {
        if (value == null) {
            value = try {
                withContext(Dispatchers.IO) {
                    val url = URL(imageUrl)
                    var connection: HttpURLConnection? = null
                    var inputStream: java.io.InputStream? = null
                    try {
                        connection = url.openConnection() as HttpURLConnection
                        connection.doInput = true
                        connection.connect()
                        val responseCode = connection.responseCode
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            inputStream = connection.inputStream
                            if (inputStream != null) {
                                val bufferedInputStream = java.io.BufferedInputStream(inputStream)
                                val bitmap = BitmapFactory.decodeStream(bufferedInputStream)
                                if (bitmap != null) {
                                    imageCache.put(imageUrl, bitmap)
                                } else {
                                    Log.e("ImageLoader", "BitmapFactory.decodeStream returned null for $imageUrl")
                                }
                                bitmap
                            } else {
                                Log.e("ImageLoader", "Input stream is null for $imageUrl")
                                null
                            }
                        } else {
                            Log.e("ImageLoader", "HTTP error code: $responseCode, message: ${connection.responseMessage} for $imageUrl")
                            null
                        }
                    } finally {
                        inputStream?.close()
                        connection?.disconnect()
                    }
                }
            } catch (e: Exception) {
                Log.e("ImageLoader", "Failed to load image from $imageUrl: ${e.message}", e)
                null
            }
        }
    }
    return bitmapState.value
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MeteoCanadaTheme {
        val navController = rememberNavController()
        val context = LocalContext.current
        val imageCache = remember { ImageCache(context) }
        WeatherScreen(
            weatherData = WeatherData(
                location = "Montreal",
                latitude = 45.5017,
                longitude = -73.5673,
                currentCondition = "Partly Cloudy",
                currentTemperature = "20",
                currentFeelsLike = "25",
                wind = "SW 10 km/h",
                currentIconUrl = "https://meteo.gc.ca/weathericons/00.gif",
                dailyForecasts = listOf(
                    DailyForecast("Mon", "Sunny", "25", "00", "https://meteo.gc.ca/weathericons/00.gif", null),
                    DailyForecast("Tue", "Rain", "15", "12", "https://meteo.gc.ca/weathericons/12.gif", null)
                ),
                hourlyForecasts = listOf(
                    HourlyForecast("10h00", "Sunny", "22", "00", "https://meteo.gc.ca/weathericons/00.gif", null),
                    HourlyForecast("11h00", "Sunny", "23", "00", "https://meteo.gc.ca/weathericons/00.gif", null)
                )
            ),
            navController = navController,
            imageCache = imageCache
        )
    }
}

@Composable
fun SettingsScreen(navController: NavController, onLanguageChange: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    var selectedLanguage by remember { mutableStateOf(sharedPrefs.getString("app_language", "en")) }
    var isDarkMode by remember { mutableStateOf(sharedPrefs.getBoolean("dark_mode", false)) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).windowInsetsPadding(WindowInsets.statusBars)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.settings), style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.select_language), style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clickable { 
                selectedLanguage = "en"
                sharedPrefs.edit { putString("app_language", "en") }
                onLanguageChange()
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selectedLanguage == "en",
                onClick = { 
                    selectedLanguage = "en"
                    sharedPrefs.edit { putString("app_language", "en") }
                    onLanguageChange()
                }
            )
            Text("English")
        }
        Row(
            modifier = Modifier.fillMaxWidth().clickable { 
                selectedLanguage = "fr"
                sharedPrefs.edit { putString("app_language", "fr") }
                onLanguageChange()
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selectedLanguage == "fr",
                onClick = { 
                    selectedLanguage = "fr"
                    sharedPrefs.edit { putString("app_language", "fr") }
                    onLanguageChange()
                }
            )
            Text("Français")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = stringResource(R.string.dark_mode), style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
            Switch(
                checked = isDarkMode,
                onCheckedChange = {
                    isDarkMode = it
                    sharedPrefs.edit { putBoolean("dark_mode", it) }
                    onLanguageChange() // Trigger theme change
                }
            )
        }
    }
}

@Composable
fun RadarScreen(navController: NavController, lat: Double, lon: Double) {
    val layers by produceState(initialValue = emptyList()) {
        value = MapUtils.getRadarLayers()
    }

    val screenWidthPx = LocalWindowInfo.current.containerSize.width
    val optimalZoom = remember(screenWidthPx) {
        MapUtils.getOptimalZoomForWidth(screenWidthPx, 50.0)
    }

    var currentLayerIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(layers) {
        if (layers.isNotEmpty()) {
            currentLayerIndex = layers.size - 1
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                delay(100)
                if (layers.isNotEmpty()) {
                    currentLayerIndex = (currentLayerIndex + 1) % layers.size
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).windowInsetsPadding(WindowInsets.statusBars)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.radar), style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
        }
        if (layers.isNotEmpty()) {
            RadarMap(layer = layers[currentLayerIndex], lat = lat, lon = lon, zoom = optimalZoom, modifier = Modifier.weight(1f), scale = 2f)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { isPlaying = !isPlaying }) {
                    if (isPlaying) {
                        Icon(
                            painterResource(
                                id = R.drawable.pause_24px
                            ), contentDescription = "Pause"
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play"
                        )
                    }
                }
                Slider(
                    value = currentLayerIndex.toFloat(),
                    onValueChange = { currentLayerIndex = it.toInt() },
                    valueRange = 0f..(layers.size - 1).toFloat(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}