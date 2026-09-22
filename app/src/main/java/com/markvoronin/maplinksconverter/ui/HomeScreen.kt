package com.markvoronin.maplinksconverter.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markvoronin.maplinksconverter.data.ConversionResult
import com.markvoronin.maplinksconverter.data.MapLinkSource
import com.markvoronin.maplinksconverter.data.MapLinkType
import com.markvoronin.maplinksconverter.data.MapTargetApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Map Links Converter",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InputCard(
                inputUrl = uiState.inputUrl,
                isLoading = uiState.isLoading,
                onInputChange = { viewModel.onInputUrlChanged(it) },
                onClear = { viewModel.clearInput() },
                onPaste = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = clipboard.primaryClip
                    if (clip != null && clip.itemCount > 0) {
                        val pastedText = clip.getItemAt(0).text?.toString() ?: ""
                        if (pastedText.isNotBlank()) {
                            viewModel.onInputUrlChanged(pastedText)
                            Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            uiState.conversionResult?.let { result ->
                ConversionResultCard(
                    result = result,
                    onOpenUrl = { url ->
                        openUrlInBrowserOrMaps(context, url)
                    },
                    onCopyUrl = { label, url ->
                        copyToClipboard(context, label, url)
                    }
                )
            }

            TargetSelectionCard(
                appleMapsTarget = uiState.appleMapsTarget,
                googleMapsTarget = uiState.googleMapsTarget,
                wazeTarget = uiState.wazeTarget,
                onAppleMapsTargetChanged = { viewModel.setAppleMapsTarget(it) },
                onGoogleMapsTargetChanged = { viewModel.setGoogleMapsTarget(it) },
                onWazeTargetChanged = { viewModel.setWazeTarget(it) }
            )

            AutoRedirectCard(
                autoRedirectEnabled = uiState.autoRedirectEnabled,
                onToggleAutoRedirect = { viewModel.setAutoRedirectEnabled(it) }
            )

            InstructionsCard()
        }
    }
}

@Composable
private fun InputCard(
    inputUrl: String,
    isLoading: Boolean,
    onInputChange: (String) -> Unit,
    onClear: () -> Unit,
    onPaste: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Convert Map Link",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = inputUrl,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Paste Apple Maps, Google Maps, or Waze link...") },
                singleLine = false,
                maxLines = 3,
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        if (inputUrl.isNotEmpty()) {
                            IconButton(onClick = onClear) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onPaste) {
                    Icon(
                        Icons.Default.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Paste Clipboard")
                }
            }
        }
    }
}

@Composable
private fun TargetSelectionCard(
    appleMapsTarget: MapTargetApp,
    googleMapsTarget: MapTargetApp,
    wazeTarget: MapTargetApp,
    onAppleMapsTargetChanged: (MapTargetApp) -> Unit,
    onGoogleMapsTargetChanged: (MapTargetApp) -> Unit,
    onWazeTargetChanged: (MapTargetApp) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(
                    text = "Open Links In...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            TargetOptionRow(
                label = "Apple Maps links open in:",
                selectedTarget = appleMapsTarget,
                onTargetSelected = onAppleMapsTargetChanged
            )

            TargetOptionRow(
                label = "Google Maps links open in:",
                selectedTarget = googleMapsTarget,
                onTargetSelected = onGoogleMapsTargetChanged
            )

            TargetOptionRow(
                label = "Waze links open in:",
                selectedTarget = wazeTarget,
                onTargetSelected = onWazeTargetChanged
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetOptionRow(
    label: String,
    selectedTarget: MapTargetApp,
    onTargetSelected: (MapTargetApp) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTarget == MapTargetApp.GOOGLE_MAPS,
                onClick = { onTargetSelected(MapTargetApp.GOOGLE_MAPS) },
                label = { Text("Google Maps") }
            )

            FilterChip(
                selected = selectedTarget == MapTargetApp.WAZE,
                onClick = { onTargetSelected(MapTargetApp.WAZE) },
                label = { Text("Waze") }
            )
        }
    }
}

