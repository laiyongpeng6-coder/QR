# Design Document

## Overview

Fast QR Scan is an Android application targeting the overseas Google Play market. It provides QR/barcode scanning, code generation, and AI art beautification features through a modern Jetpack Compose UI. The app follows a **Clean Architecture** pattern with **MVVM** presentation layer, organized as a multi-module Android project.

### Technology Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| UI | Jetpack Compose + Material 3 | Declarative UI with theming |
| Navigation | Compose Navigation | Type-safe screen routing |
| DI | Hilt | Dependency injection |
| Async | Kotlin Coroutines + Flow | Reactive data streams |
| Database | Room + SQLCipher | Encrypted local persistence |
| Camera | CameraX + ML Kit Barcode | Camera control and scanning |
| QR Generation | ZXing | Barcode/QR code creation |
| Image Loading | Coil | Async image loading for Compose |
| Networking | Retrofit + OkHttp | Product lookup API calls |
| Serialization | Kotlinx Serialization | JSON parsing |
| Testing | JUnit 5 + Turbine + Mockk + Kotest Property | Unit, flow, and property testing |

### Design Decisions

- **Single Activity pattern** eliminates fragment lifecycle complexity
- **Multi-module structure** enables independent feature development and testing
- **SQLCipher encryption** protects user history data at rest
- **ML Kit + ZXing combination** provides both real-time scanning (ML Kit) and generation (ZXing)
- **Repository pattern** abstracts data sources for testability
- **Monetization excluded** from active implementation; architectural placeholders included for future integration

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    App Module (Shell)                     │
│  - Single Activity, Navigation Host, Hilt Entry Point    │
├─────────────────────────────────────────────────────────┤
│              Feature Modules (UI + ViewModel)             │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───────────┐  │
│  │ :scanner │ │:generator│ │ :history │ │:onboarding│  │
│  └──────────┘ └──────────┘ └──────────┘ └───────────┘  │
│  ┌──────────────┐ ┌──────────────────┐                   │
│  │:ai-workspace │ │ :product-lookup  │                   │
│  └──────────────┘ └──────────────────┘                   │
├─────────────────────────────────────────────────────────┤
│                    Core Module (:core)                    │
│  ┌────────┐ ┌──────────┐ ┌────────┐ ┌───────────────┐  │
│  │ :data  │ │ :domain  │ │ :ui    │ │ :common       │  │
│  └────────┘ └──────────┘ └────────┘ └───────────────┘  │
│  - Room DB, Repositories, Use Cases, Shared Composables  │
└─────────────────────────────────────────────────────────┘
```

### Navigation Graph

```
NavHost (MainNavHost)
├── onboarding_graph (conditional, first launch only)
│   └── OnboardingScreen
├── main_graph
│   ├── Tab: History
│   │   ├── HistoryListScreen
│   │   └── HistoryDetailScreen
│   ├── Tab: Scan
│   │   ├── ScannerScreen
│   │   ├── ScanResultScreen
│   │   └── ProductDetailScreen
│   └── Tab: Create
│       ├── GeneratorInputScreen
│       ├── GeneratorPreviewScreen
│       └── AiWorkspaceScreen
└── shared_routes
    └── SettingsScreen (future)
