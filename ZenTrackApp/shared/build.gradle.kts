plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm()

    // android {} replaces both androidTarget {} (KMP target) and the top-level android {} block.
    // Required by com.android.kotlin.multiplatform.library (AGP 9.0+).
    android {
        namespace  = "me.dcueto.zentrackapp.shared"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk     = libs.versions.androidMinSdk.get().toInt()
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.clientCore)
            implementation(libs.ktor.clientContentNegotiation)
            implementation(libs.ktor.serializationJson)
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.clientCio)
        }
        androidMain.dependencies {
            implementation(libs.ktor.clientOkhttp)
        }
    }
    jvmToolchain(8)
}