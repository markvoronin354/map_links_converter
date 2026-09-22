package com.markvoronin.maplinksconverter

import com.markvoronin.maplinksconverter.data.AppleMapsConverter
import com.markvoronin.maplinksconverter.data.MapLinkSource
import com.markvoronin.maplinksconverter.data.MapLinkType
import com.markvoronin.maplinksconverter.data.MapTargetApp
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
    fun testExtractAppleMapsUrl_shortAppleMapsUrl() {
        val input = "https://maps.apple/p/EfDaGc5RHNY4Q0"
        val extracted = AppleMapsConverter.extractAppleMapsUrl(input)
        assertEquals("https://maps.apple/p/EfDaGc5RHNY4Q0", extracted)
    }

    @Test
    fun testConvert_shortAppleMapsUrl() {
        val input = "https://maps.apple/p/EfDaGc5RHNY4Q0"
        val result = AppleMapsConverter.convert(input)

        assertTrue(result.isSuccess)
        assertEquals(MapLinkSource.APPLE_MAPS, result.linkSource)
        assertEquals("https://www.google.com/maps/search/?api=1&query=https%3A%2F%2Fmaps.apple%2Fp%2FEfDaGc5RHNY4Q0", result.googleMapsUrl)
    }

    @Test
    fun testConvert_invalidInput() {
        val input = "https://www.example.com/not-a-map-link"
        val result = AppleMapsConverter.convert(input)

        assertFalse(result.isSuccess)
        assertNotNull(result.errorMessage)
    }
}
