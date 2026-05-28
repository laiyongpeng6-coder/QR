# Implementation Plan

## Overview

This implementation plan covers the QR Scan Max Android application — a multi-module Jetpack Compose app providing QR/barcode scanning, code generation, AI art beautification, and history management. The plan is organized into 15 task groups covering project scaffolding, core domain and data layers, UI components, feature modules (scanner, generator, history, AI workspace, onboarding, product lookup), localization, documentation, and performance optimization.

## Tasks

- [x] 1. Project Scaffolding and Build Configuration
  - [x] 1.1 Create root `build.gradle.kts` with Android Gradle Plugin, Kotlin, Hilt, and KSP plugin declarations
    - _Requirements: 1.1, 1.3, 1.5_
  - [x] 1.2 Create `settings.gradle.kts` with all module includes (app, core/data, core/domain, core/ui, core/common, feature/scanner, feature/generator, feature/history, feature/ai-workspace, feature/onboarding, feature/product-lookup)
    - _Requirements: 1.2_
  - [x] 1.3 Create `gradle/libs.versions.toml` version catalog with all dependency versions (Compose BOM, Hilt, Room, CameraX, ML Kit, ZXing, SQLCipher, Retrofit, Coil, Kotlinx Serialization, JUnit 5, Mockk, Kotest)
    - _Requirements: 1.5, 1.6_
  - [x] 1.4 Create `gradle.properties` with Kotlin and Android build optimization flags
    - _Requirements: 1.5_
  - [x] 1.5 Create module-level `build.gradle.kts` for `:app` module with Compose, Hilt, and all feature module dependencies
    - _Requirements: 1.1, 1.2, 1.3_
  - [x] 1.6 Create module-level `build.gradle.kts` for each core sub-module (`:core:data`, `:core:domain`, `:core:ui`, `:core:common`)
    - _Requirements: 1.2, 1.4_
  - [x] 1.7 Create module-level `build.gradle.kts` for each feature module (`:feature:scanner`, `:feature:generator`, `:feature:history`, `:feature:ai-workspace`, `:feature:onboarding`, `:feature:product-lookup`)
    - _Requirements: 1.2_
  - [x] 1.8 Create `AndroidManifest.xml` for app module with camera, internet, and storage permissions declared
    - _Requirements: 1.5, 4.1_
  - [x] 1.9 Verify project syncs and compiles successfully with empty module structure
    - _Requirements: 1.1_

- [x] 2. Core Domain Models and Use Cases
  - [x] 2.1 Create `ContentType` enum in `:core:domain` with values: URL, WIFI, VCARD, PHONE, EMAIL, SMS, SOCIAL_MEDIA, GEO, PLAIN_TEXT, PRODUCT
    - _Requirements: 5.1_
  - [x] 2.2 Create `BarcodeFormat` enum in `:core:domain` with values: QR_CODE, EAN_13, EAN_8, UPC_A, UPC_E, CODE_128, CODE_39, ITF, PDF_417, DATA_MATRIX, AZTEC
    - _Requirements: 4.3_
  - [x] 2.3 Create `ScanResult` data class in `:core:domain` with fields: rawValue, format, contentType, timestamp, metadata map
    - _Requirements: 4.2, 5.1_
  - [x] 2.4 Create `ResultMapperUseCase` in `:core:domain` implementing priority-ordered content classification rules (WiFi → vCard → Phone → Email → SMS → Geo → Social Media → URL → Plain Text)
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7_
  - [x] 2.5 Create `HistoryRecord` data class in `:core:domain` with fields: id, contentType, rawContent, displayTitle, timestamp, source (SCAN/GENERATED), isFavorite, thumbnailPath
    - _Requirements: 8.1_
  - [x] 2.6 Create repository interfaces (`HistoryRepository`, `ProductRepository`) in `:core:domain`
    - _Requirements: 1.4, 8.6, 9.1_
  - [x]* 2.7 Write unit tests for `ResultMapperUseCase` covering all content type classifications and edge cases
    - _Requirements: 5.1, 5.7_
  - [x]* 2.8 Write property test for content classification determinism
    - **Property 2: Content Classification Determinism**
    - **Validates: Requirements 5.1**
  - [x]* 2.9 Write property test for social media platform detection
    - **Property 3: Social Media Platform Detection**
    - **Validates: Requirements 5.6**

