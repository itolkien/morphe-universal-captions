package dev.itolkien.captions.patches

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal fun MethodReference.descriptor() =
    "$definingClass->$name(${parameterTypes.joinToString("")})$returnType"

internal data class DefaultCaptionSelector(val method: Method, val modelField: FieldReference)

/** Resolve by model ownership, never by method count, obfuscated name or iteration order. */
internal fun resolveDefaultCaptionSelector(methods: Iterable<Method>, owner: String,
                                          track: String, model: String): DefaultCaptionSelector {
    val allMethods = methods.toList()
    val candidates = allMethods.filter { it.definingClass == owner &&
        it.parameterTypes.isEmpty() && it.returnType == track }
    val evidence = candidates.map { method ->
        val implementation = method.implementation
        // For a no-arg instance method the final register is this (p0).
        val receiver = (implementation?.registerCount ?: 0) - 1
        val fields = implementation?.instructions?.toList().orEmpty().mapNotNull { instruction ->
            val field = (instruction as? ReferenceInstruction)?.reference as? FieldReference
            if (instruction.opcode == Opcode.IGET_OBJECT &&
                (instruction as? TwoRegisterInstruction)?.registerB == receiver &&
                field?.definingClass == owner && field.type == model) field else null
        }.distinctBy { "${it.definingClass}->${it.name}:${it.type}" }
        method to fields
    }
    val matches = evidence.filter { (method, fields) ->
        !AccessFlags.STATIC.isSet(method.accessFlags) && fields.size == 1 &&
            (method.implementation?.registerCount ?: 0) >= 2 &&
            method.implementation?.instructions?.any { it.opcode == Opcode.RETURN_OBJECT } == true
    }
    // Newer hosts split default calculation from a model-reading commit wrapper.
    // Require independent lifecycle + Android caption-preference evidence to distinguish them.
    val resolved = if (matches.size <= 1) matches else matches.filter { (helper, fields) ->
        helper.usesNativeCaptionPreferences() &&
            matches.filter { it.first != helper }.all { (wrapper, wrapperFields) ->
                wrapperFields.single() == fields.single() && wrapper.callsOnThis(helper) &&
                    !helper.callsOnThis(wrapper)
            } && allMethods.any { caller ->
                caller !in matches.map { it.first } && caller.initializesModelThenCalls(fields.single(), helper)
            }
    }
    check(resolved.size == 1) {
        "Universal Captions: native default caption selector expected one model reader, found ${matches.size}; " +
            "candidates=" + evidence.joinToString { (method, fields) ->
                "${method.descriptor()} [instance=${!AccessFlags.STATIC.isSet(method.accessFlags)}, " +
                    "registers=${method.implementation?.registerCount}, modelFields=" +
                    fields.joinToString(prefix = "[", postfix = "]") { "${it.name}:${it.type}" } +
                    ", nativePreferences=${method.usesNativeCaptionPreferences()}]"
            } + ". Unsupported or ambiguous bytecode; report the YouTube version and these descriptors."
    }
    val (method, fields) = resolved.single()
    return DefaultCaptionSelector(method, fields.single())
}

private fun Method.thisRegister(): Int? {
    if (AccessFlags.STATIC.isSet(accessFlags)) return null
    val registers = implementation?.registerCount ?: return null
    val argumentWords = parameterTypes.sumOf { if (it.toString() in listOf("J", "D")) 2 else 1 }
    return registers - argumentWords - 1
}

private fun Instruction.callsOnReceiver(target: Method, receiver: Int): Boolean {
    if (opcode !in setOf(Opcode.INVOKE_DIRECT, Opcode.INVOKE_VIRTUAL,
            Opcode.INVOKE_DIRECT_RANGE, Opcode.INVOKE_VIRTUAL_RANGE)) return false
    val reference = (this as? ReferenceInstruction)?.reference as? MethodReference ?: return false
    if (reference.descriptor() != target.descriptor()) return false
    return when (this) {
        is FiveRegisterInstruction -> registerCount == 1 && registerC == receiver
        is RegisterRangeInstruction -> registerCount == 1 && startRegister == receiver
        else -> false
    }
}

private fun Method.callsOnThis(target: Method): Boolean {
    val receiver = thisRegister() ?: return false
    return implementation!!.instructions.any { it.callsOnReceiver(target, receiver) }
}

private fun Method.initializesModelThenCalls(field: FieldReference, target: Method): Boolean {
    val receiver = thisRegister() ?: return false
    val instructions = implementation!!.instructions.toList()
    val write = instructions.indexOfFirst {
        it.opcode == Opcode.IPUT_OBJECT && (it as? TwoRegisterInstruction)?.registerB == receiver &&
            (it as? ReferenceInstruction)?.reference == field
    }
    return write >= 0 && instructions.drop(write + 1).any { it.callsOnReceiver(target, receiver) }
}

private fun Method.usesNativeCaptionPreferences(): Boolean {
    val calls = implementation?.instructions?.mapNotNull {
        if (it.opcode == Opcode.INVOKE_VIRTUAL || it.opcode == Opcode.INVOKE_VIRTUAL_RANGE)
            (it as? ReferenceInstruction)?.reference as? MethodReference else null
    }.orEmpty()
    val captioningManager = "Landroid/view/accessibility/CaptioningManager;"
    return calls.any { it.definingClass == captioningManager && it.name == "getLocale" &&
        it.parameterTypes.isEmpty() && it.returnType == "Ljava/util/Locale;" } &&
        calls.any { it.definingClass == captioningManager && it.name == "isEnabled" &&
            it.parameterTypes.isEmpty() && it.returnType == "Z" }
}

/** The direct-list string anchor identifies the source getter even if new list getters appear. */
internal fun resolveSourceCaptionGetter(methods: Iterable<Method>, model: String,
                                       directAnchor: Method, translated: Method): Method {
    val candidates = methods.filter { it.definingClass == model &&
        it.descriptor() == directAnchor.descriptor() &&
        it.descriptor() != translated.descriptor() &&
        it.parameterTypes.isEmpty() && it.returnType == "Ljava/util/List;" &&
        AccessFlags.PUBLIC.isSet(it.accessFlags) && !AccessFlags.STATIC.isSet(it.accessFlags) &&
        it.implementation != null }
    check(directAnchor.definingClass == model && translated.definingClass == model && candidates.size == 1) {
        "Universal Captions: native source caption getter does not resolve uniquely on $model; " +
            "direct=${directAnchor.descriptor()}, translated=${translated.descriptor()}, " +
            "matches=${candidates.size}. Unsupported bytecode; report these descriptors."
    }
    return candidates.single()
}
