package com.markvoronin.maplinksconverter.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.markvoronin.maplinksconverter.data.MapTargetApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    HomeScreenContent(
        uiState = uiState,
        onInputUrlChanged = { viewModel.onInputUrlChanged(it) },
        onClearInput = { viewModel.clearInput() },
        onResultTargetChanged = { viewModel.onTargetAppForCurrentResultChanged(it) },
        onAppleMapsTargetChanged = { viewModel.setAppleMapsTarget(it) },
        onGoogleMapsTargetChanged = { viewModel.setGoogleMapsTarget(it) },
        onWazeTargetChanged = { viewModel.setWazeTarget(it) },
        onToggleAutoRedirect = { viewModel.setAutoRedirectEnabled(it) },
        onOpenUrl = { url -> openUrlInBrowserOrMaps(context, url) },
        onCopyUrl = { label, url -> copyToClipboard(context, label, url) },
        onOpenLinkSettings = { openAppSupportedLinksSettings(context) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    uiState: MainUiState,
    onInputUrlChanged: (String) -> Unit,
    onClearInput: () -> Unit,
    onResultTargetChanged: (MapTargetApp) -> Unit = {},
    onAppleMapsTargetChanged: (MapTargetApp) -> Unit,
    onGoogleMapsTargetChanged: (MapTargetApp) -> Unit,
    onWazeTargetChanged: (MapTargetApp) -> Unit,
    onToggleAutoRedirect: (Boolean) -> Unit,
    onOpenUrl: (String) -> Unit,
    onCopyUrl: (label: String, url: String) -> Unit,
    onOpenLinkSettings: () -> Unit,
) {
    PremiumHomeScreenContent(
        uiState = uiState,
        onInputUrlChanged = onInputUrlChanged,
        onClearInput = onClearInput,
        onResultTargetChanged = onResultTargetChanged,
        onAppleMapsTargetChanged = onAppleMapsTargetChanged,
        onGoogleMapsTargetChanged = onGoogleMapsTargetChanged,
        onWazeTargetChanged = onWazeTargetChanged,
        onToggleAutoRedirect = onToggleAutoRedirect,
        onOpenUrl = onOpenUrl,
        onCopyUrl = onCopyUrl,
        onOpenLinkSettings = onOpenLinkSettings,
    )
}

private fun openAppSupportedLinksSettings(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(
                Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                "package:${context.packageName}".toUri(),
            )
            context.startActivity(intent)
        } else {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                "package:${context.packageName}".toUri(),
            )
            context.startActivity(intent)
        }
    } catch (_: Exception) {
        try {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                "package:${context.packageName}".toUri()
            )
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}

private fun openUrlInBrowserOrMaps(context: Context, url: String) {
    try {
        val uri = url.toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        val pm = context.packageManager

        if (url.contains("waze", ignoreCase = true)) {
            val wazeTestIntent = Intent(Intent.ACTION_VIEW, "waze://".toUri())
            val resolveInfo = pm.resolveActivity(wazeTestIntent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveInfo != null) {
                intent.setPackage("com.waze")
            }
        } else if (url.contains("google", ignoreCase = true)) {
            val gmapsTestIntent = Intent(Intent.ACTION_VIEW, "https://www.google.com/maps".toUri())
            gmapsTestIntent.setPackage("com.google.android.apps.maps")
            val resolveInfo = pm.resolveActivity(gmapsTestIntent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveInfo != null) {
                intent.setPackage("com.google.android.apps.maps")
            }
        }

        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, url.toUri())
            context.startActivity(fallbackIntent)
        } catch (_: Exception) {
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}
