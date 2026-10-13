package dev.itolkien.captions.patches

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

/**
 * Observe both caption track sources. The previous build only hooked auto-translate
 * track generation, which is lazy and might never run if Android filters the menu.
 */
@Suppress("unused")
val universalCaptionsProbePatch = bytecodePatch(
    name = "Universal Captions - dual native track probe (experimental)",
    description = "Observes both native caption and translation lists. Diagnostic only; does not unlock languages.",
    default = false,
) {
    extendWith("extensions/extension.mpe")
    execute {
        fun install(needle: String, methodName: String) {
            val method = Fingerprint(
                accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
                returnType = "Ljava/util/List;",
                parameters = listOf(),
                filters = listOf(string(needle)),
            ).method
            val returns = method.implementation!!.instructions.withIndex()
                .filter { it.value.opcode == Opcode.RETURN_OBJECT }
            require(returns.isNotEmpty()) { "No return for native caption list ($needle)" }
            for ((index, instruction) in returns.asReversed()) {
                val register = (instruction as OneRegisterInstruction).registerA
                method.addInstructions(index, """
                    invoke-static { v$register }, Ldev/itolkien/captions/extension/NativeTrackProbe;->$methodName(Ljava/util/List;)Ljava/util/List;
                    move-result-object v$register
                """)
            }
        }

        install("AUTO_TRANSLATE_CAPTIONS_OPTION", "observeDirect")
        install("&tlang=", "observeTranslated")
    }
}
