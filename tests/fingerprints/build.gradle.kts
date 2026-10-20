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
    kotlin.include("**/CaptionFingerprints.kt", "**/CaptionTranslationAbi.kt", "**/CaptionFallbackAbi.kt", "**/CaptionMetadataAbi.kt")
}
sourceSets.main {
    java.srcDir("../../extensions/extension/src/main/java")
    java.include("**/CaptionTranslationList.java",
        "**/OriginalCaptionSource.java", "**/EmptyTranslationFallback.java",
        "**/CaptionPenStyles.java", "**/TimedTextUrl.java")
}
sourceSets.test {
    java.srcDir("../java")
    java.srcDir("../policy-upstream")
    resources.srcDir("../fixtures")
}
tasks.test {
    useJUnitPlatform()
    for (name in listOf("YOUTUBE_CAPTIONS_APK", "CAPTIONS_BASELINE_APK", "CAPTIONS_PATCHED_APK", "CAPTIONS_COMBINED_APK", "CAPTIONS_FALLBACK_APK")) {
        val path = providers.environmentVariable(name)
        inputs.property(name, path.orElse(""))
        path.orNull?.let { inputs.file(it).withPropertyName("${name}_file") }
    }
}
