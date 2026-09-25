package com.markvoronin.maplinksconverter.data

import java.net.URLDecoder
import java.net.URLEncoder
import java.util.regex.Pattern

object AppleMapsConverter {

    private val MAP_LINK_REGEX = Pattern.compile(
        "https?://(?:[a-zA-Z0-9-]+\\.)*(?:maps\\.apple\\.com|maps\\.apple|apple\\.co|waze\\.com|google\\.com|goo\\.gl)[^\\s<>\"]*|waze://[^\\s<>\"]*",
        Pattern.CASE_INSENSITIVE,
    )

    private val LAT_LNG_REGEX = Pattern.compile(
        "^(-?\\d+(?:\\.\\d+)?)\\s*[,%2C]\\s*(-?\\d+(?:\\.\\d+)?)$",
        Pattern.CASE_INSENSITIVE,
    )

    private val PATH_COORDS_REGEX = Pattern.compile(
        """(?:[/@=]|ll\.|lat=|=|to=)(-?\d{1,3}\.\d+)\s*[,%2C\s&]+(?:lon=|lng=)?(-?\d{1,3}\.\d+)""",
        Pattern.CASE_INSENSITIVE,
    )

    private val APPLE_PLACE_NAME_REGEX = Pattern.compile(
        "/place/([^/@?#]+)",
        Pattern.CASE_INSENSITIVE
    )

    private val GOOGLE_PLACE_COORDS_REGEX = Pattern.compile(
        "/@(-?\\d+(?:\\.\\d+)?),\\s*(-?\\d+(?:\\.\\d+)?)"
    )

    private val GOOGLE_PLACE_PATH_COORDS_REGEX = Pattern.compile(
        "/place/(-?\\d+(?:\\.\\d+)?),\\s*(-?\\d+(?:\\.\\d+)?)"
    )

    private val GOOGLE_PLACE_NAME_REGEX = Pattern.compile(
        "/place/([^/@]+)"
    )

    private val GOOGLE_DIR_PATH_REGEX = Pattern.compile(
        "/dir/([^/]+)/([^/]+)"
    )

    /**
     * Extracts an Apple Maps, Google Maps, or Waze URL from any given text.
     */
    fun extractAppleMapsUrl(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        val matcher = MAP_LINK_REGEX.matcher(trimmed)
        if (matcher.find()) {
            return matcher.group(0)
        }

        if (trimmed.startsWith("maps.apple.com", ignoreCase = true) ||
            trimmed.startsWith("maps.apple", ignoreCase = true) ||
            trimmed.startsWith("apple.co", ignoreCase = true) ||
            trimmed.startsWith("waze.com", ignoreCase = true) ||
            trimmed.startsWith("www.waze.com", ignoreCase = true) ||
            trimmed.startsWith("maps.google.com", ignoreCase = true) ||
            trimmed.startsWith("www.google.com/maps", ignoreCase = true)) {
            return "https://$trimmed"
        }

        if (trimmed.startsWith("waze://", ignoreCase = true)) {
            return trimmed
        }

        return null
    }

