package com.markvoronin.maplinksconverter

import com.markvoronin.maplinksconverter.data.AppleMapsConverter
import com.markvoronin.maplinksconverter.data.MapLinkSource
import com.markvoronin.maplinksconverter.data.MapLinkType
import com.markvoronin.maplinksconverter.data.MapTargetApp
import com.markvoronin.maplinksconverter.data.UrlExpander
import com.markvoronin.maplinksconverter.data.getEffectiveTargetApp
import com.markvoronin.maplinksconverter.data.toLinkSource
import org.junit.Assert.*
import org.junit.Test

class AppleMapsConverterTest {

    @Test
    fun testExtractAppleMapsUrl_directUrl() {
        val input = "https://maps.apple.com/?q=Apple+Park&ll=37.33182,-122.03118"
        val extracted = AppleMapsConverter.extractAppleMapsUrl(input)
        assertEquals("https://maps.apple.com/?q=Apple+Park&ll=37.33182,-122.03118", extracted)
    }

    @Test
    fun testExtractAppleMapsUrl_embeddedInText() {
        val input = "Check out this place: https://maps.apple.com/?q=Eiffel+Tower&ll=48.8584,2.2945 hope you like it!"
        val extracted = AppleMapsConverter.extractAppleMapsUrl(input)
        assertEquals("https://maps.apple.com/?q=Eiffel+Tower&ll=48.8584,2.2945", extracted)
    }

    @Test
    fun testExtractAppleMapsUrl_noScheme() {
        val input = "maps.apple.com/?q=Cupertino"
        val extracted = AppleMapsConverter.extractAppleMapsUrl(input)
        assertEquals("https://maps.apple.com/?q=Cupertino", extracted)
    }

    @Test
    fun testConvert_coordinatesOnly() {
        val input = "https://maps.apple.com/?ll=37.7749,-122.4194"
        val result = AppleMapsConverter.convert(input)

        assertTrue(result.isSuccess)
        assertEquals(MapLinkSource.APPLE_MAPS, result.linkSource)
        assertEquals(MapLinkType.LOCATION, result.linkType)
        assertEquals("37.7749,-122.4194", result.coordinates)
        assertEquals("https://www.google.com/maps/search/?api=1&query=37.7749%2C-122.4194", result.googleMapsUrl)
        assertEquals("geo:37.7749,-122.4194?q=37.7749,-122.4194", result.geoUri)
    }

    @Test
    fun testConvert_appleMapsToWaze() {
        val input = "https://maps.apple.com/?q=Apple%20Park&ll=37.33182,-122.03118"
        val result = AppleMapsConverter.convert(input, MapTargetApp.WAZE)

        assertTrue(result.isSuccess)
        assertEquals(MapLinkSource.APPLE_MAPS, result.linkSource)
        assertEquals(MapTargetApp.WAZE, result.targetApp)
        assertEquals("https://waze.com/ul?ll=37.33182%2C-122.03118&q=Apple+Park&navigate=yes", result.convertedUrl)
        assertEquals("https://waze.com/ul?ll=37.33182%2C-122.03118&q=Apple+Park&navigate=yes", result.wazeUrl)
    }

    @Test
    fun testConvert_googleMapsToWaze() {
        val input = "https://www.google.com/maps/search/?api=1&query=37.7749,-122.4194"
        val result = AppleMapsConverter.convert(input, MapTargetApp.WAZE)

        assertTrue(result.isSuccess)
        assertEquals(MapLinkSource.GOOGLE_MAPS, result.linkSource)
        assertEquals(MapTargetApp.WAZE, result.targetApp)
        assertEquals("https://waze.com/ul?ll=37.7749%2C-122.4194&navigate=yes", result.convertedUrl)
    }

    @Test
    fun testConvert_googleMapsPlaceToWaze() {
        val input = "https://www.google.com/maps/place/Eiffel+Tower/@48.8583701,2.2922926,17z"
        val result = AppleMapsConverter.convert(input, MapTargetApp.WAZE)

        assertTrue(result.isSuccess)
        assertEquals(MapLinkSource.GOOGLE_MAPS, result.linkSource)
        assertEquals("48.8583701,2.2922926", result.coordinates)
        assertEquals("Eiffel Tower", result.query)
        assertEquals("https://waze.com/ul?ll=48.8583701%2C2.2922926&q=Eiffel+Tower&navigate=yes", result.convertedUrl)
    }

