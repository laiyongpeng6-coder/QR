plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.qrscanmax.core.domain"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Desugaring for java.time on API < 26
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Coroutines core (pure domain layer - no Android framework dependencies)
    implementation(libs.kotlinx.coroutines.core)

    // javax.inject for DI annotations without framework coupling
    implementation("javax.inject:javax.inject:1")

    // Testing
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotest.property)
    testImplementation(libs.kotest.assertions.core)
}
