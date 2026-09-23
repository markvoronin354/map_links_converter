package com.markvoronin.maplinksconverter.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.markvoronin.maplinksconverter.data.ConversionResult
import com.markvoronin.maplinksconverter.data.MapLinkSource
import com.markvoronin.maplinksconverter.data.MapLinkType
import com.markvoronin.maplinksconverter.data.MapTargetApp
import com.markvoronin.maplinksconverter.ui.theme.MapLinksConverterTheme

val sampleUiStateWithResult = MainUiState(
    inputUrl = "https://maps.apple.com/?q=Golden+Gate+Bridge&ll=37.8199,-122.4783",
    conversionResult = ConversionResult(
        originalInput = "https://maps.apple.com/?q=Golden+Gate+Bridge&ll=37.8199,-122.4783",
        extractedLinkUrl = "https://maps.apple.com/?q=Golden+Gate+Bridge&ll=37.8199,-122.4783",
        isSuccess = true,
        linkSource = MapLinkSource.APPLE_MAPS,
        targetApp = MapTargetApp.GOOGLE_MAPS,
        linkType = MapLinkType.LOCATION,
        query = "Golden Gate Bridge",
        coordinates = "37.8199, -122.4783",
        address = "Golden Gate Bridge, San Francisco, CA",
        convertedUrl = "https://www.google.com/maps/search/?api=1&query=37.8199,-122.4783",
        googleMapsUrl = "https://www.google.com/maps/search/?api=1&query=37.8199,-122.4783",
        wazeUrl = "https://waze.com/ul?ll=37.8199,-122.4783&navigate=yes",
        appleMapsUrl = "https://maps.apple.com/?q=Golden+Gate+Bridge&ll=37.8199,-122.4783"
    ),
    autoRedirectEnabled = true,
    appleMapsTarget = MapTargetApp.GOOGLE_MAPS,
    googleMapsTarget = MapTargetApp.WAZE,
    wazeTarget = MapTargetApp.GOOGLE_MAPS
)

@Preview(showBackground = true, name = "Current Design Preview")
@Composable
fun CurrentHomeScreenPreview() {
    MapLinksConverterTheme {
        HomeScreenContent(
            uiState = sampleUiStateWithResult,
            onInputUrlChanged = {},
            onClearInput = {},
            onAppleMapsTargetChanged = {},
            onGoogleMapsTargetChanged = {},
            onWazeTargetChanged = {},
            onToggleAutoRedirect = {},
            onOpenUrl = {},
            onCopyUrl = { _: String, _: String -> },
            onOpenLinkSettings = {}
        )
    }
}
