package com.openwear.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "OpenWear:Weather"
private const val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 minutes

data class WeatherData(
    val temperatureCelsius: Float,
    val weatherCode: Int,          // WMO weather code
    val windSpeedKmh: Float,
    val condition: String,
    val conditionEmoji: String,
    val isDay: Boolean,
    val feelsLike: Float           // derived from wind chill
)

@Singleton
class WeatherRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("openwear_weather", Context.MODE_PRIVATE)

    fun getLastKnownLocation(): Pair<Double, Double>? {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
            val providers = locationManager?.getProviders(true) ?: emptyList()
            var bestLocation: android.location.Location? = null
            for (provider in providers) {
                val l = locationManager?.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                    bestLocation = l
                }
            }
            bestLocation?.let { Pair(it.latitude, it.longitude) }
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permissions not granted for weather location lookup", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location", e)
            null
        }
    }

    /** Fetch weather for the given coordinates. Returns null on failure. */
    suspend fun fetchWeather(lat: Double, lon: Double): WeatherData? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cachedTime = prefs.getLong("weather_cached_at", 0L)
        val cachedLat = prefs.getFloat("weather_lat", 0f)
        val cachedLon = prefs.getFloat("weather_lon", 0f)

        // Return cached data if still fresh and location hasn't changed much
        if (now - cachedTime < CACHE_DURATION_MS &&
            Math.abs(cachedLat - lat.toFloat()) < 0.01f &&
            Math.abs(cachedLon - lon.toFloat()) < 0.01f
        ) {
            return@withContext getCachedWeather()
        }

        return@withContext try {
            val url = "https://api.open-meteo.com/v1/forecast?" +
                    "latitude=$lat&longitude=$lon" +
                    "&current_weather=true" +
                    "&hourly=apparent_temperature" +
                    "&forecast_days=1"
            val json = URL(url).readText()
            val root = JSONObject(json)
            val current = root.getJSONObject("current_weather")

            val tempC = current.getDouble("temperature").toFloat()
            val windSpeed = current.getDouble("windspeed").toFloat()
            val code = current.getInt("weathercode")
            val isDay = current.getInt("is_day") == 1

            // Get apparent temperature (feels like) from first hourly entry
            val feelsLike = try {
                root.getJSONObject("hourly")
                    .getJSONArray("apparent_temperature")
                    .getDouble(0).toFloat()
            } catch (e: Exception) { tempC }

            val (condition, emoji) = weatherCodeToCondition(code, isDay)

            val data = WeatherData(
                temperatureCelsius = tempC,
                weatherCode = code,
                windSpeedKmh = windSpeed,
                condition = condition,
                conditionEmoji = emoji,
                isDay = isDay,
                feelsLike = feelsLike
            )

            // Cache result
            prefs.edit()
                .putFloat("weather_temp", tempC)
                .putFloat("weather_wind", windSpeed)
                .putInt("weather_code", code)
                .putBoolean("weather_is_day", isDay)
                .putFloat("weather_feels_like", feelsLike)
                .putLong("weather_cached_at", now)
                .putFloat("weather_lat", lat.toFloat())
                .putFloat("weather_lon", lon.toFloat())
                .apply()

            data
        } catch (e: Exception) {
            Log.e(TAG, "Weather fetch failed: ${e.message}")
            getCachedWeather() // Fall back to cache even if expired
        }
    }

    private fun getCachedWeather(): WeatherData? {
        val cached = prefs.getLong("weather_cached_at", 0L)
        if (cached == 0L) return null
        val code = prefs.getInt("weather_code", 0)
        val isDay = prefs.getBoolean("weather_is_day", true)
        val (condition, emoji) = weatherCodeToCondition(code, isDay)
        return WeatherData(
            temperatureCelsius = prefs.getFloat("weather_temp", 0f),
            weatherCode = code,
            windSpeedKmh = prefs.getFloat("weather_wind", 0f),
            condition = condition,
            conditionEmoji = emoji,
            isDay = isDay,
            feelsLike = prefs.getFloat("weather_feels_like", 0f)
        )
    }

    private fun weatherCodeToCondition(code: Int, isDay: Boolean): Pair<String, String> {
        return when (code) {
            0 -> if (isDay) Pair("Clear Sky", "☀️") else Pair("Clear Night", "🌙")
            1, 2 -> Pair("Partly Cloudy", "⛅")
            3 -> Pair("Overcast", "☁️")
            45, 48 -> Pair("Foggy", "🌫️")
            51, 53, 55 -> Pair("Drizzle", "🌦️")
            61, 63, 65 -> Pair("Rainy", "🌧️")
            71, 73, 75 -> Pair("Snowy", "❄️")
            80, 81, 82 -> Pair("Showers", "🌦️")
            95 -> Pair("Thunderstorm", "⛈️")
            else -> Pair("Cloudy", "☁️")
        }
    }
}
