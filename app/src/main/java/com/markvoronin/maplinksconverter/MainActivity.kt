package com.markvoronin.maplinksconverter

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.markvoronin.maplinksconverter.data.AppleMapsConverter
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

        handleIncomingIntent(intent)

        setContent {
            MapLinksConverterTheme {
                HomeScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        val input = when (intent.action) {
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

        if (!input.isNullOrBlank()) {
            val isExternalIntent = intent.action == Intent.ACTION_VIEW || intent.action == Intent.ACTION_SEND

            lifecycleScope.launch {
                val expandedInput = UrlExpander.expandUrlIfNeeded(input)
                val targetApp = viewModel.getTargetAppForInput(expandedInput)
                val conversionResult = AppleMapsConverter.convert(expandedInput, targetApp)

                if (isExternalIntent && conversionResult.isSuccess && viewModel.isAutoRedirectEnabled()) {
                    val redirectUrl = conversionResult.convertedUrl
                    val isWazeTarget = conversionResult.targetApp == MapTargetApp.WAZE
                    val appName = if (isWazeTarget) "Waze" else "Google Maps"

                    if (!redirectUrl.isNullOrBlank()) {
                        try {
                            val redirectIntent = Intent(Intent.ACTION_VIEW, Uri.parse(redirectUrl))
                            val pm = packageManager
                            if (isWazeTarget) {
                                val wazeTestIntent = Intent(Intent.ACTION_VIEW, Uri.parse("waze://"))
                                val resolveInfo = pm.resolveActivity(wazeTestIntent, PackageManager.MATCH_DEFAULT_ONLY)
                                if (resolveInfo != null) {
                                    redirectIntent.setPackage("com.waze")
                                }
                            } else {
                                val gmapsTestIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps"))
                                gmapsTestIntent.setPackage("com.google.android.apps.maps")
                                val resolveInfo = pm.resolveActivity(gmapsTestIntent, PackageManager.MATCH_DEFAULT_ONLY)
                                if (resolveInfo != null) {
                                    redirectIntent.setPackage("com.google.android.apps.maps")
                                }
                            }

                            startActivity(redirectIntent)
                            Toast.makeText(this@MainActivity, "Opening in $appName...", Toast.LENGTH_SHORT).show()
                            finish()
                            return@launch
                        } catch (e: Exception) {
                            try {
                                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(redirectUrl))
                                startActivity(fallbackIntent)
                                Toast.makeText(this@MainActivity, "Opening in $appName...", Toast.LENGTH_SHORT).show()
                                finish()
                                return@launch
                            } catch (e2: Exception) {
                                Toast.makeText(this@MainActivity, "Could not launch $appName: ${e2.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }

                viewModel.processIntentInput(expandedInput)
            }
        }
    }
}
