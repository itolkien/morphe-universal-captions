package dev.itolkien.captions.extension;

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

    public static List<CaptionTrack> fallbackDirect(List<CaptionTrack> tracks,
            EmptyTranslationFallback.CaptionSource source) {
        CaptionTranslationList.trace("hook-direct", "in=" + (tracks == null ? -1 : tracks.size()));
        List<CaptionTrack> result = EmptyTranslationFallback.direct(tracks, source, menuLocale());
        CaptionTranslationList.trace("hook-direct", "out=" + (result == null ? -1 : result.size()));
        return result;
    }

    public static List<CaptionTrack> fallbackTranslated(List<CaptionTrack> tracks,
            EmptyTranslationFallback.CaptionSource source) {
        CaptionTranslationList.trace("hook-translated", "in=" + (tracks == null ? -1 : tracks.size()));
        List<CaptionTrack> result = EmptyTranslationFallback.translated(tracks, source, menuLocale());
        CaptionTranslationList.trace("hook-translated", "out=" + (result == null ? -1 : result.size()));
        return result;
    }

}