    /**
     * Converts a given input (Apple Maps, Google Maps, or Waze URL/text) into Google Maps and Waze format.
     */
    fun convert(input: String, targetApp: MapTargetApp = MapTargetApp.GOOGLE_MAPS): ConversionResult {
        val trimmedInput = input.trim()
        if (trimmedInput.isEmpty()) {
            return ConversionResult(
                originalInput = input,
                isSuccess = false,
                errorMessage = "Input is empty"
            )
        }

        val extractedUrl = extractAppleMapsUrl(trimmedInput)
            ?: return ConversionResult(
                originalInput = input,
                isSuccess = false,
                errorMessage = "No valid map link (Apple Maps, Google Maps, or Waze) found in input"
            )

        val linkSource = when {
            extractedUrl.contains("apple", ignoreCase = true) -> MapLinkSource.APPLE_MAPS
            extractedUrl.contains("google", ignoreCase = true) || extractedUrl.contains("goo.gl", ignoreCase = true) -> MapLinkSource.GOOGLE_MAPS
            extractedUrl.contains("waze", ignoreCase = true) -> MapLinkSource.WAZE
            else -> MapLinkSource.UNKNOWN
        }

        val queryParams = parseQueryParams(extractedUrl)

        var qParam = queryParams["q"]
            ?: queryParams["query"]
            ?: queryParams["name"]
            ?: queryParams["title"]
            ?: queryParams["place"]
        var llParam = queryParams["ll"]
            ?: queryParams["latlng"]
            ?: queryParams["sll"]
            ?: queryParams["center"]
            ?: queryParams["point"]
            ?: queryParams["coordinate"]
            ?: queryParams["near"]
        val addressParam = queryParams["address"] ?: queryParams["addr"]
        var toParam = queryParams["to"] ?: queryParams["destination"] ?: queryParams["daddr"]
        var fromParam = queryParams["from"] ?: queryParams["from_ll"] ?: queryParams["origin"] ?: queryParams["saddr"]
        val dirflgParam = queryParams["dirflg"] ?: queryParams["travelmode"]

        if (llParam.isNullOrBlank()) {
            val lat = queryParams["lat"]
            val lon = queryParams["lon"] ?: queryParams["lng"]
            if (!lat.isNullOrBlank() && !lon.isNullOrBlank()) {
                llParam = "$lat,$lon"
            }
        }

        if (llParam.isNullOrBlank()) {
            val pathCoordsMatcher = PATH_COORDS_REGEX.matcher(extractedUrl)
            if (pathCoordsMatcher.find()) {
                llParam = "${pathCoordsMatcher.group(1)},${pathCoordsMatcher.group(2)}"
            }
        }

        if (qParam.isNullOrBlank() && (linkSource == MapLinkSource.APPLE_MAPS)) {
            val applePlaceMatcher = APPLE_PLACE_NAME_REGEX.matcher(extractedUrl)
            if (applePlaceMatcher.find()) {
                val rawPlace = applePlaceMatcher.group(1)
                if (!rawPlace.isNullOrBlank() && !rawPlace.equals("p", ignoreCase = true) && !isCoordinatesFormat(rawPlace) && !isRawUrl(rawPlace)) {
                    qParam = decode(rawPlace.replace("-", " ").replace("+", " "))
                }
            }
        }

        // Extract parameters embedded in Google Maps path if missing in query parameters
        if (linkSource == MapLinkSource.GOOGLE_MAPS) {
            if (llParam.isNullOrBlank()) {
                val coordsMatcher = GOOGLE_PLACE_COORDS_REGEX.matcher(extractedUrl)
                if (coordsMatcher.find()) {
                    llParam = "${coordsMatcher.group(1)},${coordsMatcher.group(2)}"
                } else {
                    val pathCoordsMatcher = GOOGLE_PLACE_PATH_COORDS_REGEX.matcher(extractedUrl)
                    if (pathCoordsMatcher.find()) {
                        llParam = "${pathCoordsMatcher.group(1)},${pathCoordsMatcher.group(2)}"
                    }
                }
            }

            if (qParam.isNullOrBlank()) {
                val placeMatcher = GOOGLE_PLACE_NAME_REGEX.matcher(extractedUrl)
                if (placeMatcher.find()) {
                    val rawPlaceName = placeMatcher.group(1)
                    if ((rawPlaceName != null) && !isCoordinatesFormat(rawPlaceName)) {
                        qParam = decode(rawPlaceName.replace("+", " "))
                    }
                }
            }

            if (toParam.isNullOrBlank() && fromParam.isNullOrBlank()) {
                val dirMatcher = GOOGLE_DIR_PATH_REGEX.matcher(extractedUrl)
                if (dirMatcher.find()) {
                    val rawOrigin = dirMatcher.group(1)
                    val rawDest = dirMatcher.group(2)
                    if ((rawOrigin != null) && (rawDest != null)) {
                        fromParam = decode(rawOrigin.replace("+", " "))
                        toParam = decode(rawDest.replace("+", " "))
                    }
                }
            }
        }

        val coordinates = cleanCoordinates(llParam) ?: extractCoordinatesFromQuery(qParam) ?: cleanCoordinates(toParam) ?: cleanCoordinates(addressParam)
        val query = cleanQuery(qParam)
        val address = addressParam?.trim()

        // 1. Directions mode
        if (!toParam.isNullOrBlank() || !fromParam.isNullOrBlank()) {
            val rawDestination = toParam ?: query ?: address ?: coordinates ?: ""
            val destination = cleanCoordinates(rawDestination) ?: cleanQuery(rawDestination) ?: rawDestination
            val rawOrigin = fromParam ?: ""
            val origin = cleanCoordinates(rawOrigin) ?: cleanQuery(rawOrigin) ?: rawOrigin
            val travelMode = parseTravelMode(dirflgParam)

            // Google Maps URL
            val gMapsUrlBuilder = StringBuilder("https://www.google.com/maps/dir/?api=1")
            if (origin.isNotBlank()) {
                gMapsUrlBuilder.append("&origin=").append(encode(origin))
            }
            if (destination.isNotBlank()) {
                gMapsUrlBuilder.append("&destination=").append(encode(destination))
            }
            if (!travelMode.isNullOrBlank()) {
                gMapsUrlBuilder.append("&travelmode=").append(travelMode)
            }
            val gMapsUrl = gMapsUrlBuilder.toString()

            // Waze URL
            val wazeUrl = buildWazeUrl(
                query = null,
                coordinates = if (isCoordinatesFormat(destination)) destination else null,
                address = if (!isCoordinatesFormat(destination)) destination else null,
                destination = destination
            )

            // Apple Maps URL
            val appleMapsUrlBuilder = StringBuilder("https://maps.apple.com/?daddr=")
            if (destination.isNotBlank()) {
                val formattedDest = if (isCoordinatesFormat(destination)) destination else encode(destination)
                appleMapsUrlBuilder.append(formattedDest)
            }
            if (origin.isNotBlank()) {
                val formattedOrigin = if (isCoordinatesFormat(origin)) origin else encode(origin)
                appleMapsUrlBuilder.append("&saddr=").append(formattedOrigin)
            }
            if (!travelMode.isNullOrBlank()) {
                val dirFlg = when (travelMode.lowercase()) {
                    "driving" -> "d"
                    "walking" -> "w"
                    "transit" -> "r"
                    "bicycling" -> "r"
                    else -> null
                }
                dirFlg?.let {
                    appleMapsUrlBuilder.append("&dirflg=").append(it)
                }
            }
            val appleMapsUrl = appleMapsUrlBuilder.toString()

            val geoUri = if (destination.isNotBlank()) "geo:0,0?q=${encode(destination)}" else "geo:0,0"

            val convertedUrl = when (targetApp) {
                MapTargetApp.WAZE -> wazeUrl
                MapTargetApp.APPLE_MAPS -> appleMapsUrl
                MapTargetApp.GOOGLE_MAPS -> gMapsUrl
            }

            return ConversionResult(
                originalInput = input,
                extractedLinkUrl = extractedUrl,
                targetApp = targetApp,
                convertedUrl = convertedUrl,
                googleMapsUrl = gMapsUrl,
                wazeUrl = wazeUrl,
                appleMapsUrl = appleMapsUrl,
                geoUri = geoUri,
                linkSource = linkSource,
                linkType = MapLinkType.DIRECTIONS,
                query = query,
                coordinates = coordinates,
                address = address,
                origin = origin.ifBlank { null },
                destination = destination.ifBlank { null },
                travelMode = travelMode,
                isSuccess = true
            )
        }

        // 2. Search / Location mode
        val linkType: MapLinkType
        val gMapsUrl: String

        when {
            !query.isNullOrBlank() -> {
                linkType = MapLinkType.SEARCH
                val builder = StringBuilder("https://www.google.com/maps/search/?api=1")
                builder.append("&query=").append(encode(query))
                if (!coordinates.isNullOrBlank() && !isCoordinatesFormat(query)) {
                    builder.append("&center=").append(encode(coordinates))
                }
                gMapsUrl = builder.toString()
            }
            !address.isNullOrBlank() -> {
                linkType = MapLinkType.SEARCH
                val builder = StringBuilder("https://www.google.com/maps/search/?api=1")
                builder.append("&query=").append(encode(address))
                if (!coordinates.isNullOrBlank()) {
                    builder.append("&center=").append(encode(coordinates))
                }
                gMapsUrl = builder.toString()
            }
            !coordinates.isNullOrBlank() -> {
                linkType = MapLinkType.LOCATION
                gMapsUrl = "https://www.google.com/maps/search/?api=1&query=${encode(coordinates)}"
            }
            else -> {
                // If query, address, and coordinates are null but we have an Apple/Google/Waze link (e.g. short place or route ID link), use the extracted URL as search query
                val isHomepage = extractedUrl.equals("https://maps.apple.com", ignoreCase = true) ||
                        extractedUrl.equals("https://maps.apple.com/", ignoreCase = true) ||
                        extractedUrl.equals("https://www.google.com/maps", ignoreCase = true) ||
                        extractedUrl.equals("https://www.google.com/maps/", ignoreCase = true) ||
                        extractedUrl.equals("https://waze.com/ul", ignoreCase = true)

                if (isHomepage) {
                    return ConversionResult(
                        originalInput = input,
                        extractedLinkUrl = extractedUrl,
                        targetApp = targetApp,
                        linkSource = linkSource,
                        linkType = MapLinkType.UNKNOWN,
                        isSuccess = false,
                        errorMessage = "Link '$extractedUrl' is a general map homepage link and does not contain a specific place or location to convert."
                    )
                }

                linkType = MapLinkType.SEARCH
                gMapsUrl = "https://www.google.com/maps/search/?api=1&query=${encode(extractedUrl)}"
            }
        }

        val wazeUrl = buildWazeUrl(
            query = query ?: extractedUrl,
            coordinates = coordinates,
            address = address,
            destination = null
        )

        val appleMapsUrl = when {
            !query.isNullOrBlank() -> {
                if (!coordinates.isNullOrBlank() && !isCoordinatesFormat(query)) {
                    "https://maps.apple.com/?q=${encode(query)}&ll=$coordinates"
                } else {
                    "https://maps.apple.com/?q=${encode(query)}"
                }
            }
            !address.isNullOrBlank() -> {
                if (!coordinates.isNullOrBlank()) {
                    "https://maps.apple.com/?q=${encode(address)}&ll=$coordinates"
                } else {
                    "https://maps.apple.com/?q=${encode(address)}"
                }
            }
            !coordinates.isNullOrBlank() -> {
                "https://maps.apple.com/?q=$coordinates&ll=$coordinates"
            }
            else -> {
                val isHomepage = extractedUrl.equals("https://maps.apple.com", ignoreCase = true) ||
                        extractedUrl.equals("https://maps.apple.com/", ignoreCase = true) ||
                        extractedUrl.equals("https://www.google.com/maps", ignoreCase = true) ||
                        extractedUrl.equals("https://www.google.com/maps/", ignoreCase = true) ||
                        extractedUrl.equals("https://waze.com/ul", ignoreCase = true)

                if (isHomepage) {
                    "https://maps.apple.com"
                } else {
                    "https://maps.apple.com/?q=${encode(extractedUrl)}"
                }
            }
        }

        val geoUri = when {
            !coordinates.isNullOrBlank() && !query.isNullOrBlank() -> "geo:$coordinates?q=${encode(query)}"
            !coordinates.isNullOrBlank() -> "geo:$coordinates?q=$coordinates"
            !query.isNullOrBlank() -> "geo:0,0?q=${encode(query)}"
            !address.isNullOrBlank() -> "geo:0,0?q=${encode(address)}"
            else -> "geo:0,0?q=${encode(extractedUrl)}"
        }

        val convertedUrl = when (targetApp) {
            MapTargetApp.WAZE -> wazeUrl
            MapTargetApp.APPLE_MAPS -> appleMapsUrl
            MapTargetApp.GOOGLE_MAPS -> gMapsUrl
        }

        return ConversionResult(
            originalInput = input,
            extractedLinkUrl = extractedUrl,
            targetApp = targetApp,
            convertedUrl = convertedUrl,
            googleMapsUrl = gMapsUrl,
            wazeUrl = wazeUrl,
            appleMapsUrl = appleMapsUrl,
            geoUri = geoUri,
            linkSource = linkSource,
            linkType = linkType,
            query = query,
            coordinates = coordinates,
            address = address,
            isSuccess = true
        )
    }