```

### Build Configuration

```kotlin
android {
    compileSdk = 35
    defaultConfig {
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
}

// Key dependencies (version catalog)
[versions]
kotlin = "2.0.0"
compose-bom = "2024.06.00"
hilt = "2.51.1"
room = "2.6.1"
camerax = "1.3.4"
mlkit-barcode = "17.3.0"
zxing = "3.5.3"
sqlcipher = "4.5.7"
retrofit = "2.11.0"
coil = "2.7.0"
```

## Components and Interfaces

### 1. App Module (`:app`)

**Responsibility:** Application entry point, Hilt application class, single Activity host, top-level navigation graph.

**Key Components:**
- `FastQrScanApplication` - Hilt application class
- `MainActivity` - Single activity hosting the Compose navigation
- `MainNavHost` - Top-level NavHost with tab-based navigation
- `FastQrScanTheme` - Material 3 theme definition

---

### 2. Scanner Module (`:feature:scanner`)

**Responsibility:** Camera preview, real-time barcode/QR detection, album import, flash control, auto-zoom.

```kotlin
class ScannerViewModel @Inject constructor(
    private val decodeUseCase: DecodeContentUseCase,
    private val resultMapper: ResultMapperUseCase,
    private val historyRepository: HistoryRepository
) : ViewModel()

@Composable
fun ScannerScreen(
    onResultDetected: (ScanResult) -> Unit,
    viewModel: ScannerViewModel = hiltViewModel()
)

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    fun bindCamera(previewView: PreviewView)
    fun enableTorch(enabled: Boolean)
    fun setZoomRatio(ratio: Float)
}
```

**Data Flow:**
1. CameraX provides preview frames → ML Kit BarcodeScanner analyzes frames
2. On detection → `DecodeContentUseCase` extracts raw value
3. `ResultMapperUseCase` classifies content type
4. Result displayed on `ScanResultScreen` with type-specific actions
5. Record saved to `HistoryRepository`

**Auto-Zoom Logic:**
- ML Kit returns bounding box of detected barcode
- If bounding box area < 15% of frame area, apply 2x zoom
- If bounding box area < 5% of frame area, apply 4x zoom
- Zoom resets when barcode is successfully decoded or leaves frame

**Flash Assist Logic:**
- CameraX `ImageAnalysis` provides luminosity data
- If average luminosity < threshold (configurable, default 40/255), enable torch
- Torch disabled when luminosity returns above threshold + hysteresis margin

---

### 3. Generator Module (`:feature:generator`)

**Responsibility:** QR code creation from multiple input types with validation and export.

```kotlin
sealed class GeneratorInputType {
    data class PlainText(val text: String) : GeneratorInputType()
    data class Url(val url: String) : GeneratorInputType()
    data class WiFi(val ssid: String, val password: String, val security: WifiSecurity) : GeneratorInputType()
    data class Contact(val vCard: VCardData) : GeneratorInputType()
    data class Phone(val number: String) : GeneratorInputType()
    data class SocialMedia(val platform: SocialPlatform, val profileUrl: String) : GeneratorInputType()
}

class GeneratorViewModel @Inject constructor(
    private val generateQrUseCase: GenerateQrUseCase,
    private val validateInputUseCase: ValidateInputUseCase,
    private val historyRepository: HistoryRepository
) : ViewModel()

class QrEncoder {
    fun encode(content: String, size: Int, errorCorrection: ErrorCorrectionLevel): Bitmap
}
```

**Validation Rules:**
- URL: Must match URI pattern with scheme (http/https)
- WiFi: SSID required, password required for WPA/WPA2
- Phone: Must match E.164 or common phone number patterns
- Contact: At minimum, name field required
- All: Content must not exceed QR version capacity (max ~4296 alphanumeric chars for version 40)

**Output Resolutions:** 256px, 512px, 1024px (user-selectable)

---

### 4. History Module (`:feature:history`)

**Responsibility:** Timeline display of scan/generation records with search, swipe actions, and encrypted persistence.

```kotlin
@Entity(tableName = "history_records")
data class HistoryRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contentType: ContentType,
    val rawContent: String,
    val displayTitle: String,
    val timestamp: Instant,
    val source: RecordSource, // SCAN or GENERATED
    val isFavorite: Boolean = false,
    val thumbnailPath: String? = null
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<HistoryRecord>>

    @Query("SELECT * FROM history_records WHERE rawContent LIKE :query OR displayTitle LIKE :query")
    fun searchRecords(query: String): Flow<List<HistoryRecord>>

    @Query("SELECT * FROM history_records WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getRecordsByDateRange(start: Instant, end: Instant): Flow<List<HistoryRecord>>
}

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onItemClick: (HistoryRecord) -> Unit
)
```

**Encryption Strategy:**
- Room database encrypted using SQLCipher
- Encryption key generated and stored in Android Keystore
- Key alias: `fast_qr_scan_db_key`
- AES-256-GCM encryption for database file

**Swipe Actions:**
- Swipe left → Delete with undo snackbar (5 second window)
- Swipe right → Toggle favorite/pin status

---

### 5. AI Workspace Module (`:feature:ai-workspace`)

**Responsibility:** QR code visual customization with real-time preview and scannability validation.

```kotlin
data class QrStyle(
    val foregroundColor: Color = Color.Black,
    val backgroundColor: Color = Color.White,
    val cornerRadius: Dp = 0.dp,
    val dotShape: DotShape = DotShape.Square,
    // TODO [FUTURE-MONETIZATION]: Add gradient, pattern, and AI template fields
)

enum class DotShape { Square, Circle, Rounded }

class AiWorkspaceViewModel @Inject constructor(
    private val qrEncoder: QrEncoder,
    private val scannabilityValidator: ScannabilityValidator
) : ViewModel()

