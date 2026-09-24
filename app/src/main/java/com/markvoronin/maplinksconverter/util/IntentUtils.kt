package com.markvoronin.maplinksconverter.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.net.toUri

object IntentUtils {

    /**
     * Attempts to open a URL in an external app or browser, strictly excluding our own app
     * package (com.markvoronin.maplinksconverter) to prevent infinite activity re-launch loops.
     */
    fun openExternalUrl(context: Context, url: String, preferredPackage: String? = null): Boolean {
        val uri = try {
            url.toUri()
        } catch (_: Exception) {
            return false
        }

        val pm = context.packageManager

        // 1. If a preferred package is specified and installed, attempt to launch it
        if (!preferredPackage.isNullOrBlank()) {
            val preferredIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(preferredPackage)
            }
            val resolveInfo = pm.resolveActivity(preferredIntent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveInfo != null && resolveInfo.activityInfo.packageName != context.packageName) {
                return try {
                    context.startActivity(preferredIntent)
                    true
                } catch (_: Exception) {
                    false
                }
            }
        }

        // 2. Query all activities handling ACTION_VIEW for this URI
        val viewIntent = Intent(Intent.ACTION_VIEW, uri)
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(viewIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(viewIntent, PackageManager.MATCH_DEFAULT_ONLY)
        }

        // Filter out our own package so we never loop back to MapLinksConverter
        val externalPackages = resolveInfos
            .map { it.activityInfo.packageName }
            .filter { it != context.packageName }
            .distinct()

        if (externalPackages.isNotEmpty()) {
            val externalIntents = externalPackages.map { pkg ->
                Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage(pkg)
                }
            }

            val primaryIntent = externalIntents.first()
            if (externalIntents.size == 1) {
                return try {
                    context.startActivity(primaryIntent)
                    true
                } catch (_: Exception) {
                    false
                }
            } else {
                val chooserIntent = Intent.createChooser(primaryIntent, "Open with").apply {
                    putExtra(Intent.EXTRA_INITIAL_INTENTS, externalIntents.drop(1).toTypedArray())
                }
                return try {
                    context.startActivity(chooserIntent)
                    true
                } catch (_: Exception) {
                    false
                }
            }
        }

        // 3. Fallback to an external web browser
        val browserIntent = Intent(Intent.ACTION_VIEW, "https://www.google.com".toUri()).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        val browserInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(browserIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)
        }

        val externalBrowsers = browserInfos
            .map { it.activityInfo.packageName }
            .filter { it != context.packageName }
            .distinct()

        if (externalBrowsers.isNotEmpty()) {
            val browserPackageIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(externalBrowsers.first())
            }
            return try {
                context.startActivity(browserPackageIntent)
                true
            } catch (_: Exception) {
                false
            }
        }

        return false
    }
}