@Composable
private fun ConversionResultCard(
    result: ConversionResult,
    onOpenUrl: (String) -> Unit,
    onCopyUrl: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (result.isSuccess) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (result.isSuccess) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Successfully Converted",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Conversion Issue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (result.isSuccess) {
                val sourceLabel = when (result.linkSource) {
                    MapLinkSource.APPLE_MAPS -> "Apple Maps"
                    MapLinkSource.GOOGLE_MAPS -> "Google Maps"
                    MapLinkSource.WAZE -> "Waze"
                    MapLinkSource.UNKNOWN -> "Map Link"
                }

                val targetLabel = if (result.targetApp == MapTargetApp.WAZE) "Waze" else "Google Maps"

                val typeLabel: String
                val typeIcon: ImageVector
                when (result.linkType) {
                    MapLinkType.SEARCH -> {
                        typeLabel = "Search"
                        typeIcon = Icons.Default.Search
                    }
                    MapLinkType.LOCATION -> {
                        typeLabel = "Location Pin"
                        typeIcon = Icons.Default.LocationOn
                    }
                    MapLinkType.DIRECTIONS -> {
                        typeLabel = "Directions"
                        typeIcon = Icons.Default.Directions
                    }
                    MapLinkType.UNKNOWN -> {
                        typeLabel = "Link"
                        typeIcon = Icons.Default.Map
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Source Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "From: $sourceLabel",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Target Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "To: $targetLabel",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Type Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = null,
                            modifier = Modifier
                                .height(16.dp)
                                .width(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                result.query?.let { DetailRow("Query / Place:", it) }
                result.coordinates?.let { DetailRow("Coordinates:", it) }
                result.address?.let { DetailRow("Address:", it) }
                result.origin?.let { DetailRow("Origin:", it) }
                result.destination?.let { DetailRow("Destination:", it) }
                result.travelMode?.let { DetailRow("Travel Mode:", it) }

                result.convertedUrl?.let { cUrl ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Converted Link ($targetLabel):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = cUrl,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onOpenUrl(cUrl) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open in $targetLabel")
                            }

                            OutlinedButton(
                                onClick = { onCopyUrl("$targetLabel Link", cUrl) }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Link")
                            }
                        }

                        // Alternative app button if available
                        val altUrl = if (result.targetApp == MapTargetApp.WAZE) result.googleMapsUrl else result.wazeUrl
                        val altName = if (result.targetApp == MapTargetApp.WAZE) "Google Maps" else "Waze"
                        if (!altUrl.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = { onOpenUrl(altUrl) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Or Open in $altName")
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = result.errorMessage ?: "Could not parse map link.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AutoRedirectCard(
    autoRedirectEnabled: Boolean,
    onToggleAutoRedirect: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = "Auto-redirect incoming links",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "When receiving map links from other apps, automatically open them in your chosen target app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = autoRedirectEnabled,
                onCheckedChange = onToggleAutoRedirect
            )
        }
    }
}

@Composable
private fun InstructionsCard() {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(
                    text = "Bypass Browser for Apple Maps Links",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = "On Android 12+, non-verified web links (like maps.apple.com) open in Chrome by default unless you assign them to Map Links Converter in System Settings.\n\n" +
                        "To open Apple Maps links directly in Map Links Converter without the browser:\n\n" +
                        "1. Tap 'Open Link Settings' below.\n" +
                        "2. Tap 'Open supported links' (or 'Add link').\n" +
                        "3. Check 'maps.apple.com' to allow this app to open them directly!\n\n" +
                        "Alternatively, you can always share a link directly to 'Map Links Converter' or use 'Paste Clipboard'.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = { openAppSupportedLinksSettings(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open Link Settings (Set Defaults)")
            }
        }
    }
}

private fun openAppSupportedLinksSettings(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(
                Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
        } else {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        try {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
        } catch (e2: Exception) {
            Toast.makeText(context, "Could not open settings: ${e2.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun openUrlInBrowserOrMaps(context: Context, url: String) {
    try {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        val pm = context.packageManager

        if (url.contains("waze", ignoreCase = true)) {
            val wazeTestIntent = Intent(Intent.ACTION_VIEW, Uri.parse("waze://"))
            val resolveInfo = pm.resolveActivity(wazeTestIntent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveInfo != null) {
                intent.setPackage("com.waze")
            }
        } else if (url.contains("google", ignoreCase = true)) {
            val gmapsTestIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps"))
            gmapsTestIntent.setPackage("com.google.android.apps.maps")
            val resolveInfo = pm.resolveActivity(gmapsTestIntent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveInfo != null) {
                intent.setPackage("com.google.android.apps.maps")
            }
        }

        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(fallbackIntent)
        } catch (e2: Exception) {
            Toast.makeText(context, "Could not open link: ${e2.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied $label to clipboard", Toast.LENGTH_SHORT).show()
}
