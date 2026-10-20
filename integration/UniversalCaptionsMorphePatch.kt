/* Copyright 2026 itolkien. GPL-3.0-or-later; see LICENSE and NOTICE. */
package app.morphe.patches.youtube.layout.captions

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.shared.Constants.COMPATIBILITY_YOUTUBE

/**
 * A patch alias, not a second caption implementation.
 * The official Morphe caption hooks and their extension run once via dependsOn.
 */
@Suppress("unused")
val universalCaptionsIntegrationPatch = bytecodePatch(
    name = "Universal Captions (Morphe integration)",
    description = "Enables Morphe captions, translation languages and translated-caption styling together.",
    default = false,
) {
    dependsOn(captionsPatch)
    compatibleWith(COMPATIBILITY_YOUTUBE)
}
