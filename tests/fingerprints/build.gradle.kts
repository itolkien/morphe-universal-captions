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
    kotlin.include("**/CaptionFingerprints.kt", "**/CaptionTranslationAbi.kt")
}
sourceSets.main {
    java.srcDir("../../extensions/extension/src/main/java")
    java.include("**/CaptionTranslationList.java", "**/CaptionIntegration.java")
}
sourceSets.test {
    java.srcDir("../java")
    java.include("**/CaptionTranslationListTest.java", "**/CaptionIntegrationTest.java",
        "app/morphe/extension/youtube/patches/AutoTranslateCaptionsPatch.java")
}
tasks.test {
    useJUnitPlatform()
    for (name in listOf("YOUTUBE_CAPTIONS_APK", "CAPTIONS_BASELINE_APK", "CAPTIONS_PATCHED_APK", "CAPTIONS_COMBINED_APK")) {
        val path = providers.environmentVariable(name)
        inputs.property(name, path.orElse(""))
        path.orNull?.let { inputs.file(it).withPropertyName("${name}_file") }
    }
}
