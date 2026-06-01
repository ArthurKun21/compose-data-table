package com.seanproctor.datatable

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Configure base Kotlin options for all targets
 */
@OptIn(ExperimentalWasmDsl::class)
internal fun Project.configureKotlinMultiplatform(
    extension: KotlinMultiplatformExtension
) {
    extension.apply {
        jvmToolchain(11)

        // targets
        // AGP 9 removed the `androidLibrary { }` extension function; the
        // com.android.kotlin.multiplatform.library plugin now registers the
        // Android target as a named extension on the Kotlin extension instead.
        (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("androidLibrary") {
            minSdk = 21
            compileSdk = 36
            namespace = "com.seanproctor." + project.name.replace("-", ".")
            androidResources.enable = true
        }
        jvm()
        js {
            browser()
            useEsModules()
        }
        wasmJs {
            browser()
        }
        iosArm64()
        iosSimulatorArm64()
    }
}
