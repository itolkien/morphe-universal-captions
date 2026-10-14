plugins { kotlin("jvm") version "2.2.0" }
repositories { mavenCentral(); google() }
dependencies {
    implementation("com.android.tools.smali:smali-dexlib2:3.0.9")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.1")
}
kotlin { jvmToolchain(17) }
sourceSets.main {
    kotlin.srcDir("../../patches/src/main/kotlin")
    kotlin.include("**/CaptionFingerprints.kt")
}
tasks.test { useJUnitPlatform() }