    @Test
    fun testConvert_googleMapsToAppleMaps() {
        val input = "https://www.google.com/maps/place/Eiffel+Tower/@48.8583701,2.2922926,17z"
        val result = AppleMapsConverter.convert(input, MapTargetApp.APPLE_MAPS)

        assertTrue(result.isSuccess)
        assertEquals(MapLinkSource.GOOGLE_MAPS, result.linkSource)
        assertEquals(MapTargetApp.APPLE_MAPS, result.targetApp)
        assertEquals("48.8583701,2.2922926", result.coordinates)
        assertEquals("Eiffel Tower", result.query)
        assertEquals("https://maps.apple.com/?q=Eiffel+Tower&ll=48.8583701,2.2922926", result.convertedUrl)
        assertEquals("https://maps.apple.com/?q=Eiffel+Tower&ll=48.8583701,2.2922926", result.appleMapsUrl)
    }

    @Test
    fun testConvert_wazeToAppleMaps() {
        val input = "https://waze.com/ul?ll=37.7749,-122.4194&q=San+Francisco&navigate=yes"
        val result = AppleMapsConverter.convert(input, MapTargetApp.APPLE_MAPS)

        assertTrue(result.isSuccess)
        assertEquals(MapLinkSource.WAZE, result.linkSource)
        assertEquals(MapTargetApp.APPLE_MAPS, result.targetApp)
        assertEquals("37.7749,-122.4194", result.coordinates)
        assertEquals("https://maps.apple.com/?q=San+Francisco&ll=37.7749,-122.4194", result.convertedUrl)
        assertEquals("https://maps.apple.com/?q=San+Francisco&ll=37.7749,-122.4194", result.appleMapsUrl)
    }

    @Test
    fun testConvert_googleMapsDirectionsToAppleMaps() {
        val input = "https://www.google.com/maps/dir/?api=1&origin=Cupertino&destination=San+Francisco&travelmode=driving"
        val result = AppleMapsConverter.convert(input, MapTargetApp.APPLE_MAPS)

        assertTrue(result.isSuccess)
        assertEquals(MapLinkSource.GOOGLE_MAPS, result.linkSource)
        assertEquals(MapTargetApp.APPLE_MAPS, result.targetApp)
        assertEquals(MapLinkType.DIRECTIONS, result.linkType)
        assertEquals("https://maps.apple.com/?daddr=San+Francisco&saddr=Cupertino&dirflg=d", result.convertedUrl)
        assertEquals("https://maps.apple.com/?daddr=San+Francisco&saddr=Cupertino&dirflg=d", result.appleMapsUrl)
    }

    @Test
    fun testConvert_googleMapsToGoogleMaps_respectsTargetApp() {
        val input = "https://www.google.com/maps/search/?api=1&query=37.7749,-122.4194"
        val result = AppleMapsConverter.convert(input, MapTargetApp.GOOGLE_MAPS)

        assertTrue(result.isSuccess)
        assertEquals(MapLinkSource.GOOGLE_MAPS, result.linkSource)
        assertEquals(MapTargetApp.GOOGLE_MAPS, result.targetApp)
        assertEquals("https://www.google.com/maps/search/?api=1&query=37.7749%2C-122.4194", result.convertedUrl)
    }

    @Test
    fun testConvert_appleMapsToAppleMaps_respectsTargetApp() {
        val input = "https://maps.apple.com/?ll=37.7749,-122.4194"
        val result = AppleMapsConverter.convert(input, MapTargetApp.APPLE_MAPS)

        assertTrue(result.isSuccess)
        assertEquals(MapLinkSource.APPLE_MAPS, result.linkSource)
        assertEquals(MapTargetApp.APPLE_MAPS, result.targetApp)
        assertEquals("https://maps.apple.com/?q=37.7749,-122.4194&ll=37.7749,-122.4194", result.convertedUrl)
    }

    @Test
    fun testConvert_wazeToWaze_respectsTargetApp() {
        val input = "https://waze.com/ul?ll=37.7749,-122.4194&navigate=yes"
        val result = AppleMapsConverter.convert(input, MapTargetApp.WAZE)

        assertTrue(result.isSuccess)
        assertEquals(MapLinkSource.WAZE, result.linkSource)
        assertEquals(MapTargetApp.WAZE, result.targetApp)
        assertEquals("https://waze.com/ul?ll=37.7749%2C-122.4194&navigate=yes", result.convertedUrl)
    }

    @Test
    fun testToLinkSource() {
        assertEquals(MapLinkSource.GOOGLE_MAPS, MapTargetApp.GOOGLE_MAPS.toLinkSource())
        assertEquals(MapLinkSource.WAZE, MapTargetApp.WAZE.toLinkSource())
        assertEquals(MapLinkSource.APPLE_MAPS, MapTargetApp.APPLE_MAPS.toLinkSource())
    }