class ScannabilityValidator {
    fun validate(styledBitmap: Bitmap): ValidationResult
}
```

**Free Tier Features:**
- Foreground color picker (full color wheel)
- Background color picker (full color wheel)
- Dot shape selection (Square, Circle, Rounded)
- Corner radius adjustment

**Placeholder Features (Coming Soon UI):**
- Gradient fills (locked, shows "Coming Soon" badge)
- AI art templates (locked, shows "Coming Soon" badge)
- Custom logo embedding (locked, shows "Coming Soon" badge)

**Scannability Validation:**
- After each style change, re-encode the styled QR as bitmap
- Run ML Kit barcode detection on the styled bitmap
- If decode fails, revert to last valid style and show warning

---

### 6. Onboarding Module (`:feature:onboarding`)

**Responsibility:** First-launch guided introduction with persistence of completion state.

```kotlin
data class OnboardingPage(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val illustrationRes: Int
)

class OnboardingPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val isOnboardingComplete: Flow<Boolean>
    suspend fun setOnboardingComplete()
}
```

**Pages:**
1. "Scan Anything" - Camera scanning illustration
2. "Create & Share" - QR generation illustration
3. "Your History, Secured" - Encrypted history illustration

**Navigation Logic:**
- App launch → check `isOnboardingComplete`
- If false → show OnboardingScreen
- On skip or complete → set flag, navigate to MainNavHost (Scan tab)

---

### 7. Product Lookup Module (`:feature:product-lookup`)

**Responsibility:** External product database queries for scanned barcodes with offline caching.

```kotlin
interface ProductApiService {
    @GET("products/{barcode}")
    suspend fun lookupProduct(@Path("barcode") barcode: String): ProductResponse
}

@Entity(tableName = "product_cache")
data class CachedProduct(
    @PrimaryKey val barcode: String,
    val name: String,
    val description: String?,
    val category: String?,
    val imageUrl: String?,
    val cachedAt: Instant
)

class ProductRepository @Inject constructor(
    private val apiService: ProductApiService,
    private val productDao: ProductDao
) {
    suspend fun lookupProduct(barcode: String): Result<ProductInfo>
}
```

**Cache Strategy:**
- Check local cache first (Room)
- If cache miss or stale (> 7 days), query API
- On network failure, return cached data if available
- On complete miss (no cache, no network), return raw barcode with error state

---

### 8. Core Module (`:core`)

**Responsibility:** Shared domain models, use cases, database configuration, common UI components, and utilities.

**Sub-modules:**

#### `:core:data`
- Room database definition (`FastQrScanDatabase`)
- SQLCipher integration and key management
- Base repository interfaces
- DataStore preferences

#### `:core:domain`
- `ContentType` enum (URL, WiFi, VCard, Phone, Email, SMS, SocialMedia, Geo, PlainText)
- `ScanResult` data class
- Use case interfaces
- `ResultMapperUseCase` - content classification logic

#### `:core:ui`
- Shared Composables (themed buttons, cards, loading states)
- Material 3 theme tokens and color schemes
- Common modifiers and extensions

#### `:core:common`
- String utilities, date formatters
- Permission helpers
- Share intent builders
- Locale-aware formatting

---

### Result Mapping Rules

The `ResultMapperUseCase` classifies decoded content using the following priority-ordered rules:

| Priority | Pattern | Classification |
|----------|---------|---------------|
| 1 | Starts with `WIFI:` | WiFi |
| 2 | Starts with `BEGIN:VCARD` | vCard |
| 3 | Starts with `tel:` or matches phone regex | Phone |
| 4 | Starts with `mailto:` | Email |
| 5 | Starts with `smsto:` or `sms:` | SMS |
| 6 | Starts with `geo:` | Geographic Location |
| 7 | Matches known social media URL patterns | Social Media |
| 8 | Matches URL pattern (http/https) | URL |
| 9 | All other content | Plain Text |

**Social Media Detection Patterns:**
- Instagram: `instagram.com/`, `instagr.am/`
- Twitter/X: `twitter.com/`, `x.com/`
- Facebook: `facebook.com/`, `fb.com/`
- LinkedIn: `linkedin.com/`
- TikTok: `tiktok.com/@`
- YouTube: `youtube.com/`, `youtu.be/`
- WhatsApp: `wa.me/`

## Data Models

### Core Entities

```kotlin
// Unified scan result
data class ScanResult(
    val rawValue: String,
    val format: BarcodeFormat,
    val contentType: ContentType,
    val timestamp: Instant,
    val metadata: Map<String, String> = emptyMap()
)

