package dev.isalazy.meteocanada

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dev.isalazy.meteocanada.UserPreferences.Companion.MONTREAL_LAT
import dev.isalazy.meteocanada.UserPreferences.Companion.MONTREAL_LON
import dev.isalazy.meteocanada.UserPreferences.Companion.TORONTO_LAT
import dev.isalazy.meteocanada.UserPreferences.Companion.TORONTO_LON
import dev.isalazy.meteocanada.ui.MapUtils
import dev.isalazy.meteocanada.ui.composables.RadarMap
import dev.isalazy.meteocanada.ui.theme.MeteoCanadaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
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

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val weatherDataState = mutableStateOf<WeatherData?>(null)
    private lateinit var userPreferences: UserPreferences

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
        installSplashScreen()
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        userPreferences = UserPreferences(this)

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
                                onLocationSelected = { location ->
                                    when (location) {
                                        "montreal" -> {
                                            userPreferences.setLocation(MONTREAL_LAT, MONTREAL_LON)
                                            fetchWeather(MONTREAL_LAT.toDouble(), MONTREAL_LON.toDouble())
                                        }
                                        "toronto" -> {
                                            userPreferences.setLocation(TORONTO_LAT, TORONTO_LON)
                                            fetchWeather(TORONTO_LAT.toDouble(), TORONTO_LON.toDouble())
                                        }
                                        "detect" -> {
                                            requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                        }
                                    }
                                }
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

        if (userPreferences.isLocationSet()) {
            val (lat, lon) = userPreferences.getLocation()
            fetchWeather(lat.toDouble(), lon.toDouble())
        } else {
            fetchWeather(MONTREAL_LAT.toDouble(), MONTREAL_LON.toDouble())
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
                    userPreferences.setLocation(location.latitude.toFloat(), location.longitude.toFloat())
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

        return WeatherData(location, latitude, longitude, currentCondition, currentTemperature, wind, currentIconUrl, if (currentFeelsLike != currentTemperature && !currentFeelsLike.isNullOrBlank()) currentFeelsLike else null, dailyForecasts, hourlyForecasts)
    }
}


@Composable
fun WeatherScreen(weatherData: WeatherData?, navController: NavController, onLocationSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.select_location)) },
            text = {
                Column {
                    TextButton(onClick = {
                        onLocationSelected("montreal")
                        showDialog = false
                    }) {
                        Text(stringResource(R.string.montreal))
                    }
                    TextButton(onClick = {
                        onLocationSelected("toronto")
                        showDialog = false
                    }) {
                        Text(stringResource(R.string.toronto))
                    }
                    TextButton(onClick = {
                        onLocationSelected("detect")
                        showDialog = false
                    }) {
                        Text(stringResource(R.string.detect_my_location))
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    LazyColumn(modifier = modifier.padding(16.dp).windowInsetsPadding(WindowInsets.statusBars)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    weatherData?.let {
                        Text(text = stringResource(R.string.location, it.location), style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                    } ?: run {
                        Text(text = stringResource(id = R.string.loading_weather_data), style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                    }
                    IconButton(onClick = { showDialog = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.edit_24px),
                            contentDescription = stringResource(R.string.select_location)
                        )
                    }
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
                    AsyncImage(
                        model = weatherData.currentIconUrl,
                        contentDescription = weatherData.currentCondition,
                        modifier = Modifier.size(72.dp)
                    )
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
                        AsyncImage(
                            model = forecast.iconUrl,
                            contentDescription = forecast.summary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = forecast.date, modifier = Modifier.weight(1f))
                        Text(text = "${forecast.summary}${if (forecast.precip.isNotEmpty() && forecast.precip != "0") " (${forecast.precip}%)" else ""}", modifier = Modifier.weight(2f))
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
                        AsyncImage(
                            model = forecast.iconUrl,
                            contentDescription = forecast.condition,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = forecast.time, modifier = Modifier.weight(1f))
                        Text(text = "${forecast.condition}${if (forecast.precip.isNotEmpty() && forecast.precip != "0") " (${forecast.precip}%)" else ""}", modifier = Modifier.weight(2f))
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



@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MeteoCanadaTheme {
        val navController = rememberNavController()
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
                    DailyForecast("Mon", "Sunny", "25", "00", "https://meteo.gc.ca/weathericons/00.gif", null, "0"),
                    DailyForecast("Tue", "Rain", "15", "12", "https://meteo.gc.ca/weathericons/12.gif", null, "80")
                ),
                hourlyForecasts = listOf(
                    HourlyForecast("10h00", "Sunny", "22", "00", "https://meteo.gc.ca/weathericons/00.gif", null, "0"),
                    HourlyForecast("11h00", "Sunny", "23", "00", "https://meteo.gc.ca/weathericons/00.gif", null, "0")
                )
            ),
            navController = navController,
            onLocationSelected = {}
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