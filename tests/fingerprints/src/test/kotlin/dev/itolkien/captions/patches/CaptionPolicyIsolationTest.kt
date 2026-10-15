package dev.itolkien.captions.patches

import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File

/** Both files remain local; compare with clean YouTube or an already Morphe-patched baseline. */
class CaptionPolicyIsolationTest {
    private fun classes(file: String): Map<String, ClassDef> {
        val container = DexFileFactory.loadDexContainer(File(file), Opcodes.getDefault())
        return container.dexEntryNames.flatMap { container.getEntry(it)!!.dexFile.classes }.associateBy { it.type }
    }

    private fun canonicalDefinition(type: ClassDef): ByteArray {
        val memory = MemoryDataStore()
        DexPool.writeTo(memory, ImmutableDexFile(Opcodes.getDefault(), listOf(type)))
        return memory.buffer.copyOf(memory.size)
    }

    @Test fun `addon preserves the native caption policy classes`() {
        val before = System.getenv("CAPTIONS_BASELINE_APK")
        val after = System.getenv("CAPTIONS_PATCHED_APK")
        assumeTrue(!before.isNullOrBlank() && !after.isNullOrBlank(),
            "Set CAPTIONS_BASELINE_APK and CAPTIONS_PATCHED_APK to check native policy isolation")
        val baseline = classes(before!!)
        val patched = classes(after!!)
        val policyClasses = baseline.values.filter { type ->
            type.methods.any { method ->
                method.implementation?.instructions?.any { instruction ->
                    val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                    ref?.definingClass == "Landroid/view/accessibility/CaptioningManager;"
                } == true
            }
        }
        assertFalse(policyClasses.isEmpty(), "The fixture must include real native caption policy")
        for (type in policyClasses) {
            val actual = patched[type.type]
            assertNotNull(actual, "Native caption policy class removed: ${type.type}")
            assertArrayEquals(canonicalDefinition(type), canonicalDefinition(actual!!),
                "Caption visibility/selection policy changed: ${type.type}")
        }
        println("Preserved ${policyClasses.size} native caption policy classes")
    }
}
