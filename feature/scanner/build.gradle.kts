plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.qrscanfast.feature.scanner"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

// 强制使用 listenablefuture 1.0 真实库（仅编译类路径），覆盖 CameraX 引入的 9999.0 空桩库
configurations.matching { it.name.contains("CompileClasspath", ignoreCase = true) }.all {
    resolutionStrategy {
        force("com.google.guava:listenablefuture:1.0")
    }
}

dependencies {
    // Core modules
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:ads"))

    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // CameraX
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.camerax.mlkit.vision)

    // 提供 ListenableFuture 实际类（CameraX 的 enableTorch 等方法返回此类型，编译时需要访问）
    // 注意：CameraX 会引入空桩库 listenablefuture:9999.0，需要在下方 configurations 中强制使用 1.0 真实库
    implementation(libs.guava.listenablefuture)

    // ML Kit Barcode Scanning
    implementation(libs.mlkit.barcode)
}
