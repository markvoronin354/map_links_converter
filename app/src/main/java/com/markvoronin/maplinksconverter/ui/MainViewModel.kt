package com.markvoronin.maplinksconverter.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
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
    val targetApp: MapTargetApp = MapTargetApp.GOOGLE_MAPS,
    val isLoading: Boolean = false,
    val userNotice: String? = null,
) {
    val appleMapsTarget: MapTargetApp get() = targetApp
    val googleMapsTarget: MapTargetApp get() = targetApp
    val wazeTarget: MapTargetApp get() = targetApp
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences = application.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    private val _uiState = MutableStateFlow(
        MainUiState(
            autoRedirectEnabled = prefs.getBoolean(KEY_AUTO_REDIRECT, true),
            targetApp = loadSavedTargetApp()
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
        return _uiState.value.targetApp
    }

    fun getTargetAppForSource(source: MapLinkSource): MapTargetApp {
        return _uiState.value.targetApp
    }

    fun setTargetApp(target: MapTargetApp) {
        prefs.edit { putString(KEY_TARGET_APP, target.name) }
        _uiState.update { it.copy(targetApp = target) }
        reconvertCurrentInput()
    }

    fun setAppleMapsTarget(target: MapTargetApp) = setTargetApp(target)
    fun setGoogleMapsTarget(target: MapTargetApp) = setTargetApp(target)
    fun setWazeTarget(target: MapTargetApp) = setTargetApp(target)

    fun onTargetAppForCurrentResultChanged(target: MapTargetApp) {
        val currentResult = _uiState.value.conversionResult ?: return
        val updatedConvertedUrl = when (target) {
            MapTargetApp.GOOGLE_MAPS -> currentResult.googleMapsUrl
            MapTargetApp.WAZE -> currentResult.wazeUrl
            MapTargetApp.APPLE_MAPS -> currentResult.appleMapsUrl
        }
        _uiState.update {
            it.copy(
                conversionResult = currentResult.copy(
                    targetApp = target,
                    convertedUrl = updatedConvertedUrl
                )
            )
        }
    }

    fun setAutoRedirectEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AUTO_REDIRECT, enabled) }
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

    private fun loadSavedTargetApp(): MapTargetApp {
        val name = prefs.getString(KEY_TARGET_APP, null)
            ?: prefs.getString(KEY_TARGET_APPLE_MAPS, null)
            ?: prefs.getString(KEY_TARGET_GOOGLE_MAPS, null)
            ?: prefs.getString(KEY_TARGET_WAZE, null)
            ?: return MapTargetApp.GOOGLE_MAPS

        return try {
            MapTargetApp.valueOf(name)
        } catch (_: Exception) {
            MapTargetApp.GOOGLE_MAPS
        }
    }

    companion object {
        private const val PREFS_NAME = "map_links_converter_prefs"
        private const val KEY_AUTO_REDIRECT = "auto_redirect_enabled"
        private const val KEY_TARGET_APP = "target_app"
        private const val KEY_TARGET_APPLE_MAPS = "target_apple_maps"
        private const val KEY_TARGET_GOOGLE_MAPS = "target_google_maps"
        private const val KEY_TARGET_WAZE = "target_waze"
    }
}
