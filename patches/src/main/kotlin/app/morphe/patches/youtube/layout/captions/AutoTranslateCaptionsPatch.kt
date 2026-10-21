/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3566
 * https://github.com/MorpheApp/morphe-patches/pull/3754
 * Adapted from https://github.com/itolkien/morphe-universal-captions
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.youtube.layout.captions

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import app.morphe.patches.youtube.misc.extension.sharedExtensionPatch
import app.morphe.patches.youtube.misc.settings.settingsPatch
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.matchAllMethodIndicesForEach
import app.morphe.util.registersUsed
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS =
    "Lapp/morphe/extension/youtube/patches/AutoTranslateCaptionsPatch;"
private const val EXTENSION_CAPTION_TRACK_INTERFACE =
    $$"Lapp/morphe/extension/youtube/patches/AutoTranslateCaptionsPatch$CaptionTrackInterface;"
private const val PREFERRED_CAPTION_TRACK_INTERFACE =
    $$"Lapp/morphe/extension/youtube/patches/PreferredCaptionLanguagePatch$CaptionTrackInterface;"
private const val CAPTION_MANAGER_INTERFACE =
    $$"Lapp/morphe/extension/youtube/patches/AutoTranslateCaptionsPatch$CaptionManagerInterface;"


/** Resolve the native translation-target list by its dataflow, not obfuscated field names. */
private data class NativeTargets(
    val metadata: FieldReference,
    val list: FieldReference,
    val code: FieldReference,
)

private fun resolveNativeTargets(
    manager: ClassDef, translated: Method, languageSetter: MethodReference,
): NativeTargets {
    val instructions = translated.implementation!!.instructions.toList()
    fun Instruction.ref() = (this as? ReferenceInstruction)?.reference
    val setterIndex = instructions.indices.filter { instructions[it].ref() == languageSetter }.single()
    val setter = instructions[setterIndex]
    val languageRegister = when (setter) {
        is FiveRegisterInstruction -> setter.registerD
        is RegisterRangeInstruction -> setter.startRegister + 1
        else -> error("Unsupported native translation language setter")
    }
    val codeIndex = (setterIndex - 1 downTo 0).first {
        instructions[it].opcode == Opcode.IGET_OBJECT
                && (instructions[it] as TwoRegisterInstruction).registerA == languageRegister
    }
    val languageField = instructions[codeIndex].ref() as FieldReference
    check(languageField.type == "Ljava/lang/String;")
    val castIndex = (0 until codeIndex).filter {
        instructions[it].opcode == Opcode.CHECK_CAST
                && (instructions[it].ref() as? TypeReference)?.type == languageField.definingClass
    }.single()
    val iteratorIndex = (castIndex - 1 downTo 0).first {
        (instructions[it].ref() as? MethodReference)?.let { method ->
            method.name == "iterator" && method.returnType == "Ljava/util/Iterator;"
        } == true
    }
    val iterator = instructions[iteratorIndex]
    val iteratorRegister = when (iterator) {
        is FiveRegisterInstruction -> iterator.registerC
        is RegisterRangeInstruction -> iterator.startRegister
        else -> error("Unsupported native target iterator")
    }
    val listIndex = (iteratorIndex - 1 downTo 0).first {
        instructions[it].opcode == Opcode.IGET_OBJECT
                && (instructions[it] as TwoRegisterInstruction).registerA == iteratorRegister
    }
    val list = instructions[listIndex].ref() as FieldReference
    val metadata = manager.fields.filter {
        it.type == list.definingClass && !AccessFlags.STATIC.isSet(it.accessFlags)
    }.single() as FieldReference
    check(instructions.take(listIndex).any { it.opcode == Opcode.IGET_OBJECT && it.ref() == metadata })
    return NativeTargets(metadata, list, languageField)
}

