package com.seanproctor.datatable

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

// The com.android.kotlin.multiplatform.library plugin registers the Android target as an
// extension on KotlinMultiplatformExtension. Build-script DSL accessors are generated only
// for build scripts, not convention plugins, so resolve it via the ExtensionAware API.
private fun KotlinMultiplatformExtension.androidLibrary(
    action: KotlinMultiplatformAndroidLibraryTarget.() -> Unit,
) {
    (this as ExtensionAware).extensions.configure(
        KotlinMultiplatformAndroidLibraryTarget::class.java,
        action,
    )
}

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
        androidLibrary {
            minSdk = 23
            compileSdk = 37
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
