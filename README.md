# Map Links Converter

**Map Links Converter** is an Android application built with Jetpack Compose and Kotlin that seamlessly intercepts, converts, and redirects map links between **Apple Maps**, **Google Maps**, and **Waze**.

When someone shares an Apple Maps or Waze link with an Android user (or vice versa), Map Links Converter automatically translates the coordinates, place names, addresses, or navigation parameters into your preferred navigation app.

---

## ✨ Features

- **🌐 Deep Link Interception**: Catch map URLs tapped in web browsers, messenger apps, or shared via the Android system Share sheet (`ACTION_VIEW` and `ACTION_SEND`).
- **🔀 Multi-Provider Conversion**: Converts links bidirectionally between:
  - **Apple Maps** (`maps.apple.com`, `apple.co`)
  - **Google Maps** (`maps.google.com`, `maps.app.goo.gl`)
  - **Waze** (`waze.com`, `ul.waze.com`, `waze://`)
- **🚀 Fast Auto-Redirect**: Instantly open converted links in your default app without requiring manual interaction when auto-redirect is enabled.
- **⚡ Short URL Expansion**: Asynchronously resolves shortened share links (`maps.app.goo.gl`, `apple.co`, `waze.com`) and extracts hidden coordinates and location metadata via HTTP header redirection and canonical meta tag parsing.
- **⚙️ Custom Target App Preferences**: Configure distinct target navigation apps for each link source (e.g. open Apple Maps links in Google Maps, Google Maps links in Waze).
- **🎨 Modern Material 3 UI**: Built with Jetpack Compose featuring dynamic light/dark theme support, edge-to-edge layout, manual link conversion, copy options, and quick launcher buttons.
- **📍 Rich Location Parsing**: Extracts coordinates (`ll`, `latlng`), search queries, place names, addresses, and route endpoints (origin/destination).

---

## 🏗️ Tech Stack & Architecture

- **Language**: Kotlin
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 Design
- **Architecture**: MVVM (Model-View-ViewModel) with `StateFlow` and `Kotlin Coroutines`
- **Asynchronous Execution**: Coroutines + `HttpURLConnection` for lightweight async URL expansion
- **Dependency Management**: Gradle Version Catalog (`gradle/libs.versions.toml`)
- **Minimum SDK**: Android 7.0 (API 24)
- **Target/Compile SDK**: Android 15 / SDK 37

---

## 🛠️ Project Structure

```
maplinksconverter/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/markvoronin/maplinksconverter/
│   │   │   │   ├── MainActivity.kt               # Deep-link interception & fast redirect launch
│   │   │   │   ├── data/
│   │   │   │   │   ├── AppleMapsConverter.kt      # Core conversion logic & regex parsers
│   │   │   │   │   ├── ConversionResult.kt        # Data classes & enums for map entities
│   │   │   │   │   └── UrlExpander.kt             # Short URL expander & metadata parser
│   │   │   │   └── ui/
│   │   │   │       ├── HomeScreen.kt              # Main Jetpack Compose UI
│   │   │   │       ├── MainViewModel.kt           # State management & settings persistence
│   │   │   │       ├── PremiumHomeScreen.kt       # Rich interface components
│   │   │   │       └── theme/                     # Color schemes, typography, and theme setup
│   │   │   └── AndroidManifest.xml                # Intent filters for map scheme handling
│   │   └── test/                                  # Unit tests for link parsing and conversion
```

---

## 🚀 How It Works

1. **Intent Interception**: `AndroidManifest.xml` declares intent filters for domain hosts (`maps.apple.com`, `maps.google.com`, `waze.com`, etc.) and the `geo:` scheme.
2. **Fast Path Execution**: In `MainActivity.kt`, incoming intent links are immediately inspected:
   - If direct coordinates/queries are present and Auto-Redirect is enabled, the target application is launched synchronously for zero-delay navigation.
3. **Async URL Expansion**: Shortened links (like `maps.app.goo.gl` or `apple.co`) trigger `UrlExpander`, following redirects and reading canonical metadata to retrieve coordinates.
4. **Link Conversion**: `AppleMapsConverter` parses parameters (`ll`, `q`, `saddr`, `daddr`, place paths) and formats output URLs for Google Maps (`https://www.google.com/maps/...`), Waze (`https://waze.com/ul?...`), Apple Maps, or generic `geo:` URIs.

---

## 💻 Getting Started

### Prerequisites

- **Android Studio**: Ladybug (2024.2.1) or newer recommended
- **JDK**: Java 11 or higher
- **Android SDK**: API 37 (installed via Android Studio SDK Manager)

### Building the App

Clone the repository and build using Gradle:

```bash
# Clone the repository
git clone https://github.com/markvoronin/maplinksconverter.git
cd maplinksconverter

# Build debug APK
./gradlew assembleDebug
```

The compiled APK will be available under `app/build/outputs/apk/debug/app-debug.apk`.

### Running Unit Tests

Execute the automated test suite for conversion logic and link expansion:

```bash
./gradlew test
```

---

## 📄 License

This project is open source and available under the standard MIT License (or project license terms).