internal val autoTranslateCaptionsPatch = bytecodePatch(
    description = "Adds an option to add more auto-translate languages and sort the languages by name."
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        preferredCaptionLanguagePatch
    )

    execute {
        settingsMenuCaptionGroup.add(
            SwitchPreference("morphe_auto_translate_additional_languages", summary = true)
        )

        val labelField = CaptionTrackBuilderBuildFingerprint.instructionMatches[0]
            .instruction.getReference<FieldReference>()!!
        val urlField = CaptionTracksManagerAutoTranslateTracksFingerprint.instructionMatches[0]
            .instruction.getReference<FieldReference>()!!

        val setLanguageCodeMethod = CaptionTrackBuilderSetterFingerprint("Null languageCode").method
        val setUrlMethod = CaptionTrackBuilderSetterFingerprint("Null url").method
        val setVssIdMethod = CaptionTrackBuilderSetterFingerprint("Null vssId").method

        // Add interface and helper methods to allow extension code to call obfuscated code.
        mutableClassDefBy(CaptionTrackBuilderBuildFingerprint.method.returnType).apply {
            interfaces.add(EXTENSION_CAPTION_TRACK_INTERFACE)

            // 20.x tracks are an abstract class, and the url field is in the subclass.
            addHelperMethod(
                "patch_getUrl",
                "Ljava/lang/String;",
                smali = """
                    move-object v0, p0
                    check-cast v0, ${urlField.definingClass}
                    iget-object v0, v0, $urlField
                    return-object v0
                """
            )

            addHelperMethod(
                "patch_copyTranslation",
                EXTENSION_CAPTION_TRACK_INTERFACE,
                listOf(
                    "Ljava/lang/String;",
                    "Ljava/lang/String;",
                    "Ljava/lang/String;",
                    "Ljava/lang/CharSequence;"
                ),
                smali = """
                    new-instance v0, ${CaptionTrackBuilderBuildFingerprint.classDef.type}
                    invoke-direct { v0, p0 }, ${CaptionTrackBuilderCopyConstructorFingerprint.method}
                    invoke-virtual { v0, p1 }, $setLanguageCodeMethod
                    invoke-virtual { v0, p2 }, $setUrlMethod
                    invoke-virtual { v0, p3 }, $setVssIdMethod
                    iput-object p4, v0, $labelField
                    invoke-virtual { v0 }, ${CaptionTrackBuilderBuildFingerprint.method}
                    move-result-object v0
                    return-object v0
                """
            )
        }

        val directTracks = CaptionTracksManagerDirectTracksFingerprint.method
        val autoTracks = CaptionTracksManagerAutoTranslateTracksFingerprint.method
        // Added by the preferred caption language patch.
        val translationSource = "${directTracks.definingClass}->patch_getTranslationSource()" +
                CaptionTrackBuilderBuildFingerprint.method.returnType
        // Resolve YouTube's per-video translationTargets from the same native
        // method used to construct the existing translated-caption list.
        val nativeTargets = resolveNativeTargets(
            CaptionTracksManagerDirectTracksFingerprint.classDef,
            CaptionTracksManagerAutoTranslateTracksFingerprint.method,
            setLanguageCodeMethod,
        )
        val manager = CaptionTracksManagerDirectTracksFingerprint.classDef
        manager.interfaces.add(CAPTION_MANAGER_INTERFACE)
        manager.addHelperMethod(
            "patch_getNativeTargetLanguages", "Ljava/util/List;", freeRegisters = 4,
            smali = """
                new-instance v0, Ljava/util/ArrayList;
                invoke-direct { v0 }, Ljava/util/ArrayList;-><init>()V
                iget-object v1, p0, ${nativeTargets.metadata}
                if-eqz v1, :done
                iget-object v1, v1, ${nativeTargets.list}
                if-eqz v1, :done
                invoke-interface { v1 }, Ljava/util/List;->iterator()Ljava/util/Iterator;
                move-result-object v1
                :next
                invoke-interface { v1 }, Ljava/util/Iterator;->hasNext()Z
                move-result v2
                if-eqz v2, :done
                invoke-interface { v1 }, Ljava/util/Iterator;->next()Ljava/lang/Object;
                move-result-object v2
                check-cast v2, ${nativeTargets.code.definingClass}
                if-eqz v2, :next
                iget-object v3, v2, ${nativeTargets.code}
                invoke-interface { v0, v3 }, Ljava/util/List;->add(Ljava/lang/Object;)Z
                goto :next
                :done
                return-object v0
            """
        )
        manager.addHelperMethod(
            "patch_getActiveTranslationSource", PREFERRED_CAPTION_TRACK_INTERFACE,
            smali = """
                invoke-virtual { p0 }, $translationSource
                move-result-object v0
                return-object v0
            """
        )
        // Do not read volatile native source/catalog fields outside the Java guard.
        manager.addHelperMethod(
            "patch_getAutoTranslateTracks", "Ljava/util/List;", freeRegisters = 2,
            smali = """
                invoke-virtual { p0 }, $autoTracks
                move-result-object v0
                invoke-virtual { p0 }, $directTracks
                move-result-object v1
                invoke-static { v0, v1, p0 }, $EXTENSION_CLASS->restoreTranslationTracks(Ljava/util/List;Ljava/util/List;$CAPTION_MANAGER_INTERFACE)Ljava/util/List;
                move-result-object v0
                return-object v0
            """
        )

        val adapter = "${directTracks.definingClass}->patch_getAutoTranslateTracks()Ljava/util/List;"
        // Route the UI getter and the existing preferred-language interface getter through it.
        AutoTranslateTracksCallFingerprint(autoTracks).matchAllMethodIndicesForEach { index ->
            val register = getInstruction(index).registersUsed[0]
            replaceInstruction(index, "invoke-virtual/range { v$register .. v$register }, $adapter")
        }

        // Keep YouTube's native row factory but check its real target catalog.
        CaptionTracksManagerTranslationCountFingerprint.apply {
            val countMatch = instructionMatches[1]
            val count = countMatch.getInstruction<OneRegisterInstruction>().registerA
            val result = instructionMatches.last().getInstruction<OneRegisterInstruction>().registerA
            val managerRegister = method.getFreeRegisterProvider(
                countMatch.index + 1, 1
            ).getFreeRegister4Bit()
            method.addInstructions(
                countMatch.index + 1,
                """
                    move-object/from16 v$managerRegister, p0
                    invoke-static { v$count, v$result, v$managerRegister }, $EXTENSION_CLASS->getTranslationCount(ILjava/util/List;$CAPTION_MANAGER_INTERFACE)I
                    move-result v$count
                """
            )
        }

    }
}