- [x] 3. Core Data Layer (Room + Encryption)
  - [x] 3.1 Create `QrScanMaxDatabase` Room database class in `:core:data` with entities for HistoryRecord and CachedProduct
    - _Requirements: 1.4, 8.6_
  - [x] 3.2 Create `EncryptionKeyManager` class using Android Keystore to generate and retrieve AES-256 database encryption key
    - _Requirements: 8.6_
  - [x] 3.3 Create `HistoryDao` interface with Flow-based queries: getAllRecords, searchRecords, getRecordsByDateRange, insert, delete, updateFavorite
    - _Requirements: 8.1, 8.7_
  - [x] 3.4 Create `ProductDao` interface with queries: getByBarcode, insert, deleteStale
    - _Requirements: 9.5_
  - [x] 3.5 Create `HistoryRepositoryImpl` implementing `HistoryRepository` interface with Room DAO delegation
    - _Requirements: 1.4, 8.6_
  - [x] 3.6 Create `OnboardingPreferences` class using DataStore to persist onboarding completion state
    - _Requirements: 3.4_
  - [x] 3.7 Create Hilt module (`DatabaseModule`) providing database, DAOs, and DataStore instances
    - _Requirements: 1.3_
  - [x]* 3.8 Write unit tests for HistoryRepositoryImpl with in-memory Room database
    - _Requirements: 8.6, 8.7_

- [x] 4. Core UI Components and Theme
  - [x] 4.1 Create Material 3 theme definition (`Theme.kt`, `Color.kt`, `Type.kt`) in `:core:ui` with light and dark color schemes
    - _Requirements: 2.5_
  - [x] 4.2 Create shared composable components: `QrMaxButton`, `QrMaxCard`, `QrMaxLoadingIndicator`, `QrMaxErrorState`
    - _Requirements: 2.5_
  - [x] 4.3 Create common utility extensions in `:core:common`: date formatters (locale-aware), share intent builder, permission helpers
    - _Requirements: 11.4_
  - [x] 4.4 Create `strings.xml` resource file with all externalized English strings for shared components
    - _Requirements: 11.1, 11.2_

- [x] 5. App Shell and Navigation
  - [x] 5.1 Create `QRScanMaxApplication` class with `@HiltAndroidApp` annotation
    - _Requirements: 1.1, 1.3_
  - [x] 5.2 Create `MainActivity` with `@AndroidEntryPoint` annotation hosting the root Compose content
    - _Requirements: 1.1_
  - [x] 5.3 Create `NavRoutes` sealed class/object defining all navigation destinations with type-safe arguments
    - _Requirements: 1.1, 2.1_
  - [x] 5.4 Create `TabNavigation` composable implementing the 3-tab bottom bar (History, Scan, Create) with Material 3 styling
    - _Requirements: 2.1, 2.5_
  - [x] 5.5 Create `MainNavHost` composable with conditional onboarding check and tab-based navigation with state preservation
    - _Requirements: 2.2, 2.3, 2.4, 3.4_
  - [x] 5.6 Verify app launches and displays the tab navigation shell with placeholder screens
    - _Requirements: 2.2_

