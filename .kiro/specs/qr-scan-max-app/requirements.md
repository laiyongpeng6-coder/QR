# Requirements Document

## Introduction

QR Scan Max is an Android application targeting the overseas Google Play market. It provides QR/barcode scanning, code generation, and AI art beautification features through a modern Jetpack Compose UI. This requirements document defines the app framework, core scanning and generation features, and architectural foundations. The app follows a white-label framework approach with comprehensive documentation to enable future AI-assisted development continuity.

Monetization features (subscriptions, ads) are explicitly excluded from active implementation. Architectural placeholders and comments will be left for future integration.

## Glossary

- **Scanner**: The camera-based module responsible for detecting and decoding QR codes and barcodes in real-time
- **Generator**: The module responsible for creating QR codes and barcodes from user-provided data
- **Result_Mapper**: The rule engine that classifies decoded content and routes it to the appropriate handler (URL, WiFi, vCard, Social Media, etc.)
- **History_Store**: The local encrypted storage system that persists scan and generation records
- **Navigation_Shell**: The top-level Jetpack Compose navigation container managing the 3-tab structure
- **Onboarding_Flow**: The introductory 3-page guided experience shown to first-time users
- **AI_Workspace**: The beautification module that applies visual styles to generated QR codes
- **Product_Lookup**: The module that queries external databases for barcode product information
- **Camera_Controller**: The component managing camera lifecycle, permissions, auto-zoom, and flash control
- **Tab_Bar**: The bottom navigation component with History (left), Scan (center), and Create (right) tabs

## Requirements

### Requirement 1: Application Architecture and Module Structure

**User Story:** As a developer, I want a well-structured modular Android project using Jetpack Compose, so that features can be developed, tested, and maintained independently.

#### Acceptance Criteria

1. THE Navigation_Shell SHALL use a single-Activity architecture with Jetpack Compose Navigation managing all screen transitions
2. THE Navigation_Shell SHALL organize source code into feature modules: scanner, generator, history, ai-workspace, onboarding, and a shared core module
3. THE Navigation_Shell SHALL use Hilt for dependency injection across all feature modules
4. THE Navigation_Shell SHALL define a clear data layer using Repository pattern with Room database for local persistence
5. THE Navigation_Shell SHALL target Android API level 24 (minimum) and compile against the latest stable SDK
6. THE Navigation_Shell SHALL include Kotlin coroutines and Flow for all asynchronous operations

### Requirement 2: Three-Tab Navigation Structure

**User Story:** As a user, I want a simple bottom navigation with three tabs, so that I can quickly access scanning, history, and code creation features.

#### Acceptance Criteria

1. THE Tab_Bar SHALL display three tabs: History (left position), Scan (center position), and Create (right position)
2. WHEN the app launches after onboarding is complete, THE Navigation_Shell SHALL display the Scan tab as the default selected tab
3. WHEN a user taps a tab, THE Navigation_Shell SHALL switch to the corresponding screen within 100ms perceived transition time
4. WHILE navigating between tabs, THE Navigation_Shell SHALL preserve the scroll state and UI state of each tab independently
5. THE Tab_Bar SHALL use Material 3 design tokens for icons, labels, and selection indicators

### Requirement 3: Onboarding Flow

**User Story:** As a first-time user, I want a guided introduction to the app, so that I understand the key features before using them.

#### Acceptance Criteria

1. WHEN the app is launched for the first time, THE Onboarding_Flow SHALL display a 3-page horizontal swipe introduction
2. THE Onboarding_Flow SHALL display a skip button on each page allowing the user to proceed directly to the main app
3. THE Onboarding_Flow SHALL display a completion button on the final page that navigates to the Scan tab
4. WHEN onboarding is completed or skipped, THE Onboarding_Flow SHALL persist the completion state so onboarding is not shown again on subsequent launches
5. THE Onboarding_Flow SHALL support localization for page titles, descriptions, and button labels

### Requirement 4: Camera-Based QR and Barcode Scanning

**User Story:** As a user, I want to scan QR codes and barcodes using my camera, so that I can quickly decode information from physical codes.

