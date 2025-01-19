import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "org.example"
version = "1.0-SNAPSHOT"

kotlin {
    jvmToolchain {
        this.languageVersion.value(JavaLanguageVersion.of(21))
        vendor.value(JvmVendorSpec.JETBRAINS)
    }
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs) {
        exclude("org.jetbrains.compose.material", "material")
    }
    implementation(compose.foundation)
    implementation(compose.ui)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.slf4j:slf4j-simple:2.0.1")
    implementation("org.apache.pdfbox:pdfbox:2.0.33")
    implementation("org.openani.jsystemthemedetector:jSystemThemeDetector:3.8")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "pdf-extractor"
            packageVersion = "1.1.0"
        }

        buildTypes.release {
            proguard {
                obfuscate = false
                configurationFiles.from(file("proguard-rules.pro"))
            }
        }
    }
}

