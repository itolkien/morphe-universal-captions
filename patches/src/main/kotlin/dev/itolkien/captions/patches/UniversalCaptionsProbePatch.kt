package dev.itolkien.captions.patches

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

/**
 * Actual YouTube caption-manager interception, not a resource marker.
 * Does not add missing translation targets; observes the native auto-translate list.
 */
@Suppress("unused")
val universalCaptionsProbePatch = bytecodePatch(
    name = "Universal Captions - native track probe (experimental)",
    description = "Hooks YouTube's native auto-translation track list without altering it. Diagnostic only.",
    default = false,
) {
    extendWith("extensions/extension.mpe")
    execute {
        val method = Fingerprint(
            accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
            returnType = "Ljava/util/List;",
            parameters = listOf(),
            filters = listOf(string("&tlang=")),
        ).method
        val returns = method.implementation!!.instructions.withIndex()
            .filter { it.value.opcode == Opcode.RETURN_OBJECT }
        require(returns.isNotEmpty()) { "No native auto-translation list return found" }
        for ((index, instruction) in returns.asReversed()) {
            val register = (instruction as OneRegisterInstruction).registerA
            method.addInstructions(index, """
                invoke-static { v$register }, Ldev/itolkien/captions/extension/NativeTrackProbe;->observe(Ljava/util/List;)Ljava/util/List;
                move-result-object v$register
            """)
        }
    }
}