#### Acceptance Criteria

1. WHEN the Scan tab is active, THE Camera_Controller SHALL display a full-screen camera preview with a viewfinder overlay
2. WHEN a QR code or barcode enters the viewfinder, THE Scanner SHALL decode the content within 500ms of detection
3. THE Scanner SHALL support the following formats: QR Code, EAN-13, EAN-8, UPC-A, UPC-E, Code 128, Code 39, ITF, PDF417, Data Matrix, and Aztec
4. WHEN the ambient light level is below a defined threshold, THE Camera_Controller SHALL automatically activate the device flash as a torch
5. WHEN a small QR code is detected at distance, THE Camera_Controller SHALL apply digital auto-zoom to bring the code into readable range
6. IF camera permission is denied, THEN THE Scanner SHALL display a permission rationale screen with a button to open system settings
7. WHEN the user taps the album import button, THE Scanner SHALL open the device gallery for image selection and decode any QR or barcode found in the selected image

### Requirement 5: Scan Result Rule Mapping Engine

**User Story:** As a user, I want scan results to be automatically classified and presented with relevant actions, so that I can act on decoded content without manual interpretation.

#### Acceptance Criteria

1. WHEN a code is successfully decoded, THE Result_Mapper SHALL classify the content into one of the following types: URL, WiFi, vCard, Phone, Email, SMS, Social Media, Geographic Location, or Plain Text
2. WHEN content is classified as URL, THE Result_Mapper SHALL display an action to open the URL in the default browser
3. WHEN content is classified as WiFi, THE Result_Mapper SHALL display the network name, security type, and a connect action
4. WHEN content is classified as vCard, THE Result_Mapper SHALL display contact fields and an action to save to the device contacts
5. WHEN content is classified as Phone, THE Result_Mapper SHALL display the phone number and actions to call or send SMS
6. WHEN content is classified as Social Media, THE Result_Mapper SHALL detect the platform and display a deep-link action to open the corresponding app
7. IF content does not match any known pattern, THEN THE Result_Mapper SHALL classify it as Plain Text and display copy and share actions

### Requirement 6: QR Code and Barcode Generation

**User Story:** As a user, I want to generate QR codes from various data types, so that I can share information in a scannable format.

#### Acceptance Criteria

1. THE Generator SHALL support creating QR codes from the following input types: Plain Text, URL, WiFi credentials, Contact (vCard), Phone number, and Social Media profile link
2. WHEN the user selects an input type and provides valid data, THE Generator SHALL render a QR code preview within 300ms
3. THE Generator SHALL validate input data before generation and display inline error messages for invalid fields
4. WHEN a QR code is generated, THE Generator SHALL provide actions to save the image to device gallery, share via system share sheet, and copy the encoded content
5. THE Generator SHALL allow the user to select output resolution from predefined options (256px, 512px, 1024px)
6. IF the input data exceeds the maximum capacity for the selected QR version, THEN THE Generator SHALL display an error indicating the data is too large

### Requirement 7: AI Art Beautification Workspace

**User Story:** As a user, I want to apply visual styles to my generated QR codes, so that they are visually appealing for sharing on social media or print materials.

#### Acceptance Criteria

1. WHEN a QR code is generated, THE AI_Workspace SHALL offer a "Beautify" action that navigates to the styling workspace
2. THE AI_Workspace SHALL provide a free tier of solid color customization options for foreground and background colors
3. THE AI_Workspace SHALL display a real-time preview of the styled QR code that updates within 200ms of a style change
4. THE AI_Workspace SHALL validate that styled QR codes remain scannable by running an internal decode check before allowing save
5. WHEN the user confirms a style, THE AI_Workspace SHALL provide save and share actions identical to the Generator output actions
6. THE AI_Workspace SHALL define extension points for future gradient styles and AI template features with placeholder UI elements marked as "Coming Soon"

### Requirement 8: History with Timeline Layout

**User Story:** As a user, I want to view my scan and generation history in a timeline, so that I can revisit past results without rescanning.

#### Acceptance Criteria

