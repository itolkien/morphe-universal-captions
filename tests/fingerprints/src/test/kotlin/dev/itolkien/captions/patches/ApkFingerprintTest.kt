package dev.itolkien.captions.patches

import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.io.OutputStream
import java.security.DigestInputStream
import java.security.MessageDigest

/** Opt-in local test. The APK is never committed, uploaded or used by public CI. */
class ApkFingerprintTest {
    @Test fun `reported APK resolves native default helper rather than its wrapper`() {
        val path = System.getenv("YOUTUBE_2141525_APK")
        assumeTrue(!path.isNullOrBlank(), "Set YOUTUBE_2141525_APK for local exact-APK validation")
        val digest = MessageDigest.getInstance("SHA-256")
        DigestInputStream(File(path!!).inputStream(), digest).use { it.copyTo(OutputStream.nullOutputStream()) }
        assertEquals("e470d11a48be533f607677edeaebdd937982a16e12e6d7ef4e30d7f0fafab2f2",
            digest.digest().joinToString("") { "%02x".format(it) }, "Use the reported clean APK variant")
        val container = DexFileFactory.loadDexContainer(File(path), Opcodes.getDefault())
        val owner = container.dexEntryNames.asSequence()
            .flatMap { container.getEntry(it)!!.dexFile.classes.asSequence() }
            .single { it.type == "Lasku;" }
        val result = resolveDefaultCaptionSelector(owner.methods, owner.type, "Lasmo;", "Lasmq;")
        // These names are evidence for this exact APK only, never production fingerprints.
        assertEquals("s", result.method.name)
        assertEquals("p", result.modelField.name)
    }
}
