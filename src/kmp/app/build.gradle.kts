plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
    id("org.graalvm.buildtools.native") version "0.10.4"
}

group = "xdm"
version = "8.0.0-SNAPSHOT"

application {
    mainClass.set("xdm.MainKt")
}

dependencies {
    implementation(project(":core"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("io.ktor:ktor-client-cio:3.0.3")
}

kotlin {
    jvmToolchain(21) // GraalVM CE 21+
}

/**
 * GraalVM Native Image configuration.
 *
 * This compiles the Kotlin/JVM code to a native binary using GraalVM CE.
 * The result is a single static executable with:
 * - No JVM dependency at runtime
 * - ~15-25MB binary size (vs ~50MB JVM + jar)
 * - ~10-20MB RAM (vs ~40-60MB JVM)
 * - Near-instant startup (~50ms vs ~2s JVM)
 *
 * Comparison with .NET Native AOT:
 * - GraalVM CE: free, open source, supports reflection via config
 * - .NET AOT: built-in to .NET 9, tighter OS integration on Windows
 * - Both produce single-file native executables
 * - Both support Linux, Windows, macOS
 */
graalvmNative {
    binaries {
        named("main") {
            imageName.set("xdm")
            mainClass.set("xdm.MainKt")

            buildArgs.addAll(
                // Optimization
                "-O2",
                "--no-fallback",

                // Static linking on Linux (musl)
                // "--static", "--libc=musl",  // Uncomment for fully static binary

                // Reduce binary size
                "-H:+UnlockExperimentalVMOptions",
                "-H:+CompactingOldGen",

                // Serialization support (kotlinx.serialization uses reflection)
                "--initialize-at-build-time=kotlinx.serialization",

                // Ktor + coroutines support
                "--initialize-at-build-time=io.ktor",
                "--initialize-at-build-time=kotlinx.coroutines",

                // Allow incomplete classpath
                "--allow-incomplete-classpath",
            )

            // Resource bundles
            resources {
                autodetect()
            }
        }
    }

    toolchainDetection.set(true)
}

// Task to generate GraalVM reflection config
tasks.register("generateReflectionConfig") {
    doLast {
        val configDir = file("src/main/resources/META-INF/native-image")
        configDir.mkdirs()

        // Minimal reflection config for kotlinx.serialization
        file("$configDir/reflect-config.json").writeText("""
        [
          {
            "name": "xdm.config.AppConfig",
            "allDeclaredConstructors": true,
            "allPublicConstructors": true,
            "allDeclaredMethods": true,
            "allPublicMethods": true,
            "allDeclaredFields": true
          },
          {
            "name": "xdm.model.DownloadEntry",
            "allDeclaredConstructors": true,
            "allPublicConstructors": true,
            "allDeclaredMethods": true,
            "allPublicMethods": true,
            "allDeclaredFields": true
          }
        ]
        """.trimIndent())
    }
}