// Content type classification
enum class ContentType {
    URL, WIFI, VCARD, PHONE, EMAIL, SMS,
    SOCIAL_MEDIA, GEO, PLAIN_TEXT, PRODUCT
}

// Barcode formats supported
enum class BarcodeFormat {
    QR_CODE, EAN_13, EAN_8, UPC_A, UPC_E,
    CODE_128, CODE_39, ITF, PDF_417, DATA_MATRIX, AZTEC
}
```

### History Record Entity

```kotlin
@Entity(tableName = "history_records")
data class HistoryRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contentType: ContentType,
    val rawContent: String,
    val displayTitle: String,
    val timestamp: Instant,
    val source: RecordSource, // SCAN or GENERATED
    val isFavorite: Boolean = false,
    val thumbnailPath: String? = null
)

enum class RecordSource { SCAN, GENERATED }
```

### Product Cache Entity

```kotlin
@Entity(tableName = "product_cache")
data class CachedProduct(
    @PrimaryKey val barcode: String,
    val name: String,
    val description: String?,
    val category: String?,
    val imageUrl: String?,
    val cachedAt: Instant
)
```

### Generator Input Types

```kotlin
sealed class GeneratorInputType {
    data class PlainText(val text: String) : GeneratorInputType()
    data class Url(val url: String) : GeneratorInputType()
    data class WiFi(val ssid: String, val password: String, val security: WifiSecurity) : GeneratorInputType()
    data class Contact(val vCard: VCardData) : GeneratorInputType()
    data class Phone(val number: String) : GeneratorInputType()
    data class SocialMedia(val platform: SocialPlatform, val profileUrl: String) : GeneratorInputType()
}

enum class WifiSecurity { OPEN, WEP, WPA, WPA2 }
enum class SocialPlatform { INSTAGRAM, TWITTER, FACEBOOK, LINKEDIN, TIKTOK, YOUTUBE, WHATSAPP }
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: QR Encode/Decode Round-Trip

*For any* valid input content string within QR capacity limits, encoding it into a QR code and then decoding the resulting image should produce the original content string.

**Validates: Requirements 4.3, 6.1**

### Property 2: Content Classification Determinism

*For any* decoded content string, the ResultMapperUseCase should always classify it into exactly one ContentType, and repeated classification of the same string should always produce the same result.

**Validates: Requirements 5.1**

### Property 3: Social Media Platform Detection

*For any* URL matching a known social media domain pattern, the ResultMapperUseCase should classify it as SOCIAL_MEDIA and correctly identify the platform.

**Validates: Requirements 5.6**

### Property 4: Input Validation Rejects Invalid Data

*For any* invalid input (malformed URL without scheme, empty required WiFi SSID, phone number with non-digit characters, content exceeding QR capacity), the ValidateInputUseCase should return a validation error with a non-empty error message.

**Validates: Requirements 6.3, 6.6**

### Property 5: Output Resolution Matches Selection

*For any* resolution selected from {256, 512, 1024} and any valid input content, the generated QR code bitmap should have width and height exactly equal to the selected resolution.

**Validates: Requirements 6.5**

### Property 6: Styled QR Scannability Round-Trip

*For any* valid QR content and any style configuration (foreground color, background color, dot shape, corner radius), the styled QR code should be decodable back to the original content string.

**Validates: Requirements 7.4**

### Property 7: History Ordering and Date Grouping

*For any* set of history records with distinct timestamps, the displayed list should be in strictly reverse-chronological order, and records sharing the same calendar date should be grouped together under a common date header.

**Validates: Requirements 8.1, 8.2**

### Property 8: History Search Correctness

*For any* text search query, all returned history records should contain the query string in either their rawContent or displayTitle fields, and no matching records should be excluded from results.

**Validates: Requirements 8.7**

### Property 9: Product Cache Round-Trip

*For any* product successfully looked up from the API, a subsequent lookup of the same barcode without network should return the same product name, description, and category from the local cache.

**Validates: Requirements 9.5**

### Property 10: Auto-Zoom Threshold Behavior

*For any* detected barcode bounding box, if the box area is less than 5% of frame area the zoom should be 4x, if less than 15% the zoom should be 2x, and otherwise the zoom should remain at 1x.

**Validates: Requirements 4.5**

### Property 11: Auto-Flash Threshold Behavior

*For any* luminosity reading below the configured threshold, the flash should be enabled; for any luminosity reading above the threshold plus hysteresis margin, the flash should be disabled.

**Validates: Requirements 4.4**

## Error Handling

