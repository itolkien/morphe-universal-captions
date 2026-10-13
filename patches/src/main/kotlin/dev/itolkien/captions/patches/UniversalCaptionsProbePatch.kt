package dev.itolkien.captions.patches

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

private const val BRIDGE = "Ldev/itolkien/captions/extension/NativeTranslationBridge;"
private const val STRING = "Ljava/lang/String;"
private const val OBJECT = "Ljava/lang/Object;"
private fun Instruction.field() = (this as? ReferenceInstruction)?.reference as? FieldReference
private fun Instruction.text() = ((this as? ReferenceInstruction)?.reference as? StringReference)?.string
private fun Method.code() = implementation?.instructions?.toList() ?: emptyList()
private fun Method.hasText(s: String) = code().any { it.text() == s }
private fun FieldReference.id() = "$definingClass->$name:$type"
private fun MethodReference.id() = "$definingClass->$name(${parameterTypes.joinToString("")})$returnType"
private fun <T> Iterable<T>.unique(role: String): T {
    val values = toList()
    if (values.size != 1) throw PatchException("Universal Captions: $role expected once, found ${values.size}")
    return values.single()
}

/** Forked minimal native track clone technique from itolkien/morphe-ai-caption-translator. */
@Suppress("unused")
val universalCaptionsPatch = bytecodePatch(
    name = "Universal Captions - unlock translation languages (experimental)",
    description = "Adds YouTube-native auto-translation targets to Android captions menu. No AI or external translation API.",
    default = false,
) {
    extendWith("extensions/extension.mpe")
    execute {
        val track = getAllClassesWithString("AUTO_TRANSLATE_CAPTIONS_OPTION").map { classDefBy(it.type) }
            .filter { "Landroid/os/Parcelable;" in it.interfaces }.unique("caption model")
        val sentinel = track.methods.filter { it.returnType == "Z" && it.hasText("AUTO_TRANSLATE_CAPTIONS_OPTION") }
            .unique("translation sentinel")
        val language = sentinel.code().mapNotNull { it.field() }
            .filter { it.definingClass == track.type && it.type == STRING }.distinctBy { it.id() }
            .unique("language field")
        val vss = track.methods.filter { it.returnType == "Z" && it.hasText("t") }
            .flatMap { it.code().mapNotNull { ins -> ins.field() } }
            .filter { it.type == STRING }.distinctBy { it.id() }.unique("vss field")
        val display = track.methods.filter { it.name == "toString" }
            .flatMap { it.code().mapNotNull { ins -> ins.field() } }
            .filter { it.type == "Ljava/lang/CharSequence;" }.unique("display field")
        val listMethod = getAllClassesWithString("&tlang=")
            .flatMap { classDefBy(it.type).methods.toList() }
            .filter { it.hasText("&tlang=") && it.parameterTypes.isEmpty() &&
                it.returnType == "Ljava/util/List;" }.unique("translated track list")
        val listCode = listMethod.code()
        val url = listCode.mapIndexedNotNull { i, ins ->
            if (ins.text() == "&tlang=") listCode.getOrNull(i - 2)?.field() else null
        }.filter { it.definingClass == track.type && it.type == STRING }.unique("signed caption URL")
        val builderFactory = track.methods.filter {
            AccessFlags.STATIC.isSet(it.accessFlags) && it.parameterTypes.isEmpty()
                && it.returnType.startsWith("L") && it.returnType != track.type
        }.unique("track builder factory")
        val builder = classDefBy(builderFactory.returnType)
        val copy = builder.methods.filter { it.name == "<init>" &&
            it.parameterTypes.toList() == listOf(track.type) }.unique("track copy constructor")
        fun builderField(field: FieldReference): FieldReference {
            val instructions = copy.code()
            return instructions.mapIndexedNotNull { i, ins ->
                if (ins.field()?.id() == field.id()) instructions.getOrNull(i + 1)?.field() else null
            }.filter { it.definingClass == builder.type }.unique("builder field ${field.name}")
        }
        fun setter(field: FieldReference): Method {
            val mapped = builderField(field)
            return builder.methods.filter { it.parameterTypes.toList() == listOf(STRING)
                && it.returnType == "V" && it.code().any { ins ->
                    ins.opcode == Opcode.IPUT_OBJECT && ins.field()?.id() == mapped.id()
                } }.unique("builder setter ${field.name}")
        }
        val languageSetter = setter(language)
        val urlSetter = setter(url)
        val vssSetter = setter(vss)
        val builderDisplay = builderField(display)
        val build = builder.methods.filter { it.parameterTypes.isEmpty() &&
            it.returnType == track.type }.unique("build caption track")
        for (m in listOf(copy, languageSetter, urlSetter, vssSetter, build))
            if (!AccessFlags.PUBLIC.isSet(m.accessFlags))
                throw PatchException("Universal Captions: track builder method not public")
        if (!AccessFlags.PUBLIC.isSet(builder.fields.first { it.name == builderDisplay.name }.accessFlags))
            throw PatchException("Universal Captions: builder display field not public")

        val runtime = mutableClassDefBy(BRIDGE)
        fun bind(name: String, body: String) {
            val args = if (name == "cloneTranslation") listOf(OBJECT, STRING) else listOf(OBJECT)
            val stub = runtime.methods.filter { it.name == name &&
                it.parameterTypes.toList() == args }.unique("runtime $name")
            val replacement = ImmutableMethod(BRIDGE, name, stub.parameters, stub.returnType,
                stub.accessFlags, stub.annotations, null, MutableMethodImplementation(3 + args.size)).toMutable()
            replacement.addInstructionsWithLabels(0, body.trimIndent())
            runtime.methods.remove(stub)
            runtime.methods.add(replacement)
        }
        bind("language", """
            check-cast p0, ${track.type}
            iget-object v0, p0, ${language.id()}
            return-object v0
        """)
        bind("vss", """
            check-cast p0, ${track.type}
            iget-object v0, p0, ${vss.id()}
            return-object v0
        """)
        bind("url", """
            check-cast p0, ${track.type}
            iget-object v0, p0, ${url.id()}
            return-object v0
        """)
        bind("cloneTranslation", """
            check-cast p0, ${track.type}
            new-instance v0, ${builder.type}
            invoke-direct {v0, p0}, ${copy.id()}
            invoke-virtual {v0, p1}, ${languageSetter.id()}
            invoke-static {p1}, $BRIDGE->translatedName($STRING)Ljava/lang/CharSequence;
            move-result-object v1
            iput-object v1, v0, ${builderDisplay.id()}
            iget-object v1, p0, ${url.id()}
            invoke-static {v1, p1}, $BRIDGE->translatedUrl($STRING$STRING)$STRING
            move-result-object v1
            invoke-virtual {v0, v1}, ${urlSetter.id()}
            iget-object v1, p0, ${vss.id()}
            invoke-static {v1, p1}, $BRIDGE->translatedVss($STRING$STRING)$STRING
            move-result-object v1
            invoke-virtual {v0, v1}, ${vssSetter.id()}
            invoke-virtual {v0}, ${build.id()}
            move-result-object v0
            return-object v0
        """)

        val method = mutableClassDefBy(listMethod.definingClass).methods
            .filter { it.name == listMethod.name &&
                it.parameterTypes.toList() == listMethod.parameterTypes.toList() &&
                it.returnType == listMethod.returnType }.unique("mutable translated list")
        val returns = method.implementation!!.instructions.mapIndexedNotNull { i, ins ->
            if (ins.opcode == Opcode.RETURN_OBJECT) i to (ins as OneRegisterInstruction).registerA
            else null
        }
        if (returns.isEmpty()) throw PatchException("Universal Captions: no translated track list returns")
        for ((index, register) in returns.reversed()) {
            // Replacing the return ensures label targets also execute the inserted hook.
            method.replaceInstruction(index,
                "invoke-static/range {v$register .. v$register}, $BRIDGE->augment(Ljava/util/List;)Ljava/util/List;")
            method.addInstructionsWithLabels(index + 1,
                "move-result-object v$register\nreturn-object v$register")
        }
    }
}
