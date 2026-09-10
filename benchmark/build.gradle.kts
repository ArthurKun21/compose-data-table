plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.benchmark)
}

kotlin {
    jvmToolchain(17)
}

// JMH requires benchmark classes to be non-final.
allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

dependencies {
    implementation(project(":datatable-material3"))
    implementation(libs.compose.material3)
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.benchmark.runtime)
}

benchmark {
    targets {
        register("main")
    }
    configurations {
        // Full run: ./gradlew :benchmark:benchmark
        named("main") {
            warmups = 3
            iterations = 5
            iterationTime = 1
            iterationTimeUnit = "s"
            advanced("jvmForks", "1")
        }
        // Quick sanity run: ./gradlew :benchmark:smokeBenchmark
        register("smoke") {
            warmups = 1
            iterations = 2
            iterationTime = 300
            iterationTimeUnit = "ms"
            advanced("jvmForks", "1")
        }
        // Optional filter shared by both configurations, e.g.
        // ./gradlew :benchmark:benchmark -PbenchmarkFilter=Scroll
        providers.gradleProperty("benchmarkFilter").orNull?.let { filter ->
            named("main") { include(filter) }
            named("smoke") { include(filter) }
        }
    }
}
