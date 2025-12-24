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
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dev.isalazy.meteocanada.UserPreferences.Companion.MONTREAL_LAT
import dev.isalazy.meteocanada.UserPreferences.Companion.MONTREAL_LON
import dev.isalazy.meteocanada.data.WeatherRepository
import dev.isalazy.meteocanada.ui.MapUtils
import dev.isalazy.meteocanada.ui.composables.RadarMap
import dev.isalazy.meteocanada.ui.theme.MeteoCanadaTheme
import dev.isalazy.meteocanada.viewmodel.WeatherViewModel
import dev.isalazy.meteocanada.viewmodel.WeatherViewModelFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var userPreferences: UserPreferences
    private val repository = WeatherRepository() // Simple manual injection for now

    private val viewModel: WeatherViewModel by viewModels {
        WeatherViewModelFactory(repository, userPreferences)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                getLocation()
            } else {
                Log.e("LocationData", "Permission denied for ACCESS_COARSE_LOCATION")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        userPreferences = UserPreferences(this)

        // Set initial locale based on saved preference
        setLocale(userPreferences.appLanguage)

        enableEdgeToEdge()
        setContent {
            MeteoCanadaTheme(darkTheme = userPreferences.isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val weatherData by viewModel.weatherData.collectAsState()
                    val isRefreshing by viewModel.isRefreshing.collectAsState()

                    NavHost(navController = navController, startDestination = "weather") {
                        composable("weather") {
                            WeatherScreen(
                                weatherData = weatherData,
                                navController = navController,
                                isRefreshing = isRefreshing,
                                onRefresh = { refreshWeather() },
                                onCitySelected = {
                                    userPreferences.selectedCity = it
                                    if (it == null) {
                                        requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                    } else {
                                        viewModel.loadWeather(it.lat, it.lon)
                                    }
                                },
                                onAddCity = {
                                    userPreferences.addCity(it)
                                    userPreferences.selectedCity = it
                                    viewModel.loadWeather(it.lat, it.lon)
                                },
                                onRequestPermission = { requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                                viewModel = viewModel
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                navController = navController,
                                onLanguageChange = { recreate() }
                            )
                        }
                        composable("radar") {
                            weatherData?.let {
                                RadarScreen(navController = navController, lat = it.latitude, lon = it.longitude)
                            }
                        }
                    }
                }
            }
        }

        if (userPreferences.locationMode == "detect") {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        } else if (userPreferences.isLocationSet()) {
            val (lat, lon) = userPreferences.getLocation()
            viewModel.loadWeather(lat.toDouble(), lon.toDouble())
        } else {
            viewModel.loadWeather(MONTREAL_LAT.toDouble(), MONTREAL_LON.toDouble())
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        val prefs = newBase?.getSharedPreferences("meteo_canada_prefs", MODE_PRIVATE)
        val lang = prefs?.getString("app_language", "en") ?: "en"
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
                    viewModel.loadWeather(location.latitude, location.longitude)
                } else {
                    Log.e("LocationData", "Location not found: FusedLocationProviderClient returned null")
                    viewModel.loadWeather(45.529, -73.562)
                }
            }
            .addOnFailureListener { e ->
                Log.e("LocationData", "Failed to get location: ${e.message}", e)
                viewModel.loadWeather(45.529, -73.562)
            }
    }

    private fun refreshWeather() {
        if (userPreferences.locationMode == "detect") {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        } else if (userPreferences.isLocationSet()) {
            val (lat, lon) = userPreferences.getLocation()
            viewModel.loadWeather(lat.toDouble(), lon.toDouble(), forceRefresh = true)
        } else {
            viewModel.loadWeather(MONTREAL_LAT.toDouble(), MONTREAL_LON.toDouble(), forceRefresh = true)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    weatherData: WeatherData?,
    navController: NavController,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onCitySelected: (UserPreferences.SavedCity?) -> Unit,
    onAddCity: (UserPreferences.SavedCity) -> Unit,
    onRequestPermission: () -> Unit,
    viewModel: WeatherViewModel, // Pass ViewModel to screen
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showAddCityDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    var cities by remember { mutableStateOf(userPreferences.getCities()) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                CityManagementDrawer(
                    cities = cities,
                    selectedCity = userPreferences.selectedCity,
                    onCitySelected = { city ->
                        onCitySelected(city)
                        if (city == null) {
                            onRequestPermission()
                        }
                        scope.launch { drawerState.close() }
                    },
                    onAddCityClicked = {
                        showAddCityDialog = true
                    },
                    onCityRemoved = { cityToRemove ->
                        userPreferences.removeCity(cityToRemove)
                        cities = userPreferences.getCities() // Refresh the list
                        if (userPreferences.selectedCity == cityToRemove) {
                            userPreferences.selectedCity = null // Deselect if removed
                            onRequestPermission() // Revert to detect location
                        }
                    }
                )
            }
        }
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = modifier
        ) {
            LazyColumn(modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .windowInsetsPadding(WindowInsets.statusBars)) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                            weatherData?.let {
                                Text(
                                    text = stringResource(R.string.location, it.location),
                                    style = MaterialTheme.typography.headlineMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } ?: run {
                                Text(text = stringResource(id = R.string.loading_weather_data), style = MaterialTheme.typography.headlineMedium)
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
                                Text(text = stringResource(R.string.current_conditions), style = MaterialTheme.typography.headlineSmall)
                                Text(text = "${weatherData.currentCondition}, ${weatherData.currentTemperature}°C${weatherData.currentFeelsLike?.let { " ($it°C)" } ?: ""}")
                                Text(text = stringResource(R.string.observation_time, weatherData.observationTime))
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
                        Text(text = stringResource(R.string.daily_forecast), style = MaterialTheme.typography.headlineSmall)
                    }
                    items(weatherData.dailyForecasts) { forecast ->
                        Card(modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)) {
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
                                        Text(text = "($it°C)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = stringResource(R.string.hourly_forecast), style = MaterialTheme.typography.headlineSmall)
                    }
                    items(weatherData.hourlyForecasts) { forecast ->
                        Card(modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)) {
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
                                        Text(text = "($it°C)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCityDialog) {
        AddCityDialog(
            viewModel = viewModel,
            onDismiss = { showAddCityDialog = false },
            onCitySelected = {
                onAddCity(it)
                cities = userPreferences.getCities()
                showAddCityDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityManagementDrawer(
    cities: List<UserPreferences.SavedCity>,
    selectedCity: UserPreferences.SavedCity?,
    onCitySelected: (UserPreferences.SavedCity?) -> Unit,
    onAddCityClicked: () -> Unit,
    onCityRemoved: (UserPreferences.SavedCity) -> Unit
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text(text = stringResource(R.string.cities), style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCitySelected(null) }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.detect_my_location), modifier = Modifier.weight(1f))
            if (selectedCity == null) {
                Icon(Icons.Default.Check, contentDescription = "Selected")
            }
        }
        cities.forEach { city ->
            key(city) { // Added key here
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { dismissValue ->
                        if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                            onCityRemoved(city)
                            true
                        } else {
                            false
                        }
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        val color = when (dismissState.targetValue) {
                            SwipeToDismissBoxValue.Settled -> Color.Transparent
                            SwipeToDismissBoxValue.EndToStart -> Color.Red
                            SwipeToDismissBoxValue.StartToEnd -> Color.Red
                        }
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(color)
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete_city),
                                tint = Color.White
                            )
                        }
                    },
                    enableDismissFromEndToStart = true,
                    enableDismissFromStartToEnd = true
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { onCitySelected(city) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = city.displayName, modifier = Modifier.weight(1f))
                        if (selectedCity?.displayName == city.displayName) {
                            Icon(Icons.Default.Check, contentDescription = "Selected")
                        }
                    }
                }
            } // Closed key here
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAddCityClicked() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_city))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.add_city))
        }
    }
}