    private fun buildWazeUrl(
        query: String?,
        coordinates: String?,
        address: String?,
        destination: String?
    ): String {
        val dest = destination?.trim()
        if (!dest.isNullOrBlank() && !isRawUrl(dest)) {
            return if (isCoordinatesFormat(dest)) {
                "https://waze.com/ul?ll=${encode(dest)}&navigate=yes"
            } else {
                "https://waze.com/ul?q=${encode(dest)}&navigate=yes"
            }
        }

        val coords = cleanCoordinates(coordinates)
        val q = cleanQuery(query)
        val addr = address?.trim()

        return when {
            !coords.isNullOrBlank() && !q.isNullOrBlank() && !isCoordinatesFormat(q) && !isRawUrl(q) -> {
                "https://waze.com/ul?ll=${encode(coords)}&q=${encode(q)}&navigate=yes"
            }
            !coords.isNullOrBlank() -> {
                "https://waze.com/ul?ll=${encode(coords)}&navigate=yes"
            }
            !q.isNullOrBlank() && !isRawUrl(q) -> {
                "https://waze.com/ul?q=${encode(q)}&navigate=yes"
            }
            !addr.isNullOrBlank() && !isRawUrl(addr) -> {
                "https://waze.com/ul?q=${encode(addr)}&navigate=yes"
            }
            else -> "https://waze.com/ul?navigate=yes"
        }
    }

