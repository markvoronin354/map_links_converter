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
            appleMapsTarget = loadTargetPref(KEY_TARGET_APPLE_MAPS, MapTargetApp.GOOGLE_MAPS),
            googleMapsTarget = loadTargetPref(KEY_TARGET_GOOGLE_MAPS, MapTargetApp.WAZE),
            wazeTarget = loadTargetPref(KEY_TARGET_WAZE, MapTargetApp.GOOGLE_MAPS)
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
            extractedUrl.contains("apple", ignoreCase = true) -> _uiState.value.appleMapsTarget
            extractedUrl.contains("google", ignoreCase = true) || extractedUrl.contains("goo.gl", ignoreCase = true) -> _uiState.value.googleMapsTarget
            extractedUrl.contains("waze", ignoreCase = true) -> _uiState.value.wazeTarget
            else -> MapTargetApp.GOOGLE_MAPS
        }
    }

    fun getTargetAppForSource(source: MapLinkSource): MapTargetApp {
        return when (source) {
            MapLinkSource.APPLE_MAPS -> _uiState.value.appleMapsTarget
            MapLinkSource.GOOGLE_MAPS -> _uiState.value.googleMapsTarget
            MapLinkSource.WAZE -> _uiState.value.wazeTarget
            MapLinkSource.UNKNOWN -> MapTargetApp.GOOGLE_MAPS
        }
    }

    fun setAppleMapsTarget(target: MapTargetApp) {
        prefs.edit().putString(KEY_TARGET_APPLE_MAPS, target.name).apply()
        _uiState.update { it.copy(appleMapsTarget = target) }
        reconvertCurrentInput()
    }

    fun setGoogleMapsTarget(target: MapTargetApp) {
        prefs.edit().putString(KEY_TARGET_GOOGLE_MAPS, target.name).apply()
        _uiState.update { it.copy(googleMapsTarget = target) }
        reconvertCurrentInput()
    }

    fun setWazeTarget(target: MapTargetApp) {
        prefs.edit().putString(KEY_TARGET_WAZE, target.name).apply()
        _uiState.update { it.copy(wazeTarget = target) }
        reconvertCurrentInput()
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
