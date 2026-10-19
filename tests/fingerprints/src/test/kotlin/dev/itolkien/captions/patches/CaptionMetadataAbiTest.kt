package dev.itolkien.captions.patches

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.immutable.*
import com.android.tools.smali.dexlib2.immutable.instruction.*
import com.android.tools.smali.dexlib2.immutable.reference.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CaptionMetadataAbiTest {
    private fun resolve(ambiguous: Boolean = false, wrongSetter: Boolean = false,
                        labelReads: Int = 0): CaptionMetadataAbi {
        val flags = AccessFlags.PUBLIC.value
        val data = ImmutableFieldReference("LManager;", "randomData", "LData;")
        val targets = ImmutableFieldReference("LData;", "randomTargets", "LList;")
        val code = ImmutableFieldReference("LTarget;", "randomCode", "Ljava/lang/String;")
        val targetName = ImmutableFieldReference("LTarget;", "localizedName", "Ljava/lang/String;")
        val secondName = ImmutableFieldReference("LTarget;", "wrongName", "Ljava/lang/String;")
        val foreignName = ImmutableFieldReference("LForeign;", "wrongName", "Ljava/lang/String;")
        val labelField = ImmutableFieldReference("LBuilder;", "captionLabel", "Ljava/lang/CharSequence;")
        fun method(owner: String, name: String, params: List<String>, result: String,
                   instructions: List<com.android.tools.smali.dexlib2.iface.instruction.Instruction> = emptyList()) =
            ImmutableMethod(owner, name, params.map { ImmutableMethodParameter(it, emptySet(), null) }, result,
                flags, emptySet(), emptySet(), ImmutableMethodImplementation(8, instructions, emptyList(), emptyList()))
        fun clazz(type: String, methods: List<ImmutableMethod>, fields: List<ImmutableField> = emptyList()) =
            ImmutableClassDef(type, flags, "Ljava/lang/Object;", emptyList(), null, emptySet(), fields, methods)
        val setter = method("LBuilder;", "setLanguage", listOf("Ljava/lang/String;"), "V")
        val targetLabels = when (labelReads) {
            1 -> listOf(targetName)
            2 -> listOf(targetName, secondName)
            3 -> listOf(foreignName)
            4 -> listOf(targetName, foreignName)
            else -> emptyList()
        }
        val nativeLabelWrites = targetLabels.flatMap { name -> listOf(
            ImmutableInstruction22c(Opcode.IGET_OBJECT, 4, 3, name),
            ImmutableInstruction22c(Opcode.IPUT_OBJECT, 4, 5, labelField)) }
        val translated = method("LManager;", "translations", emptyList(), "Ljava/util/List;", listOf(
            ImmutableInstruction22c(Opcode.IGET_OBJECT, 1, 7, data),
            ImmutableInstruction22c(Opcode.IGET_OBJECT, 2, 1, targets),
            ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 1, 2, 0, 0, 0, 0,
                ImmutableMethodReference("Ljava/util/List;", "iterator", emptyList(), "Ljava/util/Iterator;")),
            ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 2),
            ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 1, 2, 0, 0, 0, 0,
                ImmutableMethodReference("Ljava/util/Iterator;", "next", emptyList(), "Ljava/lang/Object;")),
            ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 3),
            ImmutableInstruction21c(Opcode.CHECK_CAST, 3, ImmutableTypeReference("LTarget;")),
            ImmutableInstruction22c(Opcode.IGET_OBJECT, 4, 3, code),
            ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 2, 5, 4, 0, 0, 0,
                if (wrongSetter) ImmutableMethodReference("LBuilder;", "other", listOf("Ljava/lang/String;"), "V") else setter)
        ) + nativeLabelWrites + listOf(ImmutableInstruction11x(Opcode.RETURN_OBJECT, 0)))
        val default = method("LManager;", "originalDefault", emptyList(), "LTrack;")
        val manager = clazz("LManager;", listOf(default) + if (ambiguous)
            listOf(method("LManager;", "anotherDefault", emptyList(), "LTrack;")) else emptyList(),
            listOf(ImmutableField(data.definingClass, data.name, data.type, flags, null, emptySet(), emptySet())))
        val track = clazz("LTrack;", emptyList())
        val abi = CaptionTranslationAbi(data, data, clazz("LBuilder;", emptyList()), setter, setter, setter,
            setter, labelField, setter)
        return resolveCaptionMetadataAbi(CaptionTranslationModel(track, translated, translated, code, code), abi, manager)
    }
    @Test fun `catalog discovery follows data flow regardless of obfuscated field names`() {
        val metadata = resolve()
        assertEquals("randomTargets", metadata.targets.name)
        assertEquals("randomCode", metadata.targetCode.name)
        assertEquals("originalDefault", metadata.defaultSource.name)
    }
    @Test fun `native localized name is trusted only when it feeds the track label`() {
        assertEquals("localizedName", resolve(labelReads = 1).targetLabel?.name)
        assertNull(resolve().targetLabel)
        assertNull(resolve(labelReads = 2).targetLabel)
        assertNull(resolve(labelReads = 3).targetLabel)
        assertNull(resolve(labelReads = 4).targetLabel)
    }
    @Test fun `ambiguous defaults and unrecognized setter flows fail closed`() {
        assertThrows(IllegalStateException::class.java) { resolve(ambiguous = true) }
        assertThrows(IllegalStateException::class.java) { resolve(wrongSetter = true) }
    }
}