    @Test
    fun testConvert_wazeUrlWithCoordinates() {
        val input = "https://waze.com/ul?ll=37.7749,-122.4194&navigate=yes"
        val result = AppleMapsConverter.convert(input, MapTargetApp.GOOGLE_MAPS)

        assertTrue(result.isSuccess)
        assertEquals(MapLinkSource.WAZE, result.linkSource)
        assertEquals(MapLinkType.LOCATION, result.linkType)
        assertEquals("37.7749,-122.4194", result.coordinates)
        assertEquals("https://www.google.com/maps/search/?api=1&query=37.7749%2C-122.4194", result.convertedUrl)
    }

    @Test
    fun testConvert_wazeUrlLatLonToAppleMaps() {
        val input = "https://waze.com/ul?lat=37.7749&lon=-122.4194&navigate=yes"
        val result = AppleMapsConverter.convert(input, MapTargetApp.APPLE_MAPS)

        assertTrue(result.isSuccess)
        assertEquals(MapLinkSource.WAZE, result.linkSource)
        assertEquals("37.7749,-122.4194", result.coordinates)
        assertEquals("https://maps.apple.com/?q=37.7749,-122.4194&ll=37.7749,-122.4194", result.appleMapsUrl)
        assertEquals("https://maps.apple.com/?q=37.7749,-122.4194&ll=37.7749,-122.4194", result.convertedUrl)
    }

    @Test
    fun testConvert_wazeShortUrlToAppleMaps_producesAppleMapsUrl() {
        val input = "https://waze.com/ul/hdhv7gd6bb"
        val result = AppleMapsConverter.convert(input, MapTargetApp.APPLE_MAPS)

        assertTrue(result.isSuccess)
        assertEquals(MapLinkSource.WAZE, result.linkSource)
        assertTrue(result.appleMapsUrl?.startsWith("https://maps.apple.com/") == true)
        assertEquals("https://maps.apple.com/?q=https%3A%2F%2Fwaze.com%2Ful%2Fhdhv7gd6bb", result.convertedUrl)
    }

    @Test
    fun testExtractAppleMapsUrl_shortAppleMapsUrl() {
        val input = "https://maps.apple/p/EfDaGc5RHNY4Q0"
        val extracted = AppleMapsConverter.extractAppleMapsUrl(input)
        assertEquals("https://maps.apple/p/EfDaGc5RHNY4Q0", extracted)
    }

    @Test
    fun testConvert_shortApplePlaceHash_requiresExpansion() {
        val input = "https://maps.apple/p/EfDaGc5RHNY4Q0"
        val result = AppleMapsConverter.convert(input, MapTargetApp.GOOGLE_MAPS)

        assertTrue(result.isSuccess)
        assertEquals(MapLinkSource.APPLE_MAPS, result.linkSource)
        assertFalse(result.hasLocationData())
        assertTrue(UrlExpander.isShortenedUrl(input))
    }

    @Test
    fun testIsShortenedUrl_appleRouteUrl() {
        assertTrue(UrlExpander.isShortenedUrl("https://maps.apple/r/_6JUrN6eGI04Wb"))
        assertTrue(UrlExpander.isShortenedUrl("https://maps.apple.com/r/_6JUrN6eGI04Wb"))
        assertTrue(UrlExpander.isShortenedUrl("https://maps.apple.com/?auid=17384920183&lsp=9902"))
    }

    @Test
    fun testConvert_appleMapsPlaceQuestionMarkAddress() {
        val input = "https://maps.apple.com/?place?address=2000%20Panama%20Blvd,%20Englewood,%20FL%2034224"
        val result = AppleMapsConverter.convert(input, MapTargetApp.GOOGLE_MAPS)

        assertTrue(result.isSuccess)
        assertEquals(MapLinkSource.APPLE_MAPS, result.linkSource)
        assertEquals("2000 Panama Blvd, Englewood, FL 34224", result.address)
        assertTrue(result.googleMapsUrl?.contains("2000+Panama+Blvd") == true)
    }

