package dev.itolkien.captions.patches

import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File

class CaptionFallbackWiringTest {
    private fun classes(path: String): List<ClassDef> {
        val dex = DexFileFactory.loadDexContainer(File(path), Opcodes.getDefault())
        return dex.dexEntryNames.flatMap { dex.getEntry(it)!!.dexFile.classes }
    }
    private fun Method.calls() = implementation!!.instructions.mapNotNull {
        (it as? ReferenceInstruction)?.reference as? MethodReference
    }
    @Test fun `original menu identities remain discoverable and raw getters cannot recurse`() {
        val baseline = System.getenv("YOUTUBE_CAPTIONS_APK")
        val patched = System.getenv("CAPTIONS_FALLBACK_APK")
        assumeTrue(!baseline.isNullOrBlank() && !patched.isNullOrBlank(), "Supply clean and experimental patched APKs")
        val clean = resolveCaptionTranslationModel(classes(baseline!!))
        val patchedClasses = classes(patched!!)
        val discovered = resolveCaptionTranslationModel(patchedClasses)
        assertEquals(clean.translated.name, discovered.translated.name)
        assertEquals(clean.direct.name, discovered.direct.name)
        val model = patchedClasses.single { it.type == clean.translated.definingClass }
        val source = "Ldev/itolkien/captions/extension/EmptyTranslationFallback\$CaptionSource;"
        val bridge = "Ldev/itolkien/captions/extension/NativeTranslationBridge;"
        assertTrue(source in model.interfaces, "The native manager must implement the raw-source bridge")
        val direct = clean.translated.definingClass.let { owner ->
            classes(baseline).single { it.type == owner }.methods.single { method ->
                method.returnType == "Ljava/util/List;" && method.parameterTypes.isEmpty() &&
                    method.implementation?.instructions?.any {
                        ((it as? ReferenceInstruction)?.reference as? com.android.tools.smali.dexlib2.iface.reference.StringReference)?.string == "AUTO_TRANSLATE_CAPTIONS_OPTION"
                    } == true
            }
        }
        for ((native, hook, raw) in listOf(
                Triple(direct, "fallbackDirect", "universalCaptions_nativeDirect"),
                Triple(clean.translated, "fallbackTranslated", "universalCaptions_nativeTranslated"))) {
            val wrapper = model.methods.single { it.name == native.name && it.parameterTypes.isEmpty() }
            assertTrue(wrapper.calls().any { it.definingClass == bridge && it.name == hook })
            val anchor = if (hook == "fallbackDirect") "AUTO_TRANSLATE_CAPTIONS_OPTION" else "&tlang="
            assertTrue(wrapper.implementation!!.instructions.any {
                ((it as? ReferenceInstruction)?.reference as? com.android.tools.smali.dexlib2.iface.reference.StringReference)?.string == anchor
            }, "Morphe must still find the original named native method by its anchor")
            val original = model.methods.single { it.name == raw }
            assertEquals(listOf("Z"), original.parameterTypes.toList(),
                "Raw copies must not match native parameterless menu fingerprints")
            assertFalse(original.calls().any { it.definingClass == bridge && it.name.startsWith("fallback") })
            assertFalse(original.calls().any { it.definingClass == model.type && it.name == native.name })
        }
        val baselineClasses = classes(baseline).associateBy { it.type }
        val abi = resolveCaptionTranslationAbi(clean.track, clean.translated, clean.language, clean.vss, baselineClasses::get)
        val metadata = resolveCaptionMetadataAbi(clean, abi, baselineClasses[model.type]!!)
        val targets = model.methods.single { it.name == "universalCaptions_targetLanguages" }
        assertEquals(listOf("Z"), targets.parameterTypes.toList())
        val reads = targets.implementation!!.instructions.mapNotNull { (it as? ReferenceInstruction)?.reference }
        assertTrue(metadata.metadata in reads && metadata.targets in reads && metadata.targetCode in reads)
        val defaultSource = model.methods.single { it.name == "universalCaptions_defaultSource" }
        assertEquals(listOf("Z"), defaultSource.parameterTypes.toList())
        assertTrue(defaultSource.calls().any { it == metadata.defaultSource })
        val factory = model.methods.single { it.name == "universalCaptions_autoTranslateOption" }
        assertTrue(factory.calls().any { it.returnType == clean.track.type })
        assertEquals(Opcode.RETURN_OBJECT, factory.implementation!!.instructions.last().opcode)
    }
}
