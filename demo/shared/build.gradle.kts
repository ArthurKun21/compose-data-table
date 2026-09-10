@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(11)

    android {
        namespace = "com.seanproctor.datatable.demo.shared"
        compileSdk = 37
        minSdk = 24
    }
    jvm()
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "DemoApp"
            isStatic = true
        }
    }
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":datatable-material3"))
            implementation(libs.compose.material3)
            implementation(libs.fastscroller.m3)
        }
    }
}
