package dev.itolkien.captions.patches

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.AppTarget
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val EXTENSION = "Ldev/itolkien/captions/extension/NativeTranslationBridge;"
private const val TRACK_INTERFACE =
    $$"Ldev/itolkien/captions/extension/CaptionTranslationList$CaptionTrack;"
private const val STRING = "Ljava/lang/String;"
private const val LABEL = "Ljava/lang/CharSequence;"

private fun MutableClass.addTranslationHelper(
    name: String, parameters: List<String>, returnType: String, locals: Int, body: String
) {
    methods.add(ImmutableMethod(type, name,
        parameters.map { ImmutableMethodParameter(it, emptySet(), null) }, returnType,
        AccessFlags.PUBLIC.value or AccessFlags.FINAL.value, null, null,
        MutableMethodImplementation(locals + parameters.size + 1)).toMutable().apply {
        addInstructions(0, body)
    })
}

private fun readSmali(reference: Reference): String = when (reference) {
    is FieldReference -> "iget-object v0, p0, $reference"
    is MethodReference -> """
        invoke-virtual { p0 }, $reference
        move-result-object v0
    """
    else -> error("Unsupported caption accessor: $reference")
}

@Suppress("unused")
val universalCaptionsPatch = bytecodePatch(
    name = "Universal Captions - additional translation languages",
    description = "Adds and alphabetizes native Auto-translate languages. Caption preferences remain managed by Morphe.",
    default = false,
) {
    compatibleWith(Compatibility(
        name = "YouTube", packageName = "com.google.android.youtube",
        targets = listOf(AppTarget(version = "21.20.405"), AppTarget(version = "21.41.525"))
    ))
    extendWith("extensions/extension.mpe")
    execute {
        val candidates = (getAllClassesWithString("DISABLE_CAPTIONS_OPTION") +
            getAllClassesWithString("&tlang=")).map { classDefBy(it.type) }
        val model = resolveCaptionTranslationModel(candidates)
        val abi = resolveCaptionTranslationAbi(model.track, model.translated,
            model.language, model.vss, ::classDefByOrNull)
        val track = mutableClassDefBy(model.track.type)
        val translated = mutableClassDefBy(model.translated.definingClass).methods.single {
            it.name == model.translated.name && it.parameterTypes == model.translated.parameterTypes &&
                it.returnType == model.translated.returnType
        }
        val returns = translated.implementation!!.instructions.mapIndexedNotNull { index, instruction ->
            if (instruction.opcode == Opcode.RETURN_OBJECT)
                index to (instruction as OneRegisterInstruction).registerA else null
        }
        check(returns.isNotEmpty()) { "Caption translations: no native list returns" }

        val mutableBuilder = mutableClassDefBy(abi.builder.type)
        mutableBuilder.addTranslationHelper("universalCaptions_setCaptionLabel", listOf(LABEL), "V", 0, """
            iput-object p1, p0, ${abi.labelField}
            return-void
        """)

        // Place field access in its owning class, including for private fields and old abstract getters.
        track.interfaces.add(TRACK_INTERFACE)
        track.addTranslationHelper("universalCaptions_getLanguageCode", emptyList(), STRING, 1, """
            ${readSmali(model.language)}
            return-object v0
        """)
        track.addTranslationHelper("universalCaptions_getVssId", emptyList(), STRING, 1, """
            ${readSmali(model.vss)}
            return-object v0
        """)
        track.addTranslationHelper("universalCaptions_getCaptionUrl", emptyList(), STRING, 1, """
            ${readSmali(abi.url)}
            return-object v0
        """)
        track.addTranslationHelper("universalCaptions_getCaptionLabel", emptyList(), LABEL, 1, """
            ${readSmali(abi.label)}
            return-object v0
        """)
        track.addTranslationHelper("universalCaptions_copyTranslation", listOf(STRING, STRING, STRING, LABEL), TRACK_INTERFACE, 1, """
            new-instance v0, ${abi.builder.type}
            invoke-direct { v0, p0 }, ${abi.copy}
            invoke-virtual { v0, p1 }, ${abi.languageSetter}
            invoke-virtual { v0, p2 }, ${abi.urlSetter}
            invoke-virtual { v0, p3 }, ${abi.vssSetter}
            invoke-virtual { v0, p4 }, ${abi.builder.type}->universalCaptions_setCaptionLabel($LABEL)V
            invoke-virtual { v0 }, ${abi.build}
            move-result-object v0
            return-object v0
        """)

        for ((index, register) in returns.reversed()) {
            // Replace the return so branch targets also pass through augmentation.
            translated.replaceInstruction(index,
                "invoke-static/range { v$register .. v$register }, $EXTENSION->augment(Ljava/util/List;)Ljava/util/List;")
            translated.addInstructions(index + 1, """
                move-result-object v$register
                return-object v$register
            """)
        }
    }
}