- [x] 6. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 7. Onboarding Feature
  - [~] 7.1 Create `OnboardingPage` data class and define 3 onboarding pages with string resources and placeholder illustrations
    - _Requirements: 3.1_
  - [~] 7.2 Create `OnboardingViewModel` that reads/writes onboarding completion state via `OnboardingPreferences`
    - _Requirements: 3.4_
  - [~] 7.3 Create `OnboardingScreen` composable with horizontal pager, page indicators, skip button, and completion button
    - _Requirements: 3.1, 3.2, 3.3_
  - [~] 7.4 Integrate onboarding navigation: show on first launch, skip to Scan tab on completion
    - _Requirements: 3.4_
  - [~] 7.5 Create `strings.xml` for onboarding module with localization-ready title and description strings
    - _Requirements: 3.5, 11.1_
  - [x]* 7.6 Write UI test verifying onboarding flow: swipe through pages, tap complete, verify navigation to Scan tab
    - _Requirements: 3.1, 3.3, 3.4_

- [ ] 8. Scanner Feature - Camera and Detection
  - [~] 8.1 Create `CameraManager` class wrapping CameraX setup: preview binding, lifecycle management, torch control, zoom control
    - _Requirements: 4.1, 4.4, 4.5_
  - [~] 8.2 Create `AutoZoomController` implementing bounding-box-based zoom logic (< 15% → 2x, < 5% → 4x)
    - _Requirements: 4.5_
  - [~] 8.3 Create `FlashAssistController` implementing luminosity-based torch activation with hysteresis
    - _Requirements: 4.4_
  - [~] 8.4 Create `ScannerViewModel` managing scanner state (scanning, result detected, permission denied) with ML Kit barcode analyzer
    - _Requirements: 4.1, 4.2, 4.3_
  - [~] 8.5 Create `ScannerScreen` composable with full-screen camera preview, viewfinder overlay, flash toggle, and album import button
    - _Requirements: 4.1, 4.7_
  - [~] 8.6 Implement album import: gallery picker intent, image decode using ML Kit on selected bitmap
    - _Requirements: 4.7_
  - [~] 8.7 Create camera permission handling: runtime request, rationale dialog, settings redirect
    - _Requirements: 4.6_
  - [~] 8.8 Create `strings.xml` for scanner module with all user-facing strings
    - _Requirements: 11.1_
  - [x]* 8.9 Write unit tests for `AutoZoomController` and `FlashAssistController` logic
    - _Requirements: 4.4, 4.5_
  - [x]* 8.10 Write property test for auto-zoom threshold behavior
    - **Property 10: Auto-Zoom Threshold Behavior**
    - **Validates: Requirements 4.5**
  - [x]* 8.11 Write property test for auto-flash threshold behavior
    - **Property 11: Auto-Flash Threshold Behavior**
    - **Validates: Requirements 4.4**

- [ ] 9. Scan Result Screen and Rule Mapping
  - [~] 9.1 Create `ScanResultScreen` composable displaying decoded content with type-specific icon and formatted fields
    - _Requirements: 5.1_
  - [~] 9.2 Implement URL result actions: open in browser, copy URL, share
    - _Requirements: 5.2_
  - [~] 9.3 Implement WiFi result actions: display SSID/security, connect action (via WiFi suggestion API)
    - _Requirements: 5.3_
  - [~] 9.4 Implement vCard result actions: display contact fields, save to contacts intent
    - _Requirements: 5.4_
  - [~] 9.5 Implement Phone result actions: display number, call intent, SMS intent
    - _Requirements: 5.5_
  - [~] 9.6 Implement Social Media result actions: detect platform, deep-link to app or fallback to browser
    - _Requirements: 5.6_
  - [~] 9.7 Implement Plain Text fallback: copy and share actions
    - _Requirements: 5.7_
  - [~] 9.8 Auto-save scan result to history on successful decode
    - _Requirements: 8.1_
  - [x]* 9.9 Write unit tests for social media URL pattern detection
    - _Requirements: 5.6_