    private fun isRawUrl(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.startsWith("http://", ignoreCase = true) ||
               trimmed.startsWith("https://", ignoreCase = true) ||
               trimmed.startsWith("maps.", ignoreCase = true) ||
               trimmed.startsWith("www.", ignoreCase = true)
    }

    private fun parseQueryParams(url: String): Map<String, String> {
        val params = mutableMapOf<String, String>()

        var queryString = ""
        val questionMarkIndex = url.indexOf('?')
        if ((questionMarkIndex != -1) && (questionMarkIndex < url.length - 1)) {
            queryString = url.substring(questionMarkIndex + 1)
        }

        val hashIndex = queryString.indexOf('#')
        if (hashIndex != -1) {
            queryString = queryString.substring(0, hashIndex)
        }

        if (queryString.isNotEmpty()) {
            val pairs = queryString.split("&")
            for (pair in pairs) {
                val idx = pair.indexOf('=')
                if (idx > 0) {
                    var rawKey = decode(pair.substring(0, idx)).lowercase().trim()
                    if (rawKey.contains('?')) {
                        rawKey = rawKey.substringAfterLast('?')
                    }
                    val value = decode(pair.substring(idx + 1)).trim()
                    if (rawKey.isNotEmpty()) {
                        params[rawKey] = value
                    }
                } else if (pair.isNotEmpty()) {
                    var rawKey = decode(pair).lowercase().trim()
                    if (rawKey.contains('?')) {
                        rawKey = rawKey.substringAfterLast('?')
                    }
                    if (rawKey.isNotEmpty()) {
                        params[rawKey] = ""
                    }
                }
            }
        }

        return params
    }

