package dev.itolkien.captions.patches

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
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
    val candidates = methods.filter { it.definingClass == owner &&
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
    check(matches.size == 1) {
        "Universal Captions: native default caption selector expected one model reader, found ${matches.size}; " +
            "candidates=" + evidence.joinToString { (method, fields) ->
                "${method.descriptor()} [instance=${!AccessFlags.STATIC.isSet(method.accessFlags)}, " +
                    "registers=${method.implementation?.registerCount}, modelFields=" +
                    fields.joinToString(prefix = "[", postfix = "]") { "${it.name}:${it.type}" } + "]"
            } + ". Unsupported or ambiguous bytecode; report the YouTube version and these descriptors."
    }
    val (method, fields) = matches.single()
    return DefaultCaptionSelector(method, fields.single())
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