- [ ] 10. QR Code Generator Feature
  - [~] 10.1 Create `QrEncoder` class wrapping ZXing library for QR code bitmap generation with configurable size and error correction
    - _Requirements: 6.1, 6.2, 6.5_
  - [~] 10.2 Create `ValidateInputUseCase` with validation rules for each input type (URL, WiFi, Phone, Contact, Social Media, Plain Text)
    - _Requirements: 6.3, 6.6_
  - [~] 10.3 Create `GeneratorViewModel` managing input state, validation errors, and generated bitmap
    - _Requirements: 6.2, 6.3_
  - [~] 10.4 Create `GeneratorInputScreen` composable with input type selector tabs and type-specific form fields
    - _Requirements: 6.1, 6.3_
  - [~] 10.5 Create `GeneratorPreviewScreen` composable displaying generated QR code with resolution selector, save, share, and beautify actions
    - _Requirements: 6.4, 6.5_
  - [~] 10.6 Implement save-to-gallery functionality using MediaStore API
    - _Requirements: 6.4_
  - [~] 10.7 Implement share via system share sheet with bitmap URI
    - _Requirements: 6.4_
  - [~] 10.8 Auto-save generated code to history
    - _Requirements: 8.1_
  - [~] 10.9 Create `strings.xml` for generator module
    - _Requirements: 11.1_
  - [x]* 10.10 Write unit tests for `ValidateInputUseCase` covering valid and invalid inputs for each type
    - _Requirements: 6.3, 6.6_
  - [x]* 10.11 Write property test for QR encode/decode round-trip
    - **Property 1: QR Encode/Decode Round-Trip**
    - **Validates: Requirements 4.3, 6.1**
  - [x]* 10.12 Write property test for input validation rejects invalid data
    - **Property 4: Input Validation Rejects Invalid Data**
    - **Validates: Requirements 6.3, 6.6**
  - [x]* 10.13 Write property test for output resolution matches selection
    - **Property 5: Output Resolution Matches Selection**
    - **Validates: Requirements 6.5**

- [ ] 11. AI Art Beautification Workspace
  - [~] 11.1 Create `QrStyle` data class with foreground color, background color, dot shape, and corner radius fields
    - _Requirements: 7.2_
  - [~] 11.2 Create `ScannabilityValidator` class that decodes a styled bitmap using ML Kit and returns pass/fail
    - _Requirements: 7.4_
  - [~] 11.3 Create `AiWorkspaceViewModel` managing style state, preview bitmap, and validation results
    - _Requirements: 7.3_
  - [~] 11.4 Create `ColorPicker` composable (color wheel or palette grid) for foreground and background selection
    - _Requirements: 7.2_
  - [~] 11.5 Create `DotShapeSelector` composable with Square, Circle, and Rounded options
    - _Requirements: 7.2_
  - [~] 11.6 Create `AiWorkspaceScreen` composable with real-time styled QR preview, style controls, and save/share actions
    - _Requirements: 7.1, 7.3, 7.5_
  - [~] 11.7 Implement "Coming Soon" locked UI sections for gradient styles and AI templates with placeholder badges
    - _Requirements: 7.6_
  - [~] 11.8 Add `// TODO [FUTURE-MONETIZATION]:` comments at subscription check points
    - _Requirements: 10.4_
  - [x]* 11.9 Write unit test for `ScannabilityValidator` with known-good and known-bad styled bitmaps
    - _Requirements: 7.4_
  - [x]* 11.10 Write property test for styled QR scannability round-trip
    - **Property 6: Styled QR Scannability Round-Trip**
    - **Validates: Requirements 7.4**

