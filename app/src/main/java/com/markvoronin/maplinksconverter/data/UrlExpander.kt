package com.markvoronin.maplinksconverter.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.regex.Pattern

object UrlExpander {

    private val OG_URL_REGEX = Pattern.compile(
        """<meta\s+[^>]*property=["']og:url["']\s+content=["']([^"']+)["']""",
        Pattern.CASE_INSENSITIVE
    )

    private val CANONICAL_URL_REGEX = Pattern.compile(
        """<link\s+[^>]*rel=["']canonical["']\s+href=["']([^"']+)["']""",
        Pattern.CASE_INSENSITIVE
    )

    private val OG_TITLE_REGEX = Pattern.compile(
        """<meta\s+[^>]*property=["'](?:og:title|twitter:title)["']\s+content=["']([^"']+)["']""",
        Pattern.CASE_INSENSITIVE
    )

    private val TITLE_TAG_REGEX = Pattern.compile(
        """<title[^>]*>([^<]+)</title>""",
        Pattern.CASE_INSENSITIVE
    )

    private val HTML_COORDS_REGEX = Pattern.compile(
        """(?:geo\.position|icbm|center|ll|coordinate|latlng)["']?\s*(?:content|value)?=["']?(-?\d{1,3}\.\d+)\s*[,%2C;]\s*(-?\d{1,3}\.\d+)""",
        Pattern.CASE_INSENSITIVE
    )

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
                val urlObj = URL(currentUrl)
                val connection = (urlObj.openConnection() as HttpURLConnection)
                connection.instanceFollowRedirects = true
                connection.requestMethod = "GET"
                connection.connectTimeout = 2000
                connection.readTimeout = 2000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1")

                val responseCode = connection.responseCode
                val finalUrl = connection.url.toString()
                val location = connection.getHeaderField("Location")

                if (finalUrl != currentUrl && !isShortenedUrl(finalUrl)) {
                    connection.disconnect()
                    return@withContext finalUrl
                }

                if (!location.isNullOrBlank() && responseCode in 300..399) {
                    connection.disconnect()
                    currentUrl = if (location.startsWith("http", ignoreCase = true)) {
                        location
                    } else {
                        "https://$location"
                    }
                    redirectCount++
                    if (!isShortenedUrl(currentUrl)) {
                        return@withContext currentUrl
                    }
                    continue
                }

                if (responseCode == 200) {
                    try {
                        val stream = connection.inputStream
                        val reader = stream.bufferedReader()
                        val sb = StringBuilder()
                        var line: String? = reader.readLine()
                        var linesRead = 0
                        while (line != null && linesRead < 100) {
                            sb.append(line).append("\n")
                            if (line.contains("</head>", ignoreCase = true)) break
                            line = reader.readLine()
                            linesRead++
                        }
                        val html = sb.toString()
                        connection.disconnect()

                        val ogUrlMatcher = OG_URL_REGEX.matcher(html)
                        if (ogUrlMatcher.find()) {
                            val foundUrl = ogUrlMatcher.group(1)
                            if (!foundUrl.isNullOrBlank() && foundUrl != currentUrl) {
                                return@withContext foundUrl
                            }
                        }

                        val canonicalMatcher = CANONICAL_URL_REGEX.matcher(html)
                        if (canonicalMatcher.find()) {
                            val foundUrl = canonicalMatcher.group(1)
                            if (!foundUrl.isNullOrBlank() && foundUrl != currentUrl) {
                                return@withContext foundUrl
                            }
                        }

                        val titleMatcher = OG_TITLE_REGEX.matcher(html)
                        val title = if (titleMatcher.find()) {
                            titleMatcher.group(1)
                        } else {
                            val htmlTitleMatcher = TITLE_TAG_REGEX.matcher(html)
                            if (htmlTitleMatcher.find()) htmlTitleMatcher.group(1) else null
                        }

                        val coordsMatcher = HTML_COORDS_REGEX.matcher(html)
                        val coords = if (coordsMatcher.find()) "${coordsMatcher.group(1)},${coordsMatcher.group(2)}" else null
                        val cleanedTitle = cleanTitle(title)

                        if (!coords.isNullOrBlank() && !cleanedTitle.isNullOrBlank()) {
                            return@withContext "https://maps.apple.com/?q=${URLEncoder.encode(cleanedTitle, "UTF-8")}&ll=$coords"
                        } else if (!coords.isNullOrBlank()) {
                            return@withContext "https://maps.apple.com/?ll=$coords"
                        } else if (!cleanedTitle.isNullOrBlank()) {
                            return@withContext "https://maps.apple.com/?q=${URLEncoder.encode(cleanedTitle, "UTF-8")}"
                        }
                    } catch (e: Exception) {
                        connection.disconnect()
                    }
                } else {
                    connection.disconnect()
                }
                break
            }
            currentUrl
        } catch (e: Exception) {
            trimmed
        }
    }

    private fun cleanTitle(rawTitle: String?): String? {
        if (rawTitle.isNullOrBlank()) return null
        var t = rawTitle.trim()
        t = t.replace(Regex("(?i)\\s*[-|•]\\s*Apple\\s+Maps"), "")
        t = t.replace(Regex("(?i)Apple\\s+Maps\\s*[-|•]\\s*"), "")
        t = t.replace(Regex("(?i)\\s*[-|•]\\s*Waze"), "")
        t = t.replace(Regex("(?i)Waze\\s*[-|•]\\s*"), "")
        t = t.replace(Regex("(?i)\\s*[-|•]\\s*Google\\s+Maps"), "")
        t = t.replace(Regex("(?i)Google\\s+Maps\\s*[-|•]\\s*"), "")
        t = t.trim()
        return if (t.isNotBlank() && !t.equals("Apple Maps", ignoreCase = true) && !t.equals("Waze", ignoreCase = true) && !t.equals("Google Maps", ignoreCase = true)) t else null
    }

    fun isShortenedUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("maps.app.goo.gl") ||
                lower.contains("goo.gl/maps") ||
                lower.contains("goo.gl") ||
                lower.contains("bit.ly") ||
                lower.contains("tinyurl.com") ||
                lower.contains("apple.co") ||
                lower.contains("maps.apple.com/p/") ||
                lower.contains("maps.apple/p/") ||
                lower.contains("waze.com/ul/") ||
                lower.contains("ul.waze.com") ||
                lower.contains("waze.com/ul?h=") ||
                (lower.contains("maps.apple") && !hasLocationalParams(lower)) ||
                (lower.contains("waze") && !hasLocationalParams(lower))
    }

    private fun hasLocationalParams(lowerUrl: String): Boolean {
        return lowerUrl.contains("ll=") ||
                lowerUrl.contains("q=") ||
                lowerUrl.contains("address=") ||
                lowerUrl.contains("saddr=") ||
                lowerUrl.contains("daddr=") ||
                lowerUrl.contains("latlng=") ||
                lowerUrl.contains("coordinate=") ||
                lowerUrl.contains("center=") ||
                lowerUrl.contains("to=") ||
                lowerUrl.contains("destination=") ||
                lowerUrl.contains("/place/")
    }
}
