package com.markvoronin.maplinksconverter

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.markvoronin.maplinksconverter.data.AppleMapsConverter
import com.markvoronin.maplinksconverter.data.ConversionResult
import com.markvoronin.maplinksconverter.data.MapTargetApp
import com.markvoronin.maplinksconverter.data.UrlExpander
import com.markvoronin.maplinksconverter.ui.HomeScreen
import com.markvoronin.maplinksconverter.ui.MainViewModel
import com.markvoronin.maplinksconverter.ui.theme.MapLinksConverterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (handleIncomingIntentFast(intent)) {
            return
        }

        setContent {
            MapLinksConverterTheme {
                HomeScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntentFast(intent)
    }

    /**
     * Fast synchronously attempt to convert and launch target app for external intents.
     * Returns true if the activity launched target app and finished immediately.
     */
    private fun handleIncomingIntentFast(intent: Intent?): Boolean {
        if (intent == null) return false

        val input = extractInputFromIntent(intent) ?: return false
        val isExternalIntent = (intent.action == Intent.ACTION_VIEW) || (intent.action == Intent.ACTION_SEND)

        if (isExternalIntent && viewModel.isAutoRedirectEnabled()) {
            val targetApp = viewModel.getTargetAppForInput(input)
            val initialResult = AppleMapsConverter.convert(input, targetApp)

            if (initialResult.isSuccess && initialResult.hasLocationData()) {
                if (launchTargetApp(initialResult)) {
                    return true
                }
            }
        }

        // Async fallback if short URL expansion is needed or auto-redirect disabled
        lifecycleScope.launch {
            val targetApp = viewModel.getTargetAppForInput(input)
            var conversionResult = AppleMapsConverter.convert(input, targetApp)

            if (!conversionResult.isSuccess || !conversionResult.hasLocationData()) {
                val expandedInput = UrlExpander.expandUrlIfNeeded(input)
                if (expandedInput != input) {
                    val updatedTargetApp = viewModel.getTargetAppForInput(expandedInput)
                    conversionResult = AppleMapsConverter.convert(expandedInput, updatedTargetApp)
                }
            }

            if (isExternalIntent && conversionResult.isSuccess && viewModel.isAutoRedirectEnabled()) {
                if (launchTargetApp(conversionResult)) {
                    return@launch
                }
            }

            viewModel.processIntentInput(input)
        }

        return false
    }

    private fun extractInputFromIntent(intent: Intent): String? {
        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.dataString
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    intent.getStringExtra(Intent.EXTRA_TEXT)
                } else {
                    null
                }
            }
            else -> null
        }
    }

    private fun launchTargetApp(result: ConversionResult): Boolean {
        val redirectUrl = result.convertedUrl ?: return false
        val targetPackage = when (result.targetApp) {
            MapTargetApp.WAZE -> "com.waze"
            MapTargetApp.GOOGLE_MAPS -> "com.google.android.apps.maps"
            MapTargetApp.APPLE_MAPS -> null
        }

        val redirectIntent = Intent(Intent.ACTION_VIEW, redirectUrl.toUri()).apply {
            targetPackage?.let { setPackage(it) }
        }

        return try {
            startActivity(redirectIntent)
            finish()
            true
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, redirectUrl.toUri())
                startActivity(fallbackIntent)
                finish()
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
