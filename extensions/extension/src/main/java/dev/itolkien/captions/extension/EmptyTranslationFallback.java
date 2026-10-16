/* Copyright 2026 itolkien. Licensed under GPL-3.0-or-later; see LICENSE. */
package dev.itolkien.captions.extension;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import dev.itolkien.captions.extension.CaptionTranslationList.CaptionTrack;

/** Per-manager, current-audio opt-in fallback. Never stores state or selects/enables a caption. */
public final class EmptyTranslationFallback {
    private EmptyTranslationFallback() { }
    private static final String AUTO = "AUTO_TRANSLATE_CAPTIONS_OPTION";

    /** Implemented by the native list model; getters bypass the fallback hooks. */
    public interface CaptionSource {
        List<CaptionTrack> universalCaptions_nativeDirect(boolean bypassFallback);
        List<CaptionTrack> universalCaptions_nativeTranslated(boolean bypassFallback);
        CaptionTrack universalCaptions_autoTranslateOption();
        List<String> universalCaptions_targetLanguages(boolean bypassFallback);
        CaptionTrack universalCaptions_defaultSource(boolean bypassFallback);
    }

    public static List<CaptionTrack> direct(List<CaptionTrack> tracks, CaptionSource source, Locale locale) {
        if (tracks == null || source == null || locale == null) return tracks;
        try {
            for (CaptionTrack track : tracks) {
                if (track != null && AUTO.equals(track.universalCaptions_getLanguageCode())) return tracks;
            }
            CaptionTrack candidate = candidate(tracks, source);
            if (candidate == null || CaptionTranslationList.fromSource(candidate, locale,
                    source.universalCaptions_targetLanguages(true)).isEmpty()) return tracks;
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
        if (tracks == null || source == null || locale == null) return tracks;
        try {
            if (!tracks.isEmpty()) return CaptionIntegration.augmentMetadata(tracks, locale,
                    source.universalCaptions_targetLanguages(true));
            CaptionTrack candidate = candidate(source.universalCaptions_nativeDirect(true), source);
            if (candidate == null) return tracks;
            List<CaptionTrack> result = CaptionTranslationList.fromSource(candidate, locale, source.universalCaptions_targetLanguages(true));
            return result.isEmpty() ? tracks : result;
        } catch (RuntimeException | LinkageError ignored) {
            return tracks;
        }
    }

    private static CaptionTrack candidate(List<CaptionTrack> tracks, CaptionSource source) {
        if (tracks == null) return null;
        List<CaptionTrack> originals = new ArrayList<>();
        String video = null;
        for (CaptionTrack track : tracks) {
            if (track == null) return null;
            String language = track.universalCaptions_getLanguageCode();
            if ("DISABLE_CAPTIONS_OPTION".equals(language) || AUTO.equals(language)) continue;
            java.net.URI uri = SourceTranslationExperiment.originalUri(track);
            if (uri == null) return null;
            String identity = SourceTranslationExperiment.videoId(uri);
            if (video != null && !video.equals(identity)) return null;
            video = identity;
            originals.add(track);
        }
        if (originals.isEmpty()) return null;
        CaptionTrack preferred = source.universalCaptions_defaultSource(true);
        if (preferred != null) for (CaptionTrack original : originals) {
            if (original.universalCaptions_getVssId().equals(preferred.universalCaptions_getVssId())
                    && original.universalCaptions_getCaptionUrl().equals(preferred.universalCaptions_getCaptionUrl())) return original;
        }
        // The native direct list already contains only the current audio's caption indices.
        // Keep its order if YouTube declares no default; never import an unrelated source.
        return originals.get(0);
    }
}
