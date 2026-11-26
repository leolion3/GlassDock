package com.glassdock.launcher

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object WeatherClient {

    private const val ENDPOINT =
        "https://api.open-meteo.com/v1/forecast?latitude=53.0793&longitude=8.8017&current=temperature_2m,weather_code"

    fun fetchAsync(onResult: (String) -> Unit) {
        Thread {
            val formatted = runCatching { queryEndpoint() }.getOrNull() ?: return@Thread
            Handler(Looper.getMainLooper()).post { onResult(formatted) }
        }.apply { isDaemon = true }.start()
    }

    private fun queryEndpoint(): String {
        val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            connectTimeout = 4000
            readTimeout = 4000
        }
        return try {
            val body = conn.inputStream.bufferedReader().readText()
            val current = JSONObject(body).getJSONObject("current")
            val temp = Math.round(current.getDouble("temperature_2m"))
            val cond = mapWeatherCode(current.getInt("weather_code"))
            if (cond.isNotEmpty()) "  ·  ${temp}°C $cond" else "  ·  ${temp}°C"
        } finally {
            conn.disconnect()
        }
    }

    private fun mapWeatherCode(code: Int): String = when (code) {
        0 -> "Clear"
        1, 2, 3 -> "Cloudy"
        45, 48 -> "Fog"
        51, 53, 55, 61, 63, 65, 80, 81, 82 -> "Rain"
        71, 73, 75, 77 -> "Snow"
        95, 96, 99 -> "Storm"
        else -> ""
    }
}
