plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.seanproctor.datatable.android"
    compileSdk = 37
    defaultConfig {
        minSdk = 24
        targetSdk = 37
        applicationId = "com.seanproctor.datatable.android"
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation(project(":demo:shared"))
    implementation(libs.activity.compose)
    implementation(libs.compose.ui.tooling)
}
