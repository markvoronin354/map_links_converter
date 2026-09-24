package com.markvoronin.maplinksconverter.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.markvoronin.maplinksconverter.data.MapTargetApp
import com.markvoronin.maplinksconverter.util.IntentUtils

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
        onTargetAppChanged = { viewModel.setTargetApp(it) },
        onAppleMapsTargetChanged = { viewModel.setTargetApp(it) },
        onGoogleMapsTargetChanged = { viewModel.setTargetApp(it) },
        onWazeTargetChanged = { viewModel.setTargetApp(it) },
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
    onTargetAppChanged: (MapTargetApp) -> Unit = {},
    onAppleMapsTargetChanged: (MapTargetApp) -> Unit = onTargetAppChanged,
    onGoogleMapsTargetChanged: (MapTargetApp) -> Unit = onTargetAppChanged,
    onWazeTargetChanged: (MapTargetApp) -> Unit = onTargetAppChanged,
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
        onTargetAppChanged = onTargetAppChanged,
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
    val preferredPackage = when {
        url.contains("waze", ignoreCase = true) -> "com.waze"
        url.contains("google", ignoreCase = true) -> "com.google.android.apps.maps"
        else -> null
    }
    IntentUtils.openExternalUrl(context, url, preferredPackage)
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}