- [x] 12. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 13. History Feature
  - [~] 13.1 Create `HistoryViewModel` managing paginated history list, search query, and date filtering
    - _Requirements: 8.1, 8.7_
  - [~] 13.2 Create `HistoryScreen` composable with reverse-chronological waterfall layout grouped by date headers
    - _Requirements: 8.1, 8.2_
  - [~] 13.3 Implement swipe-to-delete (left) with undo snackbar (5 second window)
    - _Requirements: 8.3_
  - [~] 13.4 Implement swipe-to-favorite (right) toggling pin status
    - _Requirements: 8.4_
  - [~] 13.5 Create `HistoryDetailScreen` composable showing full result with all original actions
    - _Requirements: 8.5_
  - [~] 13.6 Implement search bar with content text, type filter, and date range picker
    - _Requirements: 8.7_
  - [~] 13.7 Create `strings.xml` for history module
    - _Requirements: 11.1_
  - [x]* 13.8 Write unit test for history search and date range filtering logic
    - _Requirements: 8.7_
  - [x]* 13.9 Write property test for history ordering and date grouping
    - **Property 7: History Ordering and Date Grouping**
    - **Validates: Requirements 8.1, 8.2**
  - [x]* 13.10 Write property test for history search correctness
    - **Property 8: History Search Correctness**
    - **Validates: Requirements 8.7**

- [ ] 14. Product Lookup Feature
  - [~] 14.1 Create `ProductApiService` Retrofit interface with product lookup endpoint
    - _Requirements: 9.1_
  - [~] 14.2 Create `ProductRepositoryImpl` with cache-first strategy: check Room cache → API call → fallback to cached/error
    - _Requirements: 9.1, 9.5_
  - [~] 14.3 Create `ProductLookupViewModel` managing lookup state (loading, found, not found, offline)
    - _Requirements: 9.2, 9.3, 9.4_
  - [~] 14.4 Create `ProductDetailScreen` composable displaying product name, description, category, and image
    - _Requirements: 9.2_
  - [~] 14.5 Create Hilt module (`NetworkModule`) providing Retrofit, OkHttp client, and API service instances
    - _Requirements: 1.3_
  - [~] 14.6 Implement 7-day cache staleness check and automatic refresh
    - _Requirements: 9.5_
  - [~] 14.7 Handle offline state: display cached data or "requires internet" message
    - _Requirements: 9.4_
  - [~] 14.8 Add `// TODO [FUTURE-MONETIZATION]:` comment for price comparison Pro feature gate
    - _Requirements: 10.4_
  - [x]* 14.9 Write unit test for `ProductRepositoryImpl` cache-first logic with mocked API and DAO
    - _Requirements: 9.1, 9.5_
  - [x]* 14.10 Write property test for product cache round-trip
    - **Property 9: Product Cache Round-Trip**
    - **Validates: Requirements 9.5**

- [ ] 15. Localization and RTL Support
  - [~] 15.1 Audit all modules and ensure every user-facing string is in `strings.xml` (no hardcoded strings)
    - _Requirements: 11.1_
  - [~] 15.2 Configure `resConfigs` in build to include supported locales
    - _Requirements: 11.2_
  - [~] 15.3 Add `android:supportsRtl="true"` to manifest and verify Compose layouts use `start`/`end` instead of `left`/`right`
    - _Requirements: 11.3_
  - [~] 15.4 Implement locale-aware date and number formatting using `java.time` and `NumberFormat`
    - _Requirements: 11.4_
  - [~] 15.5 Create a localization testing utility that validates all string resources have no missing translations for configured locales
    - _Requirements: 11.1, 11.2_

- [ ] 16. Documentation and AI Continuity
  - [~] 16.1 Create `docs/ARCHITECTURE.md` documenting module boundaries, dependency graph, data flow diagrams, and key design decisions
    - _Requirements: 10.1_
  - [~] 16.2 Create `docs/CONTRIBUTING.md` with build instructions, module creation guide, coding conventions, and PR checklist
    - _Requirements: 10.3_
  - [~] 16.3 Create `docs/ROADMAP.md` listing deferred features with integration guidance: subscriptions, ads, AI templates, cloud sync, batch scanning, price comparison
    - _Requirements: 10.5_
  - [~] 16.4 Add KDoc comments to all public classes, interfaces, and functions across all modules
    - _Requirements: 10.2_
  - [~] 16.5 Verify all `// TODO [FUTURE-MONETIZATION]:` comments are present at designated integration points
    - _Requirements: 10.4_
  - [~] 16.6 Create `docs/API-CONTRACTS.md` documenting the Product Lookup API expected request/response format
    - _Requirements: 9.1_

