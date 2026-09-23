package com.markvoronin.maplinksconverter.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.markvoronin.maplinksconverter.data.AppleMapsConverter
import com.markvoronin.maplinksconverter.data.ConversionResult
import com.markvoronin.maplinksconverter.data.MapLinkSource
import com.markvoronin.maplinksconverter.data.MapTargetApp
import com.markvoronin.maplinksconverter.data.UrlExpander
import com.markvoronin.maplinksconverter.data.toLinkSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val inputUrl: String = "",
    val conversionResult: ConversionResult? = null,
    val autoRedirectEnabled: Boolean = true,
    val appleMapsTarget: MapTargetApp = MapTargetApp.GOOGLE_MAPS,
    val googleMapsTarget: MapTargetApp = MapTargetApp.WAZE,
    val wazeTarget: MapTargetApp = MapTargetApp.GOOGLE_MAPS,
    val isLoading: Boolean = false,
    val userNotice: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences = application.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _uiState = MutableStateFlow(
        MainUiState(
            autoRedirectEnabled = prefs.getBoolean(KEY_AUTO_REDIRECT, true),
            appleMapsTarget = validateTarget(loadTargetPref(KEY_TARGET_APPLE_MAPS, MapTargetApp.GOOGLE_MAPS), MapLinkSource.APPLE_MAPS),
            googleMapsTarget = validateTarget(loadTargetPref(KEY_TARGET_GOOGLE_MAPS, MapTargetApp.WAZE), MapLinkSource.GOOGLE_MAPS),
            wazeTarget = validateTarget(loadTargetPref(KEY_TARGET_WAZE, MapTargetApp.GOOGLE_MAPS), MapLinkSource.WAZE)
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun onInputUrlChanged(newUrl: String) {
        if (newUrl.isBlank()) {
            _uiState.update {
                it.copy(
                    inputUrl = newUrl,
                    conversionResult = null,
                    isLoading = false
                )
            }
            return
        }

        val targetApp = getTargetAppForInput(newUrl)
        val initialResult = AppleMapsConverter.convert(newUrl, targetApp)

        if (initialResult.isSuccess && initialResult.hasLocationData()) {
            _uiState.update {
                it.copy(
                    inputUrl = newUrl,
                    conversionResult = initialResult,
                    isLoading = false
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                inputUrl = newUrl,
                conversionResult = initialResult,
                isLoading = UrlExpander.isShortenedUrl(newUrl)
            )
        }

        viewModelScope.launch {
            val expanded = UrlExpander.expandUrlIfNeeded(newUrl)
            if (expanded != newUrl) {
                val updatedTargetApp = getTargetAppForInput(expanded)
                val expandedResult = AppleMapsConverter.convert(expanded, updatedTargetApp)
                _uiState.update {
                    it.copy(
                        conversionResult = expandedResult,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(isLoading = false)
                }
            }
        }
    }

    fun processIntentInput(input: String) {
        onInputUrlChanged(input)
    }

    fun getTargetAppForInput(input: String): MapTargetApp {
        val extractedUrl = AppleMapsConverter.extractAppleMapsUrl(input) ?: input
        return when {
            extractedUrl.contains("apple", ignoreCase = true) -> validateTarget(_uiState.value.appleMapsTarget, MapLinkSource.APPLE_MAPS)
            extractedUrl.contains("google", ignoreCase = true) || extractedUrl.contains("goo.gl", ignoreCase = true) -> validateTarget(_uiState.value.googleMapsTarget, MapLinkSource.GOOGLE_MAPS)
            extractedUrl.contains("waze", ignoreCase = true) -> validateTarget(_uiState.value.wazeTarget, MapLinkSource.WAZE)
            else -> MapTargetApp.GOOGLE_MAPS
        }
    }

    fun getTargetAppForSource(source: MapLinkSource): MapTargetApp {
        return when (source) {
            MapLinkSource.APPLE_MAPS -> validateTarget(_uiState.value.appleMapsTarget, MapLinkSource.APPLE_MAPS)
            MapLinkSource.GOOGLE_MAPS -> validateTarget(_uiState.value.googleMapsTarget, MapLinkSource.GOOGLE_MAPS)
            MapLinkSource.WAZE -> validateTarget(_uiState.value.wazeTarget, MapLinkSource.WAZE)
            MapLinkSource.UNKNOWN -> MapTargetApp.GOOGLE_MAPS
        }
    }

    private fun validateTarget(target: MapTargetApp, source: MapLinkSource): MapTargetApp {
        if (target.toLinkSource() == source) {
            return when (source) {
                MapLinkSource.APPLE_MAPS -> MapTargetApp.GOOGLE_MAPS
                MapLinkSource.GOOGLE_MAPS -> MapTargetApp.WAZE
                MapLinkSource.WAZE -> MapTargetApp.GOOGLE_MAPS
                MapLinkSource.UNKNOWN -> MapTargetApp.GOOGLE_MAPS
            }
        }
        return target
    }

    fun setAppleMapsTarget(target: MapTargetApp) {
        val validTarget = validateTarget(target, MapLinkSource.APPLE_MAPS)
        prefs.edit().putString(KEY_TARGET_APPLE_MAPS, validTarget.name).apply()
        _uiState.update { it.copy(appleMapsTarget = validTarget) }
        reconvertCurrentInput()
    }

    fun setGoogleMapsTarget(target: MapTargetApp) {
        val validTarget = validateTarget(target, MapLinkSource.GOOGLE_MAPS)
        prefs.edit().putString(KEY_TARGET_GOOGLE_MAPS, validTarget.name).apply()
        _uiState.update { it.copy(googleMapsTarget = validTarget) }
        reconvertCurrentInput()
    }

    fun setWazeTarget(target: MapTargetApp) {
        val validTarget = validateTarget(target, MapLinkSource.WAZE)
        prefs.edit().putString(KEY_TARGET_WAZE, validTarget.name).apply()
        _uiState.update { it.copy(wazeTarget = validTarget) }
        reconvertCurrentInput()
    }

    fun onTargetAppForCurrentResultChanged(target: MapTargetApp) {
        val currentResult = _uiState.value.conversionResult ?: return
        val validTarget = validateTarget(target, currentResult.linkSource)
        val updatedConvertedUrl = when (validTarget) {
            MapTargetApp.GOOGLE_MAPS -> currentResult.googleMapsUrl
            MapTargetApp.WAZE -> currentResult.wazeUrl
            MapTargetApp.APPLE_MAPS -> currentResult.appleMapsUrl
        }
        _uiState.update {
            it.copy(
                conversionResult = currentResult.copy(
                    targetApp = validTarget,
                    convertedUrl = updatedConvertedUrl
                )
            )
        }
    }

    fun setAutoRedirectEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_REDIRECT, enabled).apply()
        _uiState.update { it.copy(autoRedirectEnabled = enabled) }
    }

    private fun reconvertCurrentInput() {
        val currentInput = _uiState.value.inputUrl
        if (currentInput.isNotBlank()) {
            onInputUrlChanged(currentInput)
        }
    }

    fun clearInput() {
        _uiState.update {
            it.copy(
                inputUrl = "",
                conversionResult = null,
                isLoading = false
            )
        }
    }

    fun clearNotice() {
        _uiState.update { it.copy(userNotice = null) }
    }

    fun isAutoRedirectEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_REDIRECT, true)
    }

    private fun loadTargetPref(key: String, default: MapTargetApp): MapTargetApp {
        val name = prefs.getString(key, null) ?: return default
        return try {
            MapTargetApp.valueOf(name)
        } catch (e: Exception) {
            default
        }
    }

    companion object {
        private const val PREFS_NAME = "map_links_converter_prefs"
        private const val KEY_AUTO_REDIRECT = "auto_redirect_enabled"
        private const val KEY_TARGET_APPLE_MAPS = "target_apple_maps"
        private const val KEY_TARGET_GOOGLE_MAPS = "target_google_maps"
        private const val KEY_TARGET_WAZE = "target_waze"
    }
}