    @Test
    fun testConvert_bareRootUrl() {
        val input = "https://maps.apple.com"
        val result = AppleMapsConverter.convert(input)

        assertFalse(result.isSuccess)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun testIsShortenedUrl_wazeShortUrl() {
        assertTrue(UrlExpander.isShortenedUrl("https://waze.com/ul/hdhv7gd6bb"))
        assertTrue(UrlExpander.isShortenedUrl("https://ul.waze.com/ul/hdhv7gd6bb"))
        assertFalse(UrlExpander.isShortenedUrl("https://waze.com/ul?ll=37.7749,-122.4194&navigate=yes"))
    }

    @Test
    fun testConvert_wazeLlPrefixCoordinates() {
        val input = "https://www.waze.com/live-map/directions?to=ll.26.933777%2C-82.225821"
        val result = AppleMapsConverter.convert(input, MapTargetApp.GOOGLE_MAPS)

        assertTrue(result.isSuccess)
        assertEquals("26.933777,-82.225821", result.coordinates)
        val googleMapsUrl = requireNotNull(result.googleMapsUrl)
        assertFalse(googleMapsUrl.contains("ll."))
        assertTrue(googleMapsUrl.contains("26.933777%2C-82.225821"))
    }

    @Test
    fun testConvert_invalidInput() {
        val input = "https://www.example.com/not-a-map-link"
        val result = AppleMapsConverter.convert(input)

        assertFalse(result.isSuccess)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun testConvert_geoUriAddress() {
        val input = "geo:0,0?q=332%20Cocoanut%20Ave%2C%20Sarasota%2C%20FL%2034236%2C%20USA"
        val result = AppleMapsConverter.convert(input, MapTargetApp.GOOGLE_MAPS)

        assertTrue(result.isSuccess)
        assertEquals("332 Cocoanut Ave, Sarasota, FL 34236, USA", result.query)
        assertNull(result.coordinates)
        assertEquals("https://www.google.com/maps/search/?api=1&query=332+Cocoanut+Ave%2C+Sarasota%2C+FL+34236%2C+USA", result.googleMapsUrl)
        assertEquals("https://waze.com/ul?q=332+Cocoanut+Ave%2C+Sarasota%2C+FL+34236%2C+USA&navigate=yes", result.wazeUrl)
        assertEquals("https://maps.apple.com/?q=332+Cocoanut+Ave%2C+Sarasota%2C+FL+34236%2C+USA", result.appleMapsUrl)
    }

    @Test
    fun testConvert_geoUriCoordinates() {
        val input = "geo:27.3381,-82.5422"
        val result = AppleMapsConverter.convert(input, MapTargetApp.GOOGLE_MAPS)

        assertTrue(result.isSuccess)
        assertEquals("27.3381,-82.5422", result.coordinates)
        assertEquals("https://www.google.com/maps/search/?api=1&query=27.3381%2C-82.5422", result.googleMapsUrl)
        assertEquals("https://waze.com/ul?ll=27.3381%2C-82.5422&navigate=yes", result.wazeUrl)
    }

    @Test
    fun testConvert_geoUriCoordinatesWithLabel() {
        val input = "geo:0,0?q=27.3381,-82.5422(My+Place)"
        val result = AppleMapsConverter.convert(input, MapTargetApp.GOOGLE_MAPS)

        assertTrue(result.isSuccess)
        assertEquals("27.3381,-82.5422", result.coordinates)
        assertEquals("My Place", result.query)
        assertEquals("https://www.google.com/maps/search/?api=1&query=My+Place&center=27.3381%2C-82.5422", result.googleMapsUrl)
        assertEquals("https://waze.com/ul?ll=27.3381%2C-82.5422&q=My+Place&navigate=yes", result.wazeUrl)
    }

    @Test
    fun testConvert_rawAddressText() {
        val input = "332 Cocoanut Ave, Sarasota, FL 34236, USA"
        val result = AppleMapsConverter.convert(input, MapTargetApp.GOOGLE_MAPS)

        assertTrue(result.isSuccess)
        assertEquals("332 Cocoanut Ave, Sarasota, FL 34236, USA", result.query)
        assertEquals("https://www.google.com/maps/search/?api=1&query=332+Cocoanut+Ave%2C+Sarasota%2C+FL+34236%2C+USA", result.googleMapsUrl)
    }

    @Test
    fun testGetEffectiveTargetApp() {
        // Preferred is Google, input source is Google -> fallback to Waze
        assertEquals(MapTargetApp.WAZE, MapTargetApp.GOOGLE_MAPS.getEffectiveTargetApp(MapLinkSource.GOOGLE_MAPS))

        // Preferred is Google, input source is Apple -> keep Google
        assertEquals(MapTargetApp.GOOGLE_MAPS, MapTargetApp.GOOGLE_MAPS.getEffectiveTargetApp(MapLinkSource.APPLE_MAPS))

        // Preferred is Waze, input source is Waze -> fallback to Google
        assertEquals(MapTargetApp.GOOGLE_MAPS, MapTargetApp.WAZE.getEffectiveTargetApp(MapLinkSource.WAZE))

        // Preferred is Apple on Android -> maps to Google Maps (or Waze if input was Google Maps)
        assertEquals(MapTargetApp.GOOGLE_MAPS, MapTargetApp.APPLE_MAPS.getEffectiveTargetApp(MapLinkSource.APPLE_MAPS))
        assertEquals(MapTargetApp.WAZE, MapTargetApp.APPLE_MAPS.getEffectiveTargetApp(MapLinkSource.GOOGLE_MAPS))
        assertEquals(MapTargetApp.GOOGLE_MAPS, MapTargetApp.APPLE_MAPS.getEffectiveTargetApp(MapLinkSource.UNKNOWN))
    }
}
