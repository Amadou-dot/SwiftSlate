# CLAUDE.md

## Project Overview

SwiftSlate is an Android accessibility service app that provides AI-powered inline text transformation. Users type trigger commands (e.g., `?fix`, `?formal`) in any text field, and the app replaces the text with AI-processed output via the Gemini or OpenAI-compatible APIs. Built with Kotlin and Jetpack Compose, targeting a ~1.3 MB APK with zero external networking/JSON dependencies.

## Tech Stack

- **Language:** Kotlin 2.1, JDK 17 target
- **UI:** Jetpack Compose + Material 3 (AMOLED dark theme)
- **Navigation:** Navigation Compose
- **Async:** Kotlin Coroutines (kotlinx-coroutines-android)
- **HTTP:** `HttpURLConnection` (no external HTTP libraries)
- **JSON:** `org.json` (Android built-in)
- **Storage:** SharedPreferences with AES-256-GCM encryption (Android Keystore)
- **Build:** Gradle with Kotlin DSL
- **Testing:** JUnit 4 + Robolectric
- **Min SDK:** 23 (Android 6.0), Target/Compile SDK: 36

## Project Structure

```
app/src/main/java/com/musheer360/swiftslate/
├── MainActivity.kt                # Entry point, navigation host
├── SwiftSlateApp.kt               # Application class
├── api/
│   ├── GeminiClient.kt            # Google Gemini API integration
│   └── OpenAICompatibleClient.kt  # Custom OpenAI-compatible provider
├── manager/
│   ├── CommandManager.kt          # Built-in & custom command management
│   └── KeyManager.kt              # API key encryption & round-robin rotation
├── model/
│   └── Command.kt                 # Command data model
├── service/
│   └── AssistantService.kt        # Accessibility service (core logic)
└── ui/
    ├── DashboardScreen.kt         # Status dashboard
    ├── KeysScreen.kt              # API key management
    ├── CommandsScreen.kt          # Command management
    ├── SettingsScreen.kt          # Provider & settings configuration
    ├── components/
    │   └── CommonComponents.kt    # Reusable UI components (SlateCard, ScreenTitle)
    └── theme/
        └── Theme.kt               # Material 3 AMOLED dark theme
```

Tests mirror the source structure under `app/src/test/java/com/musheer360/swiftslate/`.

## Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing env vars)
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run Android lint
./gradlew lint
```

Release signing requires environment variables: `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

## Testing

- **Framework:** JUnit 4 with Robolectric (`@RunWith(RobolectricTestRunner::class)`)
- **Location:** `app/src/test/java/com/musheer360/swiftslate/manager/`
- **Coverage:** `CommandManagerTest` (16 tests), `KeyManagerTest` (15 tests)
- **Pattern:** Clear SharedPreferences in `@Before` for test isolation, use `ApplicationProvider.getApplicationContext<Application>()`
- **Run:** `./gradlew test`

## Architecture & Key Patterns

### Command System
- 9 built-in commands (`?fix`, `?improve`, `?shorten`, `?expand`, `?formal`, `?casual`, `?emoji`, `?reply`, `?undo`) plus dynamic `?translate:XX`
- Custom commands stored as JSON arrays in SharedPreferences
- Configurable trigger prefix (default: `?`)
- Longest-match-wins priority for command resolution

### API Clients
- `GeminiClient` and `OpenAICompatibleClient` share the same interface pattern (`generate` and `doGenerate` methods returning `Result<String>`)
- Multi-key round-robin rotation via `KeyManager`
- Rate-limit detection with configurable cooldown (1-600s)
- Invalid key tracking to skip broken keys

### Accessibility Service
- Monitors `TYPE_VIEW_TEXT_CHANGED` events system-wide
- Fast-exit optimization: checks last character before full parsing
- Inline spinner animation during processing
- Text replacement via `ACTION_SET_TEXT` with clipboard fallback
- 120-second watchdog timer for hung processes
- Skips password fields

### Security
- AES-256-GCM encryption for API keys stored in SharedPreferences
- Android Keystore for key material; falls back to plain text if unavailable
- No analytics, no intermediary servers, no external tracking

## Code Conventions

- **Naming:** PascalCase for files/classes, camelCase for functions/properties, SCREAMING_SNAKE_CASE for constants
- **Package:** `com.musheer360.swiftslate.*`
- **UI state:** `mutableStateOf()` for local Compose state, `rememberCoroutineScope()` for async
- **Error handling:** `Result<T>` return types for API operations, try-catch with fallbacks
- **Dependencies:** Minimize external dependencies — prefer Android built-in APIs
- **Strings:** User-facing strings should be in `res/values/strings.xml`
- **Theme:** All UI uses the Material 3 AMOLED dark color scheme defined in `Theme.kt`

## CI/CD

GitHub Actions workflow (`.github/workflows/build.yml`) on push to `master` or tags:
1. **version** — Determine version from git tags
2. **lint** — Android lint (retries up to 3x)
3. **test** — Unit tests with Robolectric (retries up to 3x)
4. **build** — Signed APK
5. **release** — GitHub release with APK artifact

Versioning: base version `1.0`, patch auto-incremented from git tags. Version code from `git rev-list --count`.

## Important Notes

- The core value proposition is the tiny APK size (~1.3 MB) — avoid adding heavy dependencies
- `AssistantService.kt` is the most complex file; changes there need careful testing since it runs system-wide
- No external HTTP libraries — all networking uses `HttpURLConnection`
- No external JSON libraries — uses Android's built-in `org.json`
- SharedPreferences is the only storage mechanism; there is no database
