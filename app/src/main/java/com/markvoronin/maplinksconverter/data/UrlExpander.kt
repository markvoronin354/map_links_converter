package com.markvoronin.maplinksconverter.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object UrlExpander {

    /**
     * Checks if a URL is a shortened or redirecting map URL and expands it asynchronously.
     */
    suspend fun expandUrlIfNeeded(url: String): String = withContext(Dispatchers.IO) {
        val trimmed = url.trim()
        if (trimmed.isBlank() || !isShortenedUrl(trimmed)) {
            return@withContext trimmed
        }

        try {
            var currentUrl = trimmed
            var redirectCount = 0
            val maxRedirects = 5

            while (redirectCount < maxRedirects) {
                var connection = (URL(currentUrl).openConnection() as HttpURLConnection)
                connection.instanceFollowRedirects = false
                connection.requestMethod = "HEAD"
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1")

                val responseCode = connection.responseCode
                var location = connection.getHeaderField("Location")

                // If HEAD request did not return a location redirect, attempt a GET request
                if (location.isNullOrBlank() || responseCode in 400..499) {
                    connection.disconnect()
                    connection = (URL(currentUrl).openConnection() as HttpURLConnection)
                    connection.instanceFollowRedirects = false
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1")
                    location = connection.getHeaderField("Location")
                }

                if (!location.isNullOrBlank()) {
                    connection.disconnect()
                    currentUrl = if (location.startsWith("http", ignoreCase = true)) {
                        location
                    } else {
                        "https://$location"
                    }
                    redirectCount++
                    if (!isShortenedUrl(currentUrl)) {
                        break
                    }
                } else {
                    connection.disconnect()
                    break
                }
            }
            currentUrl
        } catch (e: Exception) {
            trimmed
        }
    }

    fun isShortenedUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("maps.app.goo.gl") ||
                lower.contains("goo.gl/maps") ||
                lower.contains("goo.gl") ||
                lower.contains("bit.ly") ||
                lower.contains("tinyurl.com") ||
                lower.contains("apple.co") ||
                lower.contains("maps.apple/") ||
                lower.contains("maps.apple.com/p/") ||
                lower.contains("maps.apple.com/place/") ||
                (lower.contains("maps.apple") && !lower.contains("ll="))
    }
}
