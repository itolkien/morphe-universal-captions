/* Copyright 2026 itolkien. Licensed under GPL-3.0-or-later; see LICENSE. */
package dev.itolkien.captions.extension;

import java.util.List;
import java.util.Locale;
import dev.itolkien.captions.extension.CaptionTranslationList.CaptionTrack;

/** Cooperates with the native integration from Morphe PR #3754 without linking patch bundles. */
public final class CaptionIntegration {
    private CaptionIntegration() { }

    private static final Class<?> NATIVE_TRACK_INTERFACE = nativeTrackInterface();

    private static Class<?> nativeTrackInterface() {
        try {
            return Class.forName(
                    "app.morphe.extension.youtube.patches.AutoTranslateCaptionsPatch$CaptionTrackInterface",
                    false, CaptionIntegration.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError ignored) {
            return null;
        }
    }

    public static List<CaptionTrack> augment(List<CaptionTrack> tracks, Locale locale) {
        return augment(tracks, locale, NATIVE_TRACK_INTERFACE);
    }

    static List<CaptionTrack> augment(List<CaptionTrack> tracks, Locale locale, Class<?> nativeInterface) {
        // This marker is injected only when the native feature is patched in. Merely bundling
        // Morphe's extension class is insufficient: Captions may not have been selected.
        // Leave both native setting states alone, irrespective of the order of the list hooks.
        if (isManagedByNative(tracks, nativeInterface)) return tracks;
        return CaptionTranslationList.augment(tracks, locale);
    }

    static boolean isManagedByNative(List<?> tracks, Class<?> nativeInterface) {
        if (nativeInterface == null || tracks == null) return false;
        for (Object track : tracks) {
            if (track != null) return nativeInterface.isInstance(track);
        }
        return false;
    }
}
