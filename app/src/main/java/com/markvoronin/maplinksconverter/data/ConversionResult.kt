package com.markvoronin.maplinksconverter.data

enum class MapLinkType {
    SEARCH,
    LOCATION,
    DIRECTIONS,
    UNKNOWN
}

enum class MapLinkSource {
    APPLE_MAPS,
    GOOGLE_MAPS,
    WAZE,
    UNKNOWN
}

enum class MapTargetApp {
    GOOGLE_MAPS,
    WAZE
}

data class ConversionResult(
    val originalInput: String,
    val extractedLinkUrl: String? = null,
    val targetApp: MapTargetApp = MapTargetApp.GOOGLE_MAPS,
    val convertedUrl: String? = null,
    val googleMapsUrl: String? = null,
    val wazeUrl: String? = null,
    val geoUri: String? = null,
    val linkSource: MapLinkSource = MapLinkSource.UNKNOWN,
    val linkType: MapLinkType = MapLinkType.UNKNOWN,
    val query: String? = null,
    val coordinates: String? = null,
    val address: String? = null,
    val origin: String? = null,
    val destination: String? = null,
    val travelMode: String? = null,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
) {
    fun hasLocationData(): Boolean {
        return !query.isNullOrBlank() ||
               !coordinates.isNullOrBlank() ||
               !address.isNullOrBlank() ||
               !destination.isNullOrBlank()
    }
}