1. THE History_Store SHALL display scan and generation records in a reverse-chronological waterfall timeline layout
2. THE History_Store SHALL group records by date with visible date headers
3. WHEN the user swipes left on a history item, THE History_Store SHALL reveal a delete action
4. WHEN the user swipes right on a history item, THE History_Store SHALL reveal a favorite/pin action
5. WHEN the user taps a history item, THE History_Store SHALL navigate to the full result detail screen with all original actions available
6. THE History_Store SHALL persist all records in a local Room database with encryption at rest using Android Keystore-backed keys
7. THE History_Store SHALL support search by content text, type classification, or date range

### Requirement 9: Barcode Product Lookup

**User Story:** As a user, I want to look up product information when I scan a barcode, so that I can identify products and compare basic details.

#### Acceptance Criteria

1. WHEN a scanned barcode is classified as a product code (EAN-13, EAN-8, UPC-A, UPC-E), THE Product_Lookup SHALL query an external product database for matching product information
2. WHEN product information is found, THE Product_Lookup SHALL display the product name, description, and category
3. IF no product information is found, THEN THE Product_Lookup SHALL display the raw barcode number with a "No product info available" message
4. IF the network is unavailable, THEN THE Product_Lookup SHALL display the raw barcode number and indicate that product lookup requires an internet connection
5. THE Product_Lookup SHALL cache previously looked-up product data locally for offline access

### Requirement 10: Documentation and AI Continuity Framework

**User Story:** As a developer or AI assistant, I want comprehensive inline documentation and architecture decision records, so that development can be continued by any team member or AI tool.

#### Acceptance Criteria

1. THE Navigation_Shell SHALL include a top-level ARCHITECTURE.md file documenting module boundaries, data flow, and dependency graph
2. THE Navigation_Shell SHALL include KDoc comments on all public classes, interfaces, and functions describing purpose, parameters, and return values
3. THE Navigation_Shell SHALL include a CONTRIBUTING.md file with build instructions, module creation guide, and coding conventions
4. THE Navigation_Shell SHALL include TODO comments with the prefix "// TODO [FUTURE-MONETIZATION]:" at all points where subscription or ad logic will be integrated
5. THE Navigation_Shell SHALL include a ROADMAP.md file listing deferred features (subscriptions, ads, Pro AI templates, gradient styles) with integration guidance for each

### Requirement 11: Localization and Internationalization

**User Story:** As an overseas user, I want the app to support multiple languages, so that I can use it comfortably in my preferred language.

#### Acceptance Criteria

1. THE Navigation_Shell SHALL externalize all user-facing strings into Android resource files (strings.xml)
2. THE Navigation_Shell SHALL provide English as the default language with complete string coverage
3. THE Navigation_Shell SHALL support RTL (right-to-left) layout mirroring for RTL languages
4. THE Navigation_Shell SHALL format dates, numbers, and units according to the device locale settings

### Requirement 12: Performance and Responsiveness

**User Story:** As a user, I want the app to feel fast and responsive, so that scanning and navigation do not feel sluggish.

#### Acceptance Criteria

1. THE Navigation_Shell SHALL achieve cold start to Scan tab camera preview in under 2 seconds on mid-range devices (Snapdragon 600 series equivalent)
2. THE Scanner SHALL maintain a minimum frame processing rate of 15 frames per second during active scanning
3. THE Navigation_Shell SHALL not drop more than 3 frames during tab switching animations
4. THE History_Store SHALL load and display the first 20 history items within 300ms of the History tab becoming visible

---

## Future Considerations (Not In Scope - Placeholders Only)

The following features are explicitly excluded from active implementation but architectural hooks and placeholder comments will be included:

- **Subscription Monetization**: Tiered Pro subscription unlocking AI templates and advanced features
- **Ad Integration**: Rewarded video ads for unlocking gradient styles, interstitial ads at natural breakpoints
- **Advanced AI Templates**: Server-side AI-generated QR code art styles
- **Cloud Sync**: Cross-device history synchronization
- **Batch Scanning**: Continuous multi-code scanning mode
- **Price Comparison**: Extended product lookup with multi-retailer price comparison