    private fun parseTravelMode(dirflg: String?): String? {
        if (dirflg.isNullOrBlank()) return null
        return when (dirflg.lowercase().trim()) {
            "d" -> "driving"
            "w" -> "walking"
            "r" -> "transit"
            "b" -> "bicycling"
            else -> dirflg.lowercase().trim()
        }
    }

    private fun cleanCoordinates(input: String?): String? {
        if (input.isNullOrBlank()) return null
        var decoded = decode(input.trim())
        decoded = decoded.replace(Regex("(?i)^(?:ll[.=:]|loc:|geo:|point:|latlng[=:]|to[.=:]|lat[=:]|@)"), "").trim()
        val matcher = LAT_LNG_REGEX.matcher(decoded)
        return if (matcher.matches()) {
            "${matcher.group(1)},${matcher.group(2)}"
        } else {
            null
        }
    }

    private fun extractCoordinatesFromQuery(query: String?): String? {
        if (query.isNullOrBlank()) return null
        return cleanCoordinates(query)
    }

    private fun isCoordinatesFormat(text: String): Boolean {
        return cleanCoordinates(text) != null
    }

    private fun cleanQuery(q: String?): String? {
        if (q.isNullOrBlank()) return null
        val trimmed = q.trim()
        if (isRawUrl(trimmed)) return null
        val coords = cleanCoordinates(trimmed)
        if (coords != null) return coords
        val cleaned = trimmed.replace(Regex("(?i)^(?:ll[.=:]|loc:|geo:|point:|latlng[=:]|@)"), "").trim()
        return cleaned.ifBlank { null }
    }

    private fun decode(s: String): String {
        return try {
            URLDecoder.decode(s, "UTF-8")
        } catch (_: Exception) {
            s
        }
    }

    private fun encode(s: String): String {
        return try {
            URLEncoder.encode(s, "UTF-8")
        } catch (_: Exception) {
            s
        }
    }
}