@Composable
fun AddCityDialog(
    viewModel: WeatherViewModel,
    onDismiss: () -> Unit,
    onCitySelected: (UserPreferences.SavedCity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    // Observe the search results from the ViewModel
    val searchResults by viewModel.citySearchResults.collectAsState()

    // Clear previous results when dialog opens
    LaunchedEffect(Unit) {
        viewModel.clearSearchResults()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.add_city), style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.searchCities(it)
                    },
                    label = { Text(stringResource(R.string.add_city)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(searchResults) {
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onCitySelected(
                                    UserPreferences.SavedCity(
                                        displayName = it.displayName,
                                        lat = it.lat,
                                        lon = it.lon
                                    )
                                )
                            }
                            .padding(vertical = 12.dp)) {
                            Text(it.displayName)
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
        // Mocking ViewModel or handling Preview would require a fake repo/viewModel or just separate the UI further.
        // For now, I'll comment out the preview or leave it broken as it needs a ViewModel instance.
    }
}

@Composable
fun SettingsScreen(
    navController: NavController,
    onLanguageChange: () -> Unit
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    var selectedLanguage by remember { mutableStateOf(userPreferences.appLanguage) }
    var isDarkMode by remember { mutableStateOf(userPreferences.isDarkMode) }
    var radarHistory by remember { mutableIntStateOf(userPreferences.radarHistory) }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
        .windowInsetsPadding(WindowInsets.statusBars)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.settings), style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.select_language), style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    selectedLanguage = "en"
                    userPreferences.appLanguage = "en"
                    onLanguageChange()
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selectedLanguage == "en",
                onClick = {
                    selectedLanguage = "en"
                    userPreferences.appLanguage = "en"
                    onLanguageChange()
                }
            )
            Text("English")
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    selectedLanguage = "fr"
                    userPreferences.appLanguage = "fr"
                    onLanguageChange()
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selectedLanguage == "fr",
                onClick = {
                    selectedLanguage = "fr"
                    userPreferences.appLanguage = "fr"
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
            Text(text = stringResource(R.string.dark_mode), style = MaterialTheme.typography.headlineSmall)
            Switch(
                checked = isDarkMode,
                onCheckedChange = {
                    isDarkMode = it
                    userPreferences.isDarkMode = it
                    onLanguageChange() // Trigger theme change
                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.radar_history), style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier
            .fillMaxWidth()
            .clickable {
                radarHistory = 1
                userPreferences.radarHistory = 1
            }, verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = radarHistory == 1,
                onClick = {
                    radarHistory = 1
                    userPreferences.radarHistory = 1
                }
            )
            Text(stringResource(R.string.one_hour))
        }
        Row(modifier = Modifier
            .fillMaxWidth()
            .clickable {
                radarHistory = 3
                userPreferences.radarHistory = 3
            }, verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = radarHistory == 3,
                onClick = {
                    radarHistory = 3
                    userPreferences.radarHistory = 3
                }
            )
            Text(stringResource(R.string.three_hours))
        }
    }
}

