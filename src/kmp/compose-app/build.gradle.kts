plugins {
    kotlin("jvm")
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
    id("org.graalvm.buildtools.native") version "0.10.4"
    application
}

group = "xdm"
version = "8.0.0-SNAPSHOT"

application {
    mainClass.set("xdm.ComposeMainKt")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":ui"))
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}

kotlin {
    jvmToolchain(21)
}

compose.desktop {
    application {
        mainClass = "xdm.ComposeMainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
            )
            packageName = "xdm"
            packageVersion = "8.0.0"
            description = "Xtreme Download Manager"
            vendor = "XDM"
            linux { iconFile.set(file("icons/xdm.png")) }
            windows { iconFile.set(file("icons/xdm.ico")) }
            macOS { iconFile.set(file("icons/xdm.icns")) }
        }
    }
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("xdm-native")
            mainClass.set("xdm.ComposeMainKt")
            buildArgs.addAll(
                "-O2",
                "--no-fallback",
                "--initialize-at-build-time=kotlinx.serialization",
                "--initialize-at-build-time=kotlinx.coroutines",
                "--allow-incomplete-classpath",
            )
        }
    }
}
