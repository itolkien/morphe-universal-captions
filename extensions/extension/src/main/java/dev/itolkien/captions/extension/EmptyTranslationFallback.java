/* Copyright 2026 itolkien. Licensed under GPL-3.0-or-later; see LICENSE. */
package dev.itolkien.captions.extension;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import dev.itolkien.captions.extension.CaptionTranslationList.CaptionTrack;

/** Single-source opt-in experiment. Never stores state or selects/enables a caption. */
public final class EmptyTranslationFallback {
    private EmptyTranslationFallback() { }
    private static final String AUTO = "AUTO_TRANSLATE_CAPTIONS_OPTION";

    /** Implemented by the native list model; getters bypass the fallback hooks. */
    public interface CaptionSource {
        List<CaptionTrack> universalCaptions_nativeDirect(boolean bypassFallback);
        List<CaptionTrack> universalCaptions_nativeTranslated(boolean bypassFallback);
        CaptionTrack universalCaptions_autoTranslateOption();
    }

    public static List<CaptionTrack> direct(List<CaptionTrack> tracks, CaptionSource source, Locale locale) {
        if (tracks == null || source == null || locale == null) return tracks;
        try {
            CaptionTrack candidate = candidate(tracks);
            if (candidate == null) return tracks;
            List<CaptionTrack> translated = source.universalCaptions_nativeTranslated(true);
            if (translated == null || !translated.isEmpty()) return tracks;
            // Use YouTube's own localized menu row, including its native option flags.
            CaptionTrack option = source.universalCaptions_autoTranslateOption();
            if (option == null || !AUTO.equals(option.universalCaptions_getLanguageCode())) return tracks;
            List<CaptionTrack> result = new ArrayList<>(tracks);
            result.add(option);
            return result;
        } catch (RuntimeException | LinkageError ignored) {
            return tracks;
        }
    }

    public static List<CaptionTrack> translated(List<CaptionTrack> tracks, CaptionSource source, Locale locale) {
        if (tracks == null || !tracks.isEmpty() || source == null || locale == null) return tracks;
        try {
            CaptionTrack candidate = candidate(source.universalCaptions_nativeDirect(true));
            if (candidate == null) return tracks;
            List<CaptionTrack> result = CaptionTranslationList.fromSource(candidate, locale);
            return result.isEmpty() ? tracks : result;
        } catch (RuntimeException | LinkageError ignored) {
            return tracks;
        }
    }

    private static CaptionTrack candidate(List<CaptionTrack> tracks) {
        if (tracks == null) return null;
        CaptionTrack candidate = null;
        for (CaptionTrack track : tracks) {
            if (track == null) return null;
            String language = track.universalCaptions_getLanguageCode();
            if ("DISABLE_CAPTIONS_OPTION".equals(language)) continue;
            // Exactly one valid original source. Labels may be arbitrary uploaded track names.
            if (candidate != null || SourceTranslationExperiment.originalUri(track) == null) return null;
            candidate = track;
        }
        return candidate;
    }
}
