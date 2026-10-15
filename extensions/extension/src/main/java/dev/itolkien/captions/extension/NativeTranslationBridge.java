package dev.itolkien.captions.extension;

import android.util.Log;
import java.util.List;
import java.util.Locale;
import dev.itolkien.captions.extension.CaptionTranslationList.CaptionTrack;

/** Menu augmentation only. Caption visibility and preferred-language policy belong to Morphe. */
public final class NativeTranslationBridge {
    private NativeTranslationBridge() { }

    private static Locale menuLocale() {
        try {
            // Resolve at runtime to avoid dependencies between independent patch bundles.
            Object locale = Class.forName("app.morphe.extension.shared.requests.Requester")
                    .getMethod("getAppLocale").invoke(null);
            if (locale instanceof Locale) return (Locale) locale;
        } catch (ReflectiveOperationException | LinkageError ignored) { }
        return Locale.getDefault();
    }

    public static List<CaptionTrack> augment(List<CaptionTrack> tracks) {
        try {
            return CaptionIntegration.augment(tracks, menuLocale());
        } catch (Exception ex) {
            Log.w("MorpheUniversalCaptions", "Could not extend translation language menu", ex);
            return tracks;
        }
    }
}
