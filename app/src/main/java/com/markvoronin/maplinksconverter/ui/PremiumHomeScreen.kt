package com.markvoronin.maplinksconverter.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.markvoronin.maplinksconverter.data.ConversionResult
import com.markvoronin.maplinksconverter.data.MapLinkSource
import com.markvoronin.maplinksconverter.data.MapLinkType
import com.markvoronin.maplinksconverter.data.MapTargetApp
import com.markvoronin.maplinksconverter.ui.theme.AppleMapsBgDark
import com.markvoronin.maplinksconverter.ui.theme.AppleMapsBgLight
import com.markvoronin.maplinksconverter.ui.theme.AppleMapsColorDark
import com.markvoronin.maplinksconverter.ui.theme.AppleMapsColorLight
import com.markvoronin.maplinksconverter.ui.theme.GoogleMapsBgDark
import com.markvoronin.maplinksconverter.ui.theme.GoogleMapsBgLight
import com.markvoronin.maplinksconverter.ui.theme.GoogleMapsColorDark
import com.markvoronin.maplinksconverter.ui.theme.GoogleMapsColorLight
import com.markvoronin.maplinksconverter.ui.theme.WazeBgDark
import com.markvoronin.maplinksconverter.ui.theme.WazeBgLight
import com.markvoronin.maplinksconverter.ui.theme.WazeColorDark
import com.markvoronin.maplinksconverter.ui.theme.WazeColorLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumHomeScreenContent(
    uiState: MainUiState,
    onInputUrlChanged: (String) -> Unit,
    onClearInput: () -> Unit,
    onResultTargetChanged: (MapTargetApp) -> Unit = {},
    onTargetAppChanged: (MapTargetApp) -> Unit = {},
    onToggleAutoRedirect: (Boolean) -> Unit,
    onOpenUrl: (String) -> Unit,
    onCopyUrl: (label: String, url: String) -> Unit,
    onOpenLinkSettings: () -> Unit,
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp),
                            )
                        }

                        Column {
                            Text(
                                text = "Map Links",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Cross-Platform Converter",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PremiumInputCard(
                inputUrl = uiState.inputUrl,
                isLoading = uiState.isLoading,
                onInputChange = onInputUrlChanged,
                onClear = onClearInput,
                onPaste = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = clipboard.primaryClip
                    if ((clip != null) && (clip.itemCount > 0)) {
                        val pastedText = clip.getItemAt(0).text?.toString() ?: ""
                        if (pastedText.isNotBlank()) {
                            onInputUrlChanged(pastedText)
                        }
                    }
                },
            )

            uiState.conversionResult?.let { result ->
                PremiumConversionResultCard(
                    result = result,
                    onOpenUrl = onOpenUrl,
                    onCopyUrl = onCopyUrl,
                    onResultTargetChanged = onResultTargetChanged,
                )
            }

            PremiumTargetSelectionCard(
                selectedTarget = uiState.targetApp,
                onTargetSelected = onTargetAppChanged,
            )

            PremiumAutoRedirectCard(
                autoRedirectEnabled = uiState.autoRedirectEnabled,
                onToggleAutoRedirect = onToggleAutoRedirect,
            )

            PremiumInstructionsCard(onOpenLinkSettings = onOpenLinkSettings)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PremiumInputCard(
    inputUrl: String,
    isLoading: Boolean,
    onInputChange: (String) -> Unit,
    onClear: () -> Unit,
    onPaste: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(18.dp),
            ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "CONVERT MAP LINK",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    onClick = onPaste,
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Paste",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            OutlinedTextField(
                value = inputUrl,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Paste Apple Maps, Google Maps, or Waze URL...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                singleLine = false,
                maxLines = 3,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                trailingIcon = {
                    if (inputUrl.isNotEmpty()) {
                        IconButton(onClick = onClear) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                shape = RoundedCornerShape(12.dp)
            )

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PremiumConversionResultCard(
    result: ConversionResult,
    onOpenUrl: (String) -> Unit,
    onCopyUrl: (String, String) -> Unit,
    onResultTargetChanged: (MapTargetApp) -> Unit = {},
) {
    val containerBorder = if (result.isSuccess) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = containerBorder,
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (result.isSuccess) {
                // Source -> Target Visual Route Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    BrandPill(source = result.linkSource)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    TargetDropdownPill(
                        currentTarget = result.targetApp,
                        source = result.linkSource,
                        onTargetSelected = onResultTargetChanged
                    )
                }

                // Place Name & Type Badge
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = result.query ?: result.address ?: "Location Link",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        TypeBadge(type = result.linkType)
                    }

                    result.coordinates?.let { coords ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = coords,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Additional metadata grid if available
                if ((result.address != null) && (result.query != null)) {
                    MetadataRow(label = "Address", value = result.address)
                }
                result.origin?.let { MetadataRow(label = "Origin", value = it) }
                result.destination?.let { MetadataRow(label = "Destination", value = it) }

                // Converted URL Code Box
                result.convertedUrl?.let { cUrl ->
                    val targetName = when (result.targetApp) {
                        MapTargetApp.GOOGLE_MAPS -> "Google Maps"
                        MapTargetApp.WAZE -> "Waze"
                        MapTargetApp.APPLE_MAPS -> "Apple Maps"
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "CONVERTED URL ($targetName)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = cUrl,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Action Buttons
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onOpenUrl(cUrl) },
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open in $targetName", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { onCopyUrl("$targetName Link", cUrl) },
                                modifier = Modifier.weight(0.9f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy")
                            }
                        }
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = result.errorMessage ?: "Could not parse map link.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun TargetDropdownPill(
    currentTarget: MapTargetApp,
    source: MapLinkSource = MapLinkSource.UNKNOWN,
    onTargetSelected: (MapTargetApp) -> Unit
) {
    var expanded by remember { mutableStateOf(value = false) }

    val availableTargets = remember(source) {
        MapTargetApp.entries.filter { target ->
            when (source) {
                MapLinkSource.GOOGLE_MAPS -> target != MapTargetApp.GOOGLE_MAPS
                MapLinkSource.WAZE -> target != MapTargetApp.WAZE
                MapLinkSource.APPLE_MAPS -> target != MapTargetApp.APPLE_MAPS
                MapLinkSource.UNKNOWN -> true
            }
        }
    }

    val isDark = isSystemInDarkTheme()

    val (name, bgColor, textColor) = when (currentTarget) {
        MapTargetApp.GOOGLE_MAPS -> Triple(
            "Google Maps",
            if (isDark) GoogleMapsBgDark else GoogleMapsBgLight,
            if (isDark) GoogleMapsColorDark else GoogleMapsColorLight,
        )
        MapTargetApp.WAZE -> Triple(
            "Waze",
            if (isDark) WazeBgDark else WazeBgLight,
            if (isDark) WazeColorDark else WazeColorLight,
        )
        MapTargetApp.APPLE_MAPS -> Triple(
            "Apple Maps",
            if (isDark) AppleMapsBgDark else AppleMapsBgLight,
            if (isDark) AppleMapsColorDark else AppleMapsColorLight,
        )
    }

    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp),
            color = bgColor,
            border = BorderStroke(
                width = 1.dp,
                color = if (isDark) textColor.copy(alpha = 0.3f) else textColor.copy(alpha = 0.4f),
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select Target Map",
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            availableTargets.forEach { target ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val targetName = when (target) {
                                MapTargetApp.GOOGLE_MAPS -> "Google Maps"
                                MapTargetApp.WAZE -> "Waze"
                                MapTargetApp.APPLE_MAPS -> "Apple Maps"
                            }
                            Text(
                                text = targetName,
                                fontWeight = if (target == currentTarget) FontWeight.Bold else FontWeight.Normal
                            )
                            if (target == currentTarget) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        onTargetSelected(target)
                    }
                )
            }
        }
    }
}

