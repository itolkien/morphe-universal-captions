package dev.itolkien.captions.patches

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.immutable.*
import com.android.tools.smali.dexlib2.immutable.instruction.*
import com.android.tools.smali.dexlib2.immutable.reference.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** Synthetic obfuscated DEX models: eligibility must follow *relationships*, not names. */
class CaptionEligibleSourceAbiTest {
    private val metadata = ImmutableFieldReference("LManager;", "anything", "LData;")
    private val audio = ImmutableFieldReference("LManager;", "other", "LAudio;")
    private val extraAudio = ImmutableFieldReference("LManager;", "extra", "LUnknown;")
    private val targets = ImmutableFieldReference("LData;", "catalog", "LMessageList;")
    private val originals = ImmutableFieldReference("LData;", "sourceMetadata", "LMessageList;")
    private val general = ImmutableFieldReference("LData;", "indexFallback", "LIntList;")
    private val extraGeneral = ImmutableFieldReference("LData;", "otherList", "LIntList;")
    private val audioIndices = ImmutableFieldReference("LAudio;", "captionIndices", "LIntList;")
    private val targetCode = ImmutableFieldReference("LTarget;", "languageCode", "Ljava/lang/String;")
    private val sourceType = "LSource;"
    private val flags = AccessFlags.PUBLIC.value
    private val copy = ImmutableMethodReference("LManager;", "convertAnyName", listOf(sourceType), "LTrack;")
    private val get = ImmutableMethodReference("Ljava/util/List;", "get", listOf("I"), "Ljava/lang/Object;")

    private fun field(f: ImmutableFieldReference) =
        ImmutableField(f.definingClass, f.name, f.type, flags, null, emptySet(), emptySet())

    private fun method(name: String, result: String, ins: List<Instruction> = emptyList(),
                       params: List<String> = emptyList(), flags: Int = this.flags) =
        ImmutableMethod("LManager;", name, params.map { ImmutableMethodParameter(it, emptySet(), null) },
            result, flags, emptySet(), emptySet(),
            ImmutableMethodImplementation(8, ins, emptyList(), emptyList()))

    private fun clazz(type: String, methods: List<ImmutableMethod> = emptyList(),
                      fields: List<ImmutableField> = emptyList()) =
        ImmutableClassDef(type, flags, "Ljava/lang/Object;", emptyList(), null,
            emptySet(), fields, methods)

    private fun read(f: ImmutableFieldReference, to: Int, from: Int) =
        ImmutableInstruction22c(Opcode.IGET_OBJECT, to, from, f)

    private fun build(ambiguousGeneral: Boolean = false, ambiguousAudio: Boolean = false,
                      missingDirectConverter: Boolean = false, missingAudioIndices: Boolean = false):
            CaptionEligibleSourceAbi? {
        val fromNative = method(copy.name, copy.returnType, params = listOf(sourceType),
            flags = AccessFlags.PRIVATE.value)
        fun instructions(): List<Instruction> = listOf(
            read(metadata, 1, 7),
            read(audio, 2, 7),
            read(audioIndices, 3, 2),
            read(general, 4, 1),
            read(originals, 5, 1),
            ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 2, 5, 6, 0, 0, 0, get),
            ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 6),
            ImmutableInstruction21c(Opcode.CHECK_CAST, 6, ImmutableTypeReference(sourceType)),
            ImmutableInstruction35c(Opcode.INVOKE_DIRECT, 2, 7, 6, 0, 0, 0, copy),
            ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 0),
            ImmutableInstruction11x(Opcode.RETURN_OBJECT, 0))
        var translated: List<Instruction> = instructions()
        var direct: List<Instruction> = instructions()
        if (ambiguousGeneral) {
            translated = listOf(read(extraGeneral, 4, 1)) + translated
            direct = listOf(read(extraGeneral, 4, 1)) + direct
        }
        if (ambiguousAudio) {
            translated = listOf(read(extraAudio, 4, 7)) + translated
            direct = listOf(read(extraAudio, 4, 7)) + direct
        }
        if (missingDirectConverter)
            direct = direct.filterNot { (it as? ImmutableInstruction35c)?.reference == copy }
        if (missingAudioIndices)
            direct = direct.filterNot { (it as? ImmutableInstruction22c)?.reference == audioIndices }
        val tr = method("nativeTranslations", "Ljava/util/List;", translated)
        val dr = method("nativeDirect", "Ljava/util/List;", direct)
        val originalDefault = method("nativeDefault", "LTrack;")
        val manager = clazz("LManager;", listOf(tr, dr, fromNative, originalDefault),
            listOf(field(metadata), field(audio), field(extraAudio)))
        val model = CaptionTranslationModel(clazz("LTrack;"), dr, tr, targetCode, targetCode)
        val metadataAbi = CaptionMetadataAbi(metadata, targets, targetCode, originalDefault)
        return resolveCaptionEligibleSourceAbi(model, metadataAbi, manager)
    }

    @Test fun `resolves native audio and general source lists by flow not field name`() {
        val resolved = build()
        assertNotNull(resolved)
        assertEquals(audio, resolved!!.audio)
        assertEquals(audioIndices, resolved.audioCaptionIndices)
        assertEquals(general, resolved.generalSourceIndices)
        assertEquals(originals, resolved.originals)
        assertEquals(copy.name, resolved.convertOriginal.name)
    }

    @Test fun `fails closed for ambiguous or missing native relationships`() {
        assertNull(build(ambiguousGeneral = true))
        assertNull(build(ambiguousAudio = true))
        assertNull(build(missingDirectConverter = true))
        assertNull(build(missingAudioIndices = true))
    }
}
