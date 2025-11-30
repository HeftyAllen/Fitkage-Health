package com.example.fitkagehealth

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitkagehealth.adapters.WeatherActivityAdapter
import com.example.fitkagehealth.databinding.ActivityWeatherBinding
import com.example.fitkagehealth.model.WeatherData
import com.google.android.gms.location.*
import org.json.JSONObject
import com.example.fitkagehealth.model.WeatherActivity
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class WeatherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWeatherBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private var currentLocation: Location? = null

    private val API_KEY = "16f0dedaf11e602dea2c265b7894baa1" // Your API key
    private val PERMISSION_ID = 1010
    private val executor = Executors.newSingleThreadExecutor()

    private val weatherActivities = mutableListOf<WeatherActivity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeatherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeViews()
        setupLocationClient()
        checkPermissions()
    }

    private fun initializeViews() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            checkPermissions()
        }

        // Setup activities recycler view
        binding.activitiesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.activitiesRecyclerView.adapter = WeatherActivityAdapter(weatherActivities)
    }

    private fun setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun checkPermissions() {
        if (isPermissionGranted()) {
            if (isLocationEnabled()) {
                getLastLocation()
            } else {
                Toast.makeText(this, "Please enable location services", Toast.LENGTH_LONG).show()
                binding.swipeRefreshLayout.isRefreshing = false
            }
        } else {
            requestPermissions()
        }
    }

    private fun isPermissionGranted(): Boolean {
        return (ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED)
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (isPermissionGranted()) {
            fusedLocationClient.lastLocation.addOnCompleteListener { task ->
                val location = task.result
                if (location != null) {
                    currentLocation = location
                    getWeatherData(location.latitude, location.longitude)
                } else {
                    requestNewLocation()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocation() {
        if (isPermissionGranted()) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        super.onLocationResult(locationResult)
                        currentLocation = locationResult.lastLocation
                        getWeatherData(currentLocation!!.latitude, currentLocation!!.longitude)
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                },
                Looper.getMainLooper()
            )
        }
    }

    private fun getWeatherData(lat: Double, lon: Double) {
        binding.progressBar.visibility = View.VISIBLE
        binding.errorText.visibility = View.GONE
        binding.mainContainer.visibility = View.GONE

        executor.execute {
            try {
                val response = URL("https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&units=metric&appid=$API_KEY")
                    .readText(Charsets.UTF_8)

                runOnUiThread {
                    parseWeatherData(response)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.errorText.visibility = View.VISIBLE
                    binding.swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(this, "Failed to fetch weather data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun parseWeatherData(result: String) {
        try {
            val jsonObj = JSONObject(result)
            val main = jsonObj.getJSONObject("main")
            val sys = jsonObj.getJSONObject("sys")
            val wind = jsonObj.getJSONObject("wind")
            val weather = jsonObj.getJSONArray("weather").getJSONObject(0)
            val visibility = jsonObj.optInt("visibility", 10000)

            val updatedAt: Long = jsonObj.getLong("dt")
            val updatedAtText = "Updated at: " + SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH).format(Date(updatedAt * 1000))
            val temp = main.getString("temp").toFloat().roundToInt().toString() + "°C"
            val feelsLike = "Feels like " + main.getString("feels_like").toFloat().roundToInt().toString() + "°C"
            val tempMin = main.getString("temp_min").toFloat().roundToInt().toString() + "°C"
            val tempMax = main.getString("temp_max").toFloat().roundToInt().toString() + "°C"
            val pressure = main.getString("pressure") + " hPa"
            val humidity = main.getString("humidity") + "%"
            val windSpeed = wind.getString("speed") + " m/s"
            val weatherDescription = weather.getString("description")
            val weatherIcon = weather.getString("icon")
            val weatherId = weather.getInt("id")

            val sunrise: Long = sys.getLong("sunrise")
            val sunset: Long = sys.getLong("sunset")
            val sunriseText = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunrise * 1000))
            val sunsetText = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunset * 1000))

            val address = jsonObj.getString("name") + ", " + sys.getString("country")

            // Create weather data object
            val weatherData = WeatherData(
                address = address,
                updatedAt = updatedAtText,
                temperature = temp,
                feelsLike = feelsLike,
                tempMin = tempMin,
                tempMax = tempMax,
                pressure = pressure,
                humidity = humidity,
                windSpeed = windSpeed,
                weatherDescription = weatherDescription,
                weatherIcon = weatherIcon,
                sunrise = sunriseText,
                sunset = sunsetText,
                visibility = visibility,
                conditionId = weatherId
            )

            updateUI(weatherData)
            generateActivityRecommendations(weatherData)

        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            binding.errorText.visibility = View.VISIBLE
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun updateUI(weatherData: WeatherData) {
        binding.address.text = weatherData.address
        binding.updatedAt.text = weatherData.updatedAt
        binding.status.text = weatherData.weatherDescription.replaceFirstChar { it.uppercase() }
        binding.temp.text = weatherData.temperature
        binding.feelsLike.text = weatherData.feelsLike
        binding.tempMin.text = "L: ${weatherData.tempMin}"
        binding.tempMax.text = "H: ${weatherData.tempMax}"
        binding.sunrise.text = weatherData.sunrise
        binding.sunset.text = weatherData.sunset
        binding.wind.text = weatherData.windSpeed
        binding.pressure.text = weatherData.pressure
        binding.humidity.text = weatherData.humidity
        binding.visibilityText.text = "${weatherData.visibility / 1000} km"

        // Set weather icon
        setWeatherIcon(weatherData.weatherIcon)

        // Update UI visibility
        binding.progressBar.visibility = View.GONE
        binding.mainContainer.visibility = View.VISIBLE
        binding.swipeRefreshLayout.isRefreshing = false
    }

    private fun setWeatherIcon(iconCode: String) {
        val iconRes = when (iconCode) {
            "01d" -> R.drawable.ic_sunny
            "01n" -> R.drawable.ic_clear_night
            "02d", "03d", "04d" -> R.drawable.ic_cloudy_day
            "02n", "03n", "04n" -> R.drawable.ic_cloudy_night
            "09d", "09n", "10d", "10n" -> R.drawable.ic_rain
            "11d", "11n" -> R.drawable.ic_storm
            "13d", "13n" -> R.drawable.ic_snow
            "50d", "50n" -> R.drawable.ic_mist
            else -> R.drawable.ic_sunny
        }
        binding.weatherIcon.setImageResource(iconRes)
    }

    private fun generateActivityRecommendations(weatherData: WeatherData) {
        weatherActivities.clear()

        val temp = weatherData.temperature.replace("°C", "").toFloatOrNull() ?: 20f
        val conditionId = weatherData.conditionId
        val windSpeed = weatherData.windSpeed.replace(" m/s", "").toFloatOrNull() ?: 0f
        val visibility = weatherData.visibility

        // Running recommendation
        val runningScore = calculateRunningScore(temp, conditionId, windSpeed)
        weatherActivities.add(WeatherActivity(
            name = "Running",
            icon = R.drawable.ic_running,
            score = runningScore,
            recommendation = getRunningRecommendation(runningScore, temp, conditionId),
            isRecommended = runningScore >= 7
        ))

        // Cycling recommendation
        val cyclingScore = calculateCyclingScore(temp, conditionId, windSpeed, visibility)
        weatherActivities.add(WeatherActivity(
            name = "Cycling",
            icon = R.drawable.ic_cycling,
            score = cyclingScore,
            recommendation = getCyclingRecommendation(cyclingScore, windSpeed, conditionId),
            isRecommended = cyclingScore >= 6
        ))

        // Hiking recommendation
        val hikingScore = calculateHikingScore(temp, conditionId, visibility)
        weatherActivities.add(WeatherActivity(
            name = "Hiking",
            icon = R.drawable.ic_hiking,
            score = hikingScore,
            recommendation = getHikingRecommendation(hikingScore, conditionId, visibility),
            isRecommended = hikingScore >= 6
        ))

        // Outdoor Gym recommendation
        val gymScore = calculateOutdoorGymScore(temp, conditionId, windSpeed)
        weatherActivities.add(WeatherActivity(
            name = "Outdoor Gym",
            icon = R.drawable.ic_gym,
            score = gymScore,
            recommendation = getGymRecommendation(gymScore, temp, conditionId),
            isRecommended = gymScore >= 6
        ))

        // Swimming recommendation (if temperature is warm)
        if (temp >= 20) {
            val swimmingScore = calculateSwimmingScore(temp, conditionId, windSpeed)
            weatherActivities.add(WeatherActivity(
                name = "Swimming",
                icon = R.drawable.ic_swimming,
                score = swimmingScore,
                recommendation = getSwimmingRecommendation(swimmingScore, temp, conditionId),
                isRecommended = swimmingScore >= 7
            ))
        }

        binding.activitiesRecyclerView.adapter?.notifyDataSetChanged()
    }

    private fun calculateRunningScore(temp: Float, conditionId: Int, windSpeed: Float): Int {
        var score = 0

        // Temperature scoring (ideal: 15-20°C)
        when {
            temp in 15f..20f -> score += 3
            temp in 10f..14f -> score += 2
            temp in 21f..25f -> score += 2
            temp in 5f..9f -> score += 1
            temp in 26f..30f -> score += 1
            else -> score += 0
        }

        // Weather condition scoring
        when {
            conditionId in 800..800 -> score += 3 // Clear
            conditionId in 801..804 -> score += 2 // Clouds
            conditionId in 300..321 -> score += 1 // Drizzle
            conditionId in 500..531 -> score += 0 // Rain
            conditionId in 200..232 -> score += 0 // Thunderstorm
            conditionId in 600..622 -> score += 0 // Snow
            conditionId in 701..781 -> score += 0 // Atmosphere
        }

        // Wind scoring
        when {
            windSpeed < 5 -> score += 2
            windSpeed in 5f..10f -> score += 1
            else -> score += 0
        }

        return score.coerceIn(0, 10)
    }

    private fun calculateCyclingScore(temp: Float, conditionId: Int, windSpeed: Float, visibility: Int): Int {
        var score = 0

        // Temperature scoring (ideal: 15-25°C)
        when {
            temp in 15f..25f -> score += 3
            temp in 10f..14f -> score += 2
            temp in 26f..30f -> score += 2
            temp in 5f..9f -> score += 1
            else -> score += 0
        }

        // Weather condition scoring
        when {
            conditionId in 800..800 -> score += 3 // Clear
            conditionId in 801..804 -> score += 2 // Clouds
            conditionId in 300..321 -> score += 1 // Drizzle
            conditionId in 500..531 -> score += 0 // Rain
            else -> score += 0
        }

        // Wind scoring (cycling is more sensitive to wind)
        when {
            windSpeed < 3 -> score += 2
            windSpeed in 3f..7f -> score += 1
            else -> score += 0
        }

        // Visibility scoring
        if (visibility >= 5000) score += 2

        return score.coerceIn(0, 10)
    }

    private fun calculateHikingScore(temp: Float, conditionId: Int, visibility: Int): Int {
        var score = 0

        // Temperature scoring (ideal: 10-25°C)
        when {
            temp in 10f..25f -> score += 3
            temp in 5f..9f -> score += 2
            temp in 26f..30f -> score += 2
            else -> score += 0
        }

        // Weather condition scoring
        when {
            conditionId in 800..800 -> score += 3 // Clear
            conditionId in 801..804 -> score += 2 // Clouds
            conditionId in 300..321 -> score += 1 // Drizzle
            conditionId in 500..531 -> score += 0 // Rain
            else -> score += 0
        }

        // Visibility scoring (important for hiking)
        when {
            visibility >= 10000 -> score += 3
            visibility >= 5000 -> score += 2
            visibility >= 2000 -> score += 1
            else -> score += 0
        }

        return score.coerceIn(0, 10)
    }

    private fun calculateOutdoorGymScore(temp: Float, conditionId: Int, windSpeed: Float): Int {
        var score = 0

        // Temperature scoring (ideal: 15-25°C)
        when {
            temp in 15f..25f -> score += 3
            temp in 10f..14f -> score += 2
            temp in 26f..30f -> score += 2
            else -> score += 0
        }

        // Weather condition scoring
        when {
            conditionId in 800..800 -> score += 3 // Clear
            conditionId in 801..804 -> score += 2 // Clouds
            conditionId in 300..321 -> score += 1 // Drizzle
            else -> score += 0
        }

        // Wind scoring
        when {
            windSpeed < 5 -> score += 2
            windSpeed in 5f..10f -> score += 1
            else -> score += 0
        }

        return score.coerceIn(0, 10)
    }

    private fun calculateSwimmingScore(temp: Float, conditionId: Int, windSpeed: Float): Int {
        var score = 0

        // Temperature scoring (ideal: 25-30°C)
        when {
            temp >= 25f -> score += 3
            temp in 20f..24f -> score += 2
            else -> score += 0
        }

        // Weather condition scoring
        when {
            conditionId in 800..800 -> score += 3 // Clear
            conditionId in 801..804 -> score += 2 // Clouds
            else -> score += 0
        }

        // Wind scoring
        when {
            windSpeed < 5 -> score += 2
            windSpeed in 5f..10f -> score += 1
            else -> score += 0
        }

        return score.coerceIn(0, 10)
    }

    private fun getRunningRecommendation(score: Int, temp: Float, conditionId: Int): String {
        return when {
            score >= 8 -> "Perfect running conditions! Ideal temperature and clear skies."
            score >= 6 -> "Good for running. Dress appropriately for the temperature."
            score >= 4 -> "Fair conditions. Consider indoor alternatives."
            else -> "Not ideal for running. Poor weather conditions."
        }
    }

    private fun getCyclingRecommendation(score: Int, windSpeed: Float, conditionId: Int): String {
        return when {
            score >= 7 -> "Excellent cycling weather! Low wind and good visibility."
            score >= 5 -> "Good for cycling. Watch for occasional gusts."
            score >= 3 -> "Fair conditions. Consider wind resistance."
            else -> "Challenging conditions. High wind or poor visibility."
        }
    }

    private fun getHikingRecommendation(score: Int, conditionId: Int, visibility: Int): String {
        return when {
            score >= 7 -> "Perfect hiking weather! Great visibility and comfortable temperature."
            score >= 5 -> "Good for hiking. Trails should be enjoyable."
            score >= 3 -> "Fair conditions. Check trail conditions first."
            else -> "Poor hiking conditions. Limited visibility or unfavorable weather."
        }
    }

    private fun getGymRecommendation(score: Int, temp: Float, conditionId: Int): String {
        return when {
            score >= 7 -> "Ideal for outdoor workouts! Comfortable temperature."
            score >= 5 -> "Good conditions for outdoor exercise."
            score >= 3 -> "Fair for outdoor workouts. Dress in layers."
            else -> "Consider indoor gym. Weather not suitable for outdoor exercise."
        }
    }

    private fun getSwimmingRecommendation(score: Int, temp: Float, conditionId: Int): String {
        return when {
            score >= 7 -> "Perfect swimming weather! Warm and sunny."
            score >= 5 -> "Good for swimming. Water might be refreshing."
            else -> "Cool for swimming. Consider heated pool."
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PERMISSION_ID
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_ID) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_LONG).show()
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}