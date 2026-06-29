plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.crashlytics)
}

android {
    namespace = "com.qrscanfast.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.qrscanfast.qr"
        minSdk = 24
        targetSdk = 35
        versionCode = 8
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // AdMob App ID - Google official test App ID for development
            manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-3940256099942544~3347511713"

            // Google official test ad unit IDs for development
            buildConfigField("String", "AD_OPEN_COLD_START", "\"ca-app-pub-3940256099942544/9257395921\"")
            buildConfigField("String", "AD_INTERSTITIAL_SCAN", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "AD_INTERSTITIAL_GENERATE", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "AD_INTERSTITIAL_AI_BEAUTIFY", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "AD_INTERSTITIAL_ADVANCED_UNLOCK", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "AD_NATIVE_ONBOARDING", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "AD_NATIVE_HOME_TAB", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "AD_NATIVE_HISTORY_LIST", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "AD_NATIVE_SCAN_RESULT_DETAIL", "\"ca-app-pub-3940256099942544/2247696110\"")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
            // AdMob App ID - 真实 App ID
            manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-8357438281849511~6312167603"

            // 真实广告单元 ID
            buildConfigField("String", "AD_OPEN_COLD_START", "\"ca-app-pub-8357438281849511/4291253318\"")
            buildConfigField("String", "AD_INTERSTITIAL_SCAN", "\"ca-app-pub-8357438281849511/2860007750\"")
            buildConfigField("String", "AD_INTERSTITIAL_GENERATE", "\"ca-app-pub-8357438281849511/7943823793\"")
            buildConfigField("String", "AD_INTERSTITIAL_AI_BEAUTIFY", "\"ca-app-pub-8357438281849511/2013829948\"")
            buildConfigField("String", "AD_INTERSTITIAL_ADVANCED_UNLOCK", "\"ca-app-pub-8357438281849511/9835489964\"")
            buildConfigField("String", "AD_NATIVE_ONBOARDING", "\"ca-app-pub-8357438281849511/2372922593\"")
            buildConfigField("String", "AD_NATIVE_HOME_TAB", "\"ca-app-pub-8357438281849511/5213452747\"")
            buildConfigField("String", "AD_NATIVE_HISTORY_LIST", "\"ca-app-pub-8357438281849511/3900371079\"")
            buildConfigField("String", "AD_NATIVE_SCAN_RESULT_DETAIL", "\"ca-app-pub-8357438281849511/8387666608\"")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // 原生库 16KB 页面对齐（Android 15+ 设备要求）
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {
    // Desugaring for java.time on API < 26
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Core modules
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    implementation(project(":core:common"))
    implementation(project(":core:ads"))
    implementation(project(":core:billing"))

    // Feature modules
    implementation(project(":feature:scanner"))
    implementation(project(":feature:generator"))
    implementation(project(":feature:history"))
    implementation(project(":feature:ai-workspace"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:product-lookup"))
    implementation(project(":feature:subscription"))

    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    // AppCompat（提供 per-app 语言切换 AppCompatDelegate.setApplicationLocales）
    implementation(libs.androidx.appcompat)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Google AdMob SDK (needed for MobileAds.initialize in Application)
    implementation(libs.play.services.ads)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.config)

    // Testing
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    // Android Instrumented Testing
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
