package dev.itolkien.captions.patches

import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File

class CaptionNativeWiringTest {
    @Test fun `combined APK contains both interfaces and both list hooks`() {
        val path = System.getenv("CAPTIONS_COMBINED_APK")
        assumeTrue(!path.isNullOrBlank(), "Set CAPTIONS_COMBINED_APK for native coexistence wiring")
        val dex = DexFileFactory.loadDexContainer(File(path!!), Opcodes.getDefault())
        val classes = dex.dexEntryNames.flatMap { dex.getEntry(it)!!.dexFile.classes }
        val nativeMarker = "Lapp/morphe/extension/youtube/patches/AutoTranslateCaptionsPatch\$CaptionTrackInterface;"
        val addonMarker = "Ldev/itolkien/captions/extension/CaptionTranslationList\$CaptionTrack;"
        assertTrue(classes.any { nativeMarker in it.interfaces && addonMarker in it.interfaces },
            "The runtime marker must be on the actual addon caption track class")
        val nativeOwner = "Lapp/morphe/extension/youtube/patches/AutoTranslateCaptionsPatch;"
        val addonOwner = "Ldev/itolkien/captions/extension/NativeTranslationBridge;"
        assertTrue(classes.flatMap { it.methods }.any { method ->
            val calls = method.implementation?.instructions?.mapNotNull {
                (it as? ReferenceInstruction)?.reference as? MethodReference
            }.orEmpty()
            calls.any { it.definingClass == nativeOwner && it.name == "addTranslationLanguages" } &&
                calls.any { it.definingClass == addonOwner && it.name == "augment" }
        }, "The translated list must pass through both hooks")
    }
}