### Camera Errors
- **Permission denied:** Display rationale screen with button to open system settings
- **Camera hardware failure:** Show error message with retry option; fall back to album import
- **ML Kit initialization failure:** Log error, disable real-time scanning, offer album import as alternative

### Network Errors
- **Product lookup timeout:** Show raw barcode with "Lookup timed out" message; offer retry
- **No network connectivity:** Display cached data if available; show "Requires internet" for uncached lookups
- **API error responses (4xx/5xx):** Show raw barcode with generic "Lookup unavailable" message

### QR Generation Errors
- **Data exceeds capacity:** Display inline error "Data too large for QR code" before generation attempt
- **Invalid input:** Show field-specific validation errors inline (e.g., "Invalid URL format")
- **Bitmap encoding failure:** Show generic error with retry option

### Database Errors
- **SQLCipher key access failure:** Log critical error; show "Unable to access history" with option to reset
- **Room migration failure:** Attempt destructive migration as last resort; log data loss event
- **Disk full:** Show "Storage full" message; suggest clearing old history records

### Styling Errors
- **Scannability validation failure:** Revert to last valid style; show warning "This style makes the QR code unreadable"
- **Color contrast too low:** Warn user that low contrast may affect scannability

### General Error Strategy
- All errors are caught at the ViewModel layer and exposed as UI state
- Kotlin `Result` type used for repository operations
- No crashes propagated to UI; all exceptions handled gracefully
- Error states include actionable recovery options where possible
- Critical errors logged for debugging (no PII in logs)

## Testing Strategy

### Testing Layers

| Layer | Tool | Scope |
|-------|------|-------|
| Unit | JUnit 5 + Mockk | ViewModels, Use Cases, Repositories |
| UI | Compose Testing | Screen composables, navigation |
| Integration | Hilt Testing | Module boundaries, DI graph |
| Property | Kotest Property | Result mapping, validation logic, QR encode/decode round-trip |
| Performance | Benchmark | Cold start, frame rate, load times |

### Property-Based Testing Configuration

- **Library:** Kotest Property Testing (io.kotest:kotest-property)
- **Minimum iterations:** 100 per property test
- **Tag format:** `Feature: qr-scan-max-app, Property {number}: {property_text}`

**Property tests to implement:**
1. QR encode/decode round-trip (Property 1)
2. Content classification determinism (Property 2)
3. Social media platform detection (Property 3)
4. Input validation rejection (Property 4)
5. Output resolution correctness (Property 5)
6. Styled QR scannability (Property 6)
7. History ordering and grouping (Property 7)
8. History search correctness (Property 8)
9. Product cache round-trip (Property 9)
10. Auto-zoom threshold behavior (Property 10)
11. Auto-flash threshold behavior (Property 11)

### Unit Testing Focus

Unit tests complement property tests by covering:
- Specific examples demonstrating correct behavior for each content type
- Integration points between ViewModel and Use Cases
- Edge cases: empty strings, maximum-length inputs, special characters
- Error state transitions in ViewModels

### UI Testing Focus

- Navigation flow: onboarding → main tabs → detail screens
- Tab switching preserves state
- Swipe gesture actions (delete, favorite)
- Permission rationale screen display
- Material 3 component rendering

### Security Considerations

- Database encryption via SQLCipher with Android Keystore-managed keys
- No sensitive data in logs (ProGuard/R8 strips log calls in release)
- Camera permission requested at runtime with rationale
- Network calls over HTTPS only
- Input validation on all user-provided data before processing
- No analytics or tracking SDKs in initial release

### Monetization Placeholders

The following integration points are defined but NOT implemented:

```kotlin
// In AiWorkspaceViewModel:
// TODO [FUTURE-MONETIZATION]: Check subscription status before unlocking gradient styles
// TODO [FUTURE-MONETIZATION]: Show rewarded ad option for temporary gradient access

// In ScanResultScreen:
// TODO [FUTURE-MONETIZATION]: Insert interstitial ad after every Nth scan (configurable)

// In GeneratorPreviewScreen:
// TODO [FUTURE-MONETIZATION]: Gate high-resolution (1024px) export behind Pro subscription

// In ProductDetailScreen:
// TODO [FUTURE-MONETIZATION]: Gate price comparison feature behind Pro subscription

// In app module:
// TODO [FUTURE-MONETIZATION]: Initialize ad SDK (AdMob/AppLovin) in Application.onCreate()
// TODO [FUTURE-MONETIZATION]: Initialize subscription SDK (Google Play Billing) in Application.onCreate()
```

