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
            // Only check whether a target exists; don't construct and sort 156 throwaway tracks.
            List<CaptionTrack> translated = source.universalCaptions_nativeTranslated(true);
            if (translated == null || !translated.isEmpty()) return tracks;
            CaptionTrack candidate = candidate(tracks, source);
            if (candidate == null) return tracks;
            String sourceKey = CaptionTranslationList.languageKey(candidate.universalCaptions_getLanguageCode());
            boolean hasTarget = CaptionTranslationList.targets(source.universalCaptions_targetLanguages(true))
                    .stream().anyMatch(code -> !sourceKey.equals(CaptionTranslationList.languageKey(code)));
            if (!hasTarget) return tracks;
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
        CaptionTrack first = null, matchedDefault = null;
        CaptionTrack nativeDefault = source.universalCaptions_defaultSource(true);
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
            if (first == null) first = track;
            if (nativeDefault != null && matchedDefault == null
                    && track.universalCaptions_getVssId().equals(nativeDefault.universalCaptions_getVssId())
                    && track.universalCaptions_getCaptionUrl().equals(nativeDefault.universalCaptions_getCaptionUrl())) {
                matchedDefault = track;
            }
        }
        // All candidates were validated before a source was returned.
        return matchedDefault != null ? matchedDefault : first;
    }
}
