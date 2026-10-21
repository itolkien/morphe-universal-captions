/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3566
 * https://github.com/MorpheApp/morphe-patches/pull/3837
 * https://github.com/MorpheApp/morphe-patches/pull/3891
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.youtube.layout.captions

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.misc.extension.sharedExtensionPatch
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS =
    "Lapp/morphe/extension/youtube/patches/TranslatedCaptionPensPatch;"
private const val EXTENSION_CAPTION_REQUEST_INTERFACE =
    $$"Lapp/morphe/extension/youtube/patches/TranslatedCaptionPensPatch$CaptionRequestInterface;"

/** Wrap the decoder call, not request dispatch, so cached and nested parses have separate scopes. */
private fun MutableMethod.scopeDecoder(invoke: Int, request: String) {
    val impl = implementation ?: error("Missing caption parser bytecode")
    val instructions = impl.instructions.toList()
    check(instructions.getOrNull(invoke + 1)?.opcode == Opcode.MOVE_RESULT_OBJECT) {
        "Unexpected caption parser invocation"
    }

    var address = 0
    val indices = mutableMapOf<Int, Int>()
    instructions.forEachIndexed { index, instruction ->
        indices[address] = index
        address += instruction.codeUnits
    }
    val start = indices.entries.single { it.value == invoke }.key
    val catches = impl.tryBlocks.filter {
        start >= it.startCodeAddress && start < it.startCodeAddress + it.codeUnitCount
    }.flatMap { it.exceptionHandlers }.map {
        indices[it.handlerCodeAddress] ?: error("Missing caption exception handler")
    }.distinct()
    check(catches.isNotEmpty() && catches.all { instructions[it].opcode == Opcode.MOVE_EXCEPTION }) {
        "Caption parser exception cleanup cannot be guaranteed"
    }
    val edits = catches.map { (it + 1) to
            "invoke-static { $request }, $EXTENSION_CLASS->endCaption(Ljava/lang/Object;)V" } +
        listOf(
            (invoke + 2) to "invoke-static { $request }, $EXTENSION_CLASS->endCaption(Ljava/lang/Object;)V",
            invoke to "invoke-static { $request }, $EXTENSION_CLASS->beginCaption(Ljava/lang/Object;)V"
        )
    edits.distinctBy { it.first }.sortedByDescending { it.first }.forEach { (index, smali) ->
        addInstructions(index, smali)
    }
}


/**
 * Auto-translated captions keep the pen (text style) ids of the original track, but the translated
 * text no longer lines up with the original segments. Channels that style their captions
 * (such as using an invisible pen for padding characters) can end up with the entire translated
 * line drawn with an invisible or black pen, so only the caption background is shown.
 *
 * Ignore all pens of auto-translated tracks so the translation uses the user caption style.
 */
internal val translatedCaptionPensPatch = bytecodePatch(
    description = "Fixes auto-translated captions of styled caption tracks showing an empty background.",
) {
    // Adds the caption track interface.
    dependsOn(sharedExtensionPatch, preferredCaptionLanguagePatch)

    execute {
        val cache = CaptionFileParserFingerprint
        val network = RequestResponseParserFingerprint
        val trackField = cache.instructionMatches[1].instruction.getReference<FieldReference>()!!
        val requestClass = mutableClassDefBy(trackField.definingClass)
        requestClass.interfaces.add(EXTENSION_CAPTION_REQUEST_INTERFACE)
        requestClass.addHelperMethod(
            "patch_getCaptionTrack", "Ljava/lang/Object;",
            smali = """
                iget-object v0, p0, $trackField
                return-object v0
            """
        )

        val callbackField = network.instructionMatches[2].instruction.getReference<FieldReference>()!!
        val callbackClass = mutableClassDefBy(callbackField.definingClass)
        callbackClass.interfaces.add(EXTENSION_CAPTION_REQUEST_INTERFACE)
        callbackClass.addHelperMethod(
            "patch_getCaptionTrack", "Ljava/lang/Object;", freeRegisters = 2,
            smali = """
                iget-object v0, p0, $callbackField
                instance-of v1, v0, ${requestClass.type}
                if-eqz v1, :unknown
                check-cast v0, ${requestClass.type}
                invoke-virtual { v0 }, ${requestClass.type}->patch_getCaptionTrack()Ljava/lang/Object;
                move-result-object v0
                return-object v0
                :unknown
                const/4 v0, 0x0
                return-object v0
            """
        )

        // Request identity is visible in both the network callback and cached parser.
        // The try-handler cleanup protects native exceptions; the Java scope checks
        // the request identity if an earlier native catch runs before beginCaption.
        network.method.scopeDecoder(network.instructionMatches[0].index, "p0")
        cache.method.scopeDecoder(cache.instructionMatches[4].index, "p1")

        TimedTextSegmentElementFingerprint.instructionMatches[2].getMethodCalled().addInstructions(
            0,
            """
                invoke-static { p1 }, $EXTENSION_CLASS->getPenId(I)I
                move-result p1
            """
        )
    }
}
