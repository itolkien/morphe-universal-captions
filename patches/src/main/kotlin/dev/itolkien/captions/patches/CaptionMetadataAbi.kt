package dev.itolkien.captions.patches

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.*
import com.android.tools.smali.dexlib2.iface.reference.*

internal data class CaptionMetadataAbi(
    val metadata: FieldReference,
    val targets: FieldReference,
    val targetCode: FieldReference,
    val defaultSource: Method,
)

/** Follow the target-language builder argument back to its protobuf entry and outer list. */
internal fun resolveCaptionMetadataAbi(model: CaptionTranslationModel, abi: CaptionTranslationAbi,
                                      manager: ClassDef): CaptionMetadataAbi {
    val code = model.translated.implementation!!.instructions.toList()
    fun Instruction.ref() = (this as? ReferenceInstruction)?.reference
    fun <T> List<T>.one(role: String): T {
        check(size == 1) { "Caption metadata: $role expected once, found $size" }
        return single()
    }
    val setter = code.indices.filter { code[it].ref() == abi.languageSetter }.one("target language setter")
    val invoke = code[setter]
    val register = when (invoke) {
        is FiveRegisterInstruction -> invoke.registerD
        is RegisterRangeInstruction -> invoke.startRegister + 1
        else -> error("Caption metadata: unsupported language setter")
    }
    val fieldIndex = (setter - 1 downTo 0).firstOrNull {
        code[it].opcode == Opcode.IGET_OBJECT && (code[it] as TwoRegisterInstruction).registerA == register
    } ?: error("Caption metadata: target code read missing")
    val targetCode = code[fieldIndex].ref() as FieldReference
    check(targetCode.type == "Ljava/lang/String;" && targetCode.definingClass != model.track.type)
    val cast = (0 until fieldIndex).filter { code[it].opcode == Opcode.CHECK_CAST &&
            (code[it].ref() as? TypeReference)?.type == targetCode.definingClass }.one("target entry cast")
    val iterator = (cast - 1 downTo 0).firstOrNull {
        val ref = code[it].ref() as? MethodReference
        ref?.name == "iterator" && ref.returnType == "Ljava/util/Iterator;"
    } ?: error("Caption metadata: target iterator missing")
    val iteratorRegister = when (val instruction = code[iterator]) {
        is FiveRegisterInstruction -> instruction.registerC
        is RegisterRangeInstruction -> instruction.startRegister
        else -> error("Caption metadata: unsupported iterator")
    }
    val listIndex = (iterator - 1 downTo 0).firstOrNull { code[it].opcode == Opcode.IGET_OBJECT &&
            (code[it] as TwoRegisterInstruction).registerA == iteratorRegister }
        ?: error("Caption metadata: target list missing")
    val targets = code[listIndex].ref() as FieldReference
    val metadata = manager.fields.filter { it.type == targets.definingClass && !AccessFlags.STATIC.isSet(it.accessFlags) }
        .map { it as FieldReference }.one("manager metadata field")
    val defaultSource = manager.methods.filter { it.returnType == model.track.type && it.parameterTypes.isEmpty() &&
            !AccessFlags.STATIC.isSet(it.accessFlags) && AccessFlags.PUBLIC.isSet(it.accessFlags) }.one("native default source")
    check(code.take(listIndex).any { it.opcode == Opcode.IGET_OBJECT && it.ref() == metadata })
    return CaptionMetadataAbi(metadata, targets, targetCode, defaultSource)
}
