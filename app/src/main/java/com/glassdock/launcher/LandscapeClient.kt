package com.glassdock.launcher

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class LandscapePhoto(
    val url: String,
    val title: String
)

object LandscapeClient {

    private const val FEED_URL =
        "https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=8&mkt=en-US"

    private val mainHandler = Handler(Looper.getMainLooper())

    fun fetchPlaylistAsync(onResult: (List<LandscapePhoto>) -> Unit) {
        Thread {
            val photos = runCatching { queryFeed() }.getOrDefault(emptyList())
            mainHandler.post { onResult(photos) }
        }.apply { isDaemon = true }.start()
    }

    fun loadBitmapAsync(photo: LandscapePhoto, onResult: (Bitmap?) -> Unit) {
        Thread {
            val bitmap = runCatching { downloadBitmap(photo.url) }.getOrNull()
            mainHandler.post { onResult(bitmap) }
        }.apply { isDaemon = true }.start()
    }

    private fun queryFeed(): List<LandscapePhoto> {
        val conn = (URL(FEED_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
        }
        return try {
            val body = conn.inputStream.bufferedReader().readText()
            val images = JSONObject(body).getJSONArray("images")
            List(images.length()) { i ->
                val item = images.getJSONObject(i)
                val urlBase = item.getString("urlbase")
                val rawCopyright = item.optString("copyright", "")
                val cleanTitle = rawCopyright.substringBefore(" (").trim()
                    .ifEmpty { item.optString("title", "") }
                LandscapePhoto("https://www.bing.com${urlBase}_1920x1080.jpg", cleanTitle)
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun downloadBitmap(urlString: String): Bitmap? {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = 7000
            readTimeout = 7000
        }
        return try {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            conn.inputStream.use { BitmapFactory.decodeStream(it, null, options) }
        } finally {
            conn.disconnect()
        }
    }
}
