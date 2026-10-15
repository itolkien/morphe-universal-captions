package dev.itolkien.captions.patches

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.*

internal data class CaptionTranslationModel(
    val track: ClassDef,
    val translated: Method,
    val language: Reference,
    val vss: Reference,
)

private fun Method.code() = implementation?.instructions?.toList().orEmpty()
private fun Method.hasText(text: String) = code().any {
    ((it as? ReferenceInstruction)?.reference as? StringReference)?.string == text
}
private fun <T> Iterable<T>.unique(role: String): T = toList().let {
    check(it.size == 1) { "Universal Captions: $role expected once, found ${it.size}: $it" }
    it.single()
}

/** Resolve only the native translation model. Never identify or intercept caption policy methods. */
internal fun resolveCaptionTranslationModel(classes: Iterable<ClassDef>): CaptionTranslationModel {
    val all = classes.distinctBy { it.type }
    fun Method.booleanWith(text: String) = returnType == "Z" && parameterTypes.isEmpty() && hasText(text)
    val track = all.filter { type -> type.methods.any { it.booleanWith("DISABLE_CAPTIONS_OPTION") } }
        .unique("caption track model")
    val languageMethod = track.methods.filter { it.booleanWith("DISABLE_CAPTIONS_OPTION") }
        .unique("language sentinel")
    val vssMethod = track.methods.filter { method ->
        method.booleanWith("t") && method.code().any {
            val ref = (it as? ReferenceInstruction)?.reference as? MethodReference
            ref?.definingClass == "Ljava/lang/String;" && ref.name == "startsWith"
        }
    }.unique("translated track identity")

    fun accessor(method: Method, role: String): Reference = method.code().mapNotNull { instruction ->
        val ref = (instruction as? ReferenceInstruction)?.reference
        when {
            instruction.opcode == Opcode.IGET_OBJECT && ref is FieldReference &&
                    ref.definingClass == track.type && ref.type == "Ljava/lang/String;" -> ref
            instruction.opcode in setOf(Opcode.INVOKE_VIRTUAL, Opcode.INVOKE_VIRTUAL_RANGE) &&
                    ref is MethodReference && ref.definingClass == track.type &&
                    ref.parameterTypes.isEmpty() && ref.returnType == "Ljava/lang/String;" -> ref
            else -> null
        }
    }.distinct().unique(role)

    fun Method.listWith(text: String) = returnType == "Ljava/util/List;" && parameterTypes.isEmpty() && hasText(text)
    val model = all.filter { type -> type.methods.any { it.listWith("AUTO_TRANSLATE_CAPTIONS_OPTION") } }
        .filter { type -> type.methods.any { it.listWith("&tlang=") } }.unique("caption list model")
    val translated = model.methods.filter { it.listWith("&tlang=") }.unique("translation list")
    return CaptionTranslationModel(track, translated, accessor(languageMethod, "language accessor"),
        accessor(vssMethod, "vss accessor"))
}