@Composable
fun RadarScreen(navController: NavController, lat: Double, lon: Double) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val layers by produceState(initialValue = emptyList(), userPreferences.radarHistory) {
        value = MapUtils.getRadarLayers(userPreferences.radarHistory)
    }

    val screenWidthPx = LocalWindowInfo.current.containerSize.width
    val screenHeightPx = LocalWindowInfo.current.containerSize.height

    val optimalZoom = remember(screenWidthPx) {
        MapUtils.getOptimalZoomForWidth(screenWidthPx, 50.0)
    }

    var currentLayerIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var isPreFetching by remember { mutableStateOf(true) } // For initial layer
    var isHistoryReady by remember { mutableStateOf(false) } // For all historical layers

    val scope = rememberCoroutineScope() // ensure scope is available

    LaunchedEffect(layers, optimalZoom, screenWidthPx, screenHeightPx) {
        if (layers.isNotEmpty()) {
            isPreFetching = true
            isHistoryReady = false // Reset history readiness

            // Fetch and display only the most recent layer immediately
            val mostRecentLayer = layers.last()
            preFetchRadarTiles(context, listOf(mostRecentLayer), lat, lon, optimalZoom, screenWidthPx, screenHeightPx)
            currentLayerIndex = layers.size - 1 // Start at the most recent layer
            isPreFetching = false // Hide initial loading spinner, show map

            // Launch a background job to pre-fetch the remaining historical layers
            scope.launch {
                val remainingLayers = layers.dropLast(1)
                preFetchRadarTiles(context, remainingLayers, lat, lon, optimalZoom, screenWidthPx, screenHeightPx)
                isHistoryReady = true // All historical layers are now ready
            }
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                delay(500)
                if (layers.isNotEmpty()) {
                    currentLayerIndex = (currentLayerIndex + 1) % layers.size
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.radar), style = MaterialTheme.typography.headlineSmall)
        }
        if (isPreFetching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (layers.isNotEmpty()) {
            Box(modifier = Modifier.weight(1f)) {
                RadarMap(layer = layers[currentLayerIndex], lat = lat, lon = lon, zoom = optimalZoom, modifier = Modifier.fillMaxSize())
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp),
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = formatTimestamp(layers[currentLayerIndex].identifier),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { isPlaying = !isPlaying }, enabled = isHistoryReady) {
                    if (!isHistoryReady) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else if (isPlaying) {
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
                    modifier = Modifier.weight(1f),
                    enabled = isHistoryReady // Enabled by isHistoryReady
                )
            }
        }
    }
}

suspend fun preFetchRadarTiles(
    context: Context,
    layers: List<MapUtils.RadarLayer>,
    lat: Double,
    lon: Double,
    zoom: Int,
    widthPx: Int,
    heightPx: Int
) {
    val imageLoader = ImageLoader(context)
    val requests = mutableListOf<ImageRequest>()

    val projectedBounds = MapUtils.getProjectedBounds(lat, lon, zoom, widthPx, heightPx)
    val (radarMinX, radarMinY, radarMaxX, radarMaxY) = MapUtils.getTileCoordinatesForBounds(projectedBounds, zoom, "radar")

    for (layer in layers) {
        for (y in radarMinY..radarMaxY) {
            for (x in radarMinX..radarMaxX) {
                val url = MapUtils.getRadarMapUrl(layer, zoom, x, y)
                requests.add(ImageRequest.Builder(context).data(url).build())
            }
        }
    }

    coroutineScope {
        requests.map {
            async {
                imageLoader.execute(it)
            }
        }.awaitAll()
    }
}

fun formatTimestamp(identifier: String): String {
    try {
        val prefix = "RADAR_1KM_RRAI_14_"
        if (!identifier.startsWith(prefix)) return ""

        val temp = identifier.removePrefix(prefix)
        val timestampString = temp.substringBeforeLast("_")

        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date: Date? = inputFormat.parse(timestampString)

        if (date != null) {
            val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            return outputFormat.format(date)
        }
        return ""
    } catch (e: Exception) {
        Log.e("TimestampFormat", "Failed to format timestamp: $identifier", e)
        return ""
    }
}