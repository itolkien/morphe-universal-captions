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
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation

private const val EXTENSION = "Ldev/itolkien/captions/extension/NativeTranslationBridge;"
private const val TRACK_INTERFACE =
    $$"Ldev/itolkien/captions/extension/CaptionTranslationList$CaptionTrack;"
private const val SOURCE_INTERFACE =
    $$"Ldev/itolkien/captions/extension/EmptyTranslationFallback$CaptionSource;"
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
    description = "Restores missing Auto-translate menus and adds YouTube translation languages from original captions. Caption preferences remain managed by Morphe.",
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
        val autoOption = resolveNativeAutoOption(model, abi)
        val metadata = resolveCaptionMetadataAbi(model, abi, classDefBy(model.direct.definingClass))
        for (method in listOf(model.direct, model.translated)) {
            check(method.implementation!!.registerCount in 1..15 && method.parameterTypes.isEmpty() &&
                !AccessFlags.STATIC.isSet(method.accessFlags)) { "Unsupported native menu register layout" }
        }
        val manager = mutableClassDefBy(model.translated.definingClass)
        val direct = manager.methods.single {
            it.name == model.direct.name && it.parameterTypes.isEmpty() && it.returnType == "Ljava/util/List;"
        }
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

        manager.addTranslationHelper("universalCaptions_targetLanguages", listOf("Z"), "Ljava/util/List;", 4, """
            new-instance v0, Ljava/util/ArrayList;
            invoke-direct { v0 }, Ljava/util/ArrayList;-><init>()V
            iget-object v1, p0, ${metadata.metadata}
            if-eqz v1, :done
            iget-object v1, v1, ${metadata.targets}
            if-eqz v1, :done
            invoke-interface { v1 }, Ljava/util/List;->iterator()Ljava/util/Iterator;
            move-result-object v1
            :next
            invoke-interface { v1 }, Ljava/util/Iterator;->hasNext()Z
            move-result v2
            if-eqz v2, :done
            invoke-interface { v1 }, Ljava/util/Iterator;->next()Ljava/lang/Object;
            move-result-object v2
            check-cast v2, ${metadata.targetCode.definingClass}
            if-eqz v2, :next
            iget-object v3, v2, ${metadata.targetCode}
            invoke-interface { v0, v3 }, Ljava/util/List;->add(Ljava/lang/Object;)Z
            goto :next
            :done
            return-object v0
        """)
        manager.addTranslationHelper("universalCaptions_defaultSource", listOf("Z"), TRACK_INTERFACE, 1, """
            invoke-virtual { p0 }, ${metadata.defaultSource}
            move-result-object v0
            return-object v0
        """)
        manager.interfaces.add(SOURCE_INTERFACE)
        manager.methods.add(ImmutableMethod(manager.type, "universalCaptions_autoTranslateOption",
            emptyList(), TRACK_INTERFACE, AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
            null, null, autoOption).toMutable())
        for ((original, rawName, hook) in listOf(
                Triple(direct, "universalCaptions_nativeDirect", "fallbackDirect"),
                Triple(translated, "universalCaptions_nativeTranslated", "fallbackTranslated"))) {
            val implementation = original.implementation!!
            val registers = implementation.registerCount
            check(registers in 1..15 && original.parameterTypes.isEmpty() &&
                !AccessFlags.STATIC.isSet(original.accessFlags)) { "Unsupported native menu register layout" }
            val snapshot = ImmutableMethodImplementation.of(implementation)!!
            // An extra unused boolean keeps raw copies out of Morphe's parameterless fingerprints.
            // With R+1 registers and two incoming words, raw p0 remains at the original R-1.
            val raw = ImmutableMethod(manager.type, rawName,
                listOf(ImmutableMethodParameter("Z", emptySet(), null)), original.returnType,
                original.accessFlags, original.annotations, original.hiddenApiRestrictions,
                ImmutableMethodImplementation(registers + 1, snapshot.instructions,
                    snapshot.tryBlocks, snapshot.debugItems)).toMutable()
            val hooked = ImmutableMethod(manager.type, original.name, original.parameters,
                original.returnType, original.accessFlags, original.annotations, original.hiddenApiRestrictions,
                ImmutableMethodImplementation(registers + 1, snapshot.instructions,
                    snapshot.tryBlocks, snapshot.debugItems)).toMutable()
            val hookedReturns = hooked.implementation!!.instructions.mapIndexedNotNull { index, instruction ->
                if (instruction.opcode == Opcode.RETURN_OBJECT)
                    index to (instruction as OneRegisterInstruction).registerA else null
            }
            for ((index, register) in hookedReturns.reversed()) {
                hooked.replaceInstruction(index,
                    "invoke-static { v$register, p0 }, $EXTENSION->$hook(Ljava/util/List;$SOURCE_INTERFACE)Ljava/util/List;")
                hooked.addInstructions(index + 1, """
                    move-result-object v$register
                    return-object v$register
                """)
            }
            // Original code still uses the old p0 register, which it is free to overwrite.
            // The new incoming p0 remains untouched and is available on every return path.
            hooked.addInstructions(0, "move-object/from16 v${registers - 1}, p0")
            manager.methods.remove(original)
            manager.methods.add(hooked)
            manager.methods.add(raw)
        }
    }
}
