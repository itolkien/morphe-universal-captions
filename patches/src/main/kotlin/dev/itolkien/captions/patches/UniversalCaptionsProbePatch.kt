package dev.itolkien.captions.patches

import app.morphe.patcher.patch.resourcePatch
import java.io.File

/**
 * Integration probe: proves this add-on can apply a resource patch to YouTube.
 * Does NOT unlock missing languages. Deliberately opt-in.
 */
@Suppress("unused")
val universalCaptionsProbePatch = resourcePatch(
    name = "Universal Captions - integration probe (no language unlock)",
    description = "Experimental add-on packaging and resource injection test. Does not modify caption selection.",
    default = false,
) {
    execute {
        val directory = get("res/raw").apply { mkdirs() }
        File(directory, "universal_captions_probe.txt").writeText(
            "Morphe Universal Captions probe; no translation behavior changed.\n"
        )
    }
}
