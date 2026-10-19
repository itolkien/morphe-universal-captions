/*
 * Styled translated-caption hooks adapted from Morphe PR #3837.
 * Copyright 2026 Morphe and itolkien. See NOTICE for GPLv3 Section 7 terms.
 */
package dev.itolkien.captions.patches

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val PEN_STYLES = "Ldev/itolkien/captions/extension/CaptionPenStyles;"

/** The Cronet timedtext request builder, identified by its native call sequence. */
private object CaptionTimedTextRequestFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "L",
    parameters = listOf("L"),
    filters = listOf(
        methodCall(
            smali = $$"Lorg/chromium/net/CronetEngine;->newUrlRequestBuilder(Ljava/lang/String;Lorg/chromium/net/UrlRequest$Callback;Ljava/util/concurrent/Executor;)Lorg/chromium/net/UrlRequest$Builder;"
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        methodCall(
            smali = "Lorg/chromium/net/UploadDataProviders;->create(Ljava/nio/ByteBuffer;)Lorg/chromium/net/UploadDataProvider;"
        )
    )
)

/** The srv3 segment handler points to the shared (int penId) pen lookup. */
private object CaptionSegmentPenFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/util/Deque;", "Lorg/xml/sax/Attributes;", "Ljava/lang/String;"),
    filters = listOf(
        string("p"),
        methodCall(
            smali = "Lorg/xml/sax/Attributes;->getValue(Ljava/lang/String;)Ljava/lang/String;",
            location = MatchAfterImmediately()
        ),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            parameters = listOf("I"),
            returnType = "L",
            location = MatchAfterWithin(5)
        ),
        string("t"),
        string("ac"),
        string("<br/>")
    )
)

/**
 * Resolve both independent host hooks before mutating any method.
 * This implementation lives in Universal Captions, not in Morphe's Captions patch.
 */
internal fun resolveCaptionPenHooks(): () -> Unit {
    val request = CaptionTimedTextRequestFingerprint
    val index = request.instructionMatches[0].index
    val urlRegister = request.method.getInstruction<FiveRegisterInstruction>(index).registerD
    val requestMethod = request.method
    val penLookup = CaptionSegmentPenFingerprint.instructionMatches[2].getMethodCalled()
    check(penLookup.parameterTypes.toList() == listOf("I") &&
        !AccessFlags.STATIC.isSet(penLookup.accessFlags) &&
        penLookup.implementation != null) { "Unexpected srv3 pen lookup signature" }

    return {
        requestMethod.addInstruction(
            index,
            "invoke-static { v$urlRegister }, $PEN_STYLES->setTimedTextUrl(Ljava/lang/String;)V"
        )
        penLookup.addInstructions(
            0,
            """
                invoke-static { p1 }, $PEN_STYLES->getPenId(I)I
                move-result p1
            """
        )
    }
}