- [ ] 17. Performance Optimization and Final Integration
  - [~] 17.1 Configure R8/ProGuard rules for release builds (strip logs, optimize, keep Hilt/Room annotations)
    - _Requirements: 12.1_
  - [~] 17.2 Implement lazy loading for History tab (load first 20 items, paginate on scroll)
    - _Requirements: 12.4_
  - [~] 17.3 Profile and optimize cold start: defer non-critical initialization, use `App Startup` library for ordered initialization
    - _Requirements: 12.1_
  - [~] 17.4 Ensure camera preview starts within 2 seconds of Scan tab visibility on mid-range devices
    - _Requirements: 12.1_
  - [~] 17.5 Verify tab switching animations maintain 60fps (no more than 3 dropped frames)
    - _Requirements: 12.3_
  - [~] 17.6 Run full integration test: onboarding → scan → view result → generate → beautify → check history
    - _Requirements: 12.1, 12.2_
  - [~] 17.7 Create signed release build configuration (keystore placeholder) and verify APK builds successfully
    - _Requirements: 1.5_

- [x] 18. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- The implementation uses Kotlin with Jetpack Compose as specified in the design
- Monetization features are excluded from active implementation; only placeholder comments are added

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3", "1.4"] },
    { "id": 1, "tasks": ["1.5", "1.6", "1.7", "1.8"] },
    { "id": 2, "tasks": ["1.9"] },
    { "id": 3, "tasks": ["2.1", "2.2", "2.3", "2.5", "2.6", "4.1", "4.4"] },
    { "id": 4, "tasks": ["2.4", "3.1", "3.2", "3.6", "4.2", "4.3"] },
    { "id": 5, "tasks": ["2.7", "2.8", "2.9", "3.3", "3.4", "3.7"] },
    { "id": 6, "tasks": ["3.5", "5.1", "5.2", "5.3"] },
    { "id": 7, "tasks": ["3.8", "5.4", "5.5"] },
    { "id": 8, "tasks": ["5.6"] },
    { "id": 9, "tasks": ["7.1", "7.2", "8.1", "8.2", "8.3", "10.1", "10.2"] },
    { "id": 10, "tasks": ["7.3", "7.4", "7.5", "8.4", "8.5", "8.6", "8.7", "8.8", "10.3", "10.4"] },
    { "id": 11, "tasks": ["7.6", "8.9", "8.10", "8.11", "9.1", "10.5", "10.6", "10.7", "10.8", "10.9"] },
    { "id": 12, "tasks": ["9.2", "9.3", "9.4", "9.5", "9.6", "9.7", "9.8", "10.10", "10.11", "10.12", "10.13"] },
    { "id": 13, "tasks": ["9.9", "11.1", "11.2", "11.3"] },
    { "id": 14, "tasks": ["11.4", "11.5", "11.6", "11.7", "11.8"] },
    { "id": 15, "tasks": ["11.9", "11.10", "13.1", "13.2"] },
    { "id": 16, "tasks": ["13.3", "13.4", "13.5", "13.6", "13.7"] },
    { "id": 17, "tasks": ["13.8", "13.9", "13.10", "14.1", "14.5"] },
    { "id": 18, "tasks": ["14.2", "14.3", "14.4"] },
    { "id": 19, "tasks": ["14.6", "14.7", "14.8", "14.9", "14.10"] },
    { "id": 20, "tasks": ["15.1", "15.2", "15.3", "15.4"] },
    { "id": 21, "tasks": ["15.5", "16.1", "16.2", "16.3", "16.6"] },
    { "id": 22, "tasks": ["16.4", "16.5"] },
    { "id": 23, "tasks": ["17.1", "17.2", "17.3"] },
    { "id": 24, "tasks": ["17.4", "17.5", "17.6"] },
    { "id": 25, "tasks": ["17.7"] }
  ]
}
```
