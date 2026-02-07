plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

group = "xdm"
version = "8.0.0-SNAPSHOT"

kotlin {
    // JVM (Desktop + Android host)
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }

    // Native - Linux
    linuxX64()
    linuxArm64()

    // Native - Windows
    mingwX64()

    // Native - macOS
    macosX64()
    macosArm64()

    // Native - iOS (for potential iOS companion app)
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    // WASM (for web UI / browser extension backend)
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.6.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
                implementation("io.ktor:ktor-client-core:3.0.3")
                implementation("io.ktor:ktor-client-content-negotiation:3.0.3")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.3")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
            }
        }

        // JVM
        val jvmMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-cio:3.0.3")
            }
        }

        // Shared native source set
        val nativeMain by creating { dependsOn(commonMain) }

        // Desktop native (curl-backed HTTP)
        val desktopNativeMain by creating {
            dependsOn(nativeMain)
            dependencies {
                implementation("io.ktor:ktor-client-curl:3.0.3")
            }
        }
        val linuxX64Main by getting { dependsOn(desktopNativeMain) }
        val linuxArm64Main by getting { dependsOn(desktopNativeMain) }
        val mingwX64Main by getting { dependsOn(desktopNativeMain) }
        val macosX64Main by getting { dependsOn(desktopNativeMain) }
        val macosArm64Main by getting { dependsOn(desktopNativeMain) }

        // iOS native (Darwin-backed HTTP)
        val iosMain by creating {
            dependsOn(nativeMain)
            dependencies {
                implementation("io.ktor:ktor-client-darwin:3.0.3")
            }
        }
        val iosX64Main by getting { dependsOn(iosMain) }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }

        // WASM
        val wasmJsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-js:3.0.3")
            }
        }
    }
}