@Composable
private fun BrandPill(source: MapLinkSource? = null, target: MapTargetApp? = null) {
    val name = when {
        (source == MapLinkSource.APPLE_MAPS) || (target == MapTargetApp.APPLE_MAPS) -> "Apple Maps"
        (source == MapLinkSource.GOOGLE_MAPS) || (target == MapTargetApp.GOOGLE_MAPS) -> "Google Maps"
        (source == MapLinkSource.WAZE) || (target == MapTargetApp.WAZE) -> "Waze"
        else -> "Map Link"
    }

    val isDark = isSystemInDarkTheme()

    val (bgColor, textColor) = when {
        (source == MapLinkSource.APPLE_MAPS) || (target == MapTargetApp.APPLE_MAPS) ->
            (if (isDark) AppleMapsBgDark else AppleMapsBgLight) to (if (isDark) AppleMapsColorDark else AppleMapsColorLight)
        (source == MapLinkSource.GOOGLE_MAPS) || (target == MapTargetApp.GOOGLE_MAPS) ->
            (if (isDark) GoogleMapsBgDark else GoogleMapsBgLight) to (if (isDark) GoogleMapsColorDark else GoogleMapsColorLight)
        (source == MapLinkSource.WAZE) || (target == MapTargetApp.WAZE) ->
            (if (isDark) WazeBgDark else WazeBgLight) to (if (isDark) WazeColorDark else WazeColorLight)
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(
                width = 1.dp,
                color = if (isDark) textColor.copy(alpha = 0.3f) else textColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun TypeBadge(type: MapLinkType) {
    val (label, icon) = when (type) {
        MapLinkType.SEARCH -> "Search" to Icons.Default.Search
        MapLinkType.LOCATION -> "Pin" to Icons.Default.LocationOn
        MapLinkType.DIRECTIONS -> "Directions" to Icons.Default.Directions
        MapLinkType.UNKNOWN -> "Link" to Icons.Default.Map
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(12.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PremiumTargetSelectionCard(
    selectedTarget: MapTargetApp,
    onTargetSelected: (MapTargetApp) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "TARGET APP ROUTING",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TargetSegmentRow(
                selectedTarget = selectedTarget,
                availableTargets = listOf(MapTargetApp.GOOGLE_MAPS, MapTargetApp.WAZE, MapTargetApp.APPLE_MAPS),
                onTargetSelected = onTargetSelected,
            )
        }
    }
}

@Composable
private fun TargetSegmentRow(
    selectedTarget: MapTargetApp,
    sourceLabel: String = "Open all links in",
    availableTargets: List<MapTargetApp> = listOf(MapTargetApp.GOOGLE_MAPS, MapTargetApp.WAZE, MapTargetApp.APPLE_MAPS),
    onTargetSelected: (MapTargetApp) -> Unit,
) {
    val selectedIndex = availableTargets.indexOf(selectedTarget).coerceAtLeast(0)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = sourceLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(3.dp)
        ) {
            val animatedIndex by animateFloatAsState(
                targetValue = selectedIndex.toFloat(),
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioLowBouncy
                ),
                label = "segmentIndex"
            )

            // Smoothly sliding selection indicator pill across available targets
            val count = availableTargets.size
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .layout { measurable, constraints ->
                        val itemWidth = constraints.maxWidth / count
                        val placeable = measurable.measure(
                            Constraints.fixed(itemWidth, constraints.maxHeight)
                        )
                        val xOffset = (itemWidth * animatedIndex).roundToInt()
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            placeable.place(xOffset, 0)
                        }
                    }
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                availableTargets.forEach { target ->
                    val label = when (target) {
                        MapTargetApp.GOOGLE_MAPS -> "Google"
                        MapTargetApp.WAZE -> "Waze"
                        MapTargetApp.APPLE_MAPS -> "Apple"
                    }
                    SegmentOption(
                        label = label,
                        isSelected = selectedTarget == target,
                        onClick = { onTargetSelected(target) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SegmentOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "segmentTextColor"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}

@Composable
private fun PremiumAutoRedirectCard(
    autoRedirectEnabled: Boolean,
    onToggleAutoRedirect: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Auto-Redirect Incoming Links",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Automatically open incoming links in target app without launching converter UI.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = autoRedirectEnabled,
                onCheckedChange = onToggleAutoRedirect,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun PremiumInstructionsCard(onOpenLinkSettings: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "BYPASS BROWSER DEFAULT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "On Android 12+, enable 'maps.app.goo.gl' and 'maps.apple.com' under 'Open by default' in System Settings, or use the Android Share menu to share links directly to Map Links Converter.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = onOpenLinkSettings,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Configure Default Link Handling", fontSize = 13.sp)
            }
        }
    }
}
