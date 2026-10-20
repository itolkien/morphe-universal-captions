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
            OriginalCaptionSource candidate = candidate(tracks, source);
            if (candidate == null) return tracks;
            boolean hasTarget = CaptionTranslationList.targets(source.universalCaptions_targetLanguages(true))
                    .stream().anyMatch(candidate::canTranslate);
            if (!hasTarget) return tracks;
            // Use YouTube's own localized menu row, including its native option flags.
            CaptionTrack option = source.universalCaptions_autoTranslateOption();
            if (option == null || !AUTO.equals(option.universalCaptions_getLanguageCode())) return tracks;
            List<CaptionTrack> result = new ArrayList<>(tracks);
            result.add(option);
            return result;
        } catch (RuntimeException | LinkageError ex) {
            CaptionTranslationList.trace("direct-error", ex.getClass().getSimpleName());
            return tracks;
        }
    }

    public static List<CaptionTrack> translated(List<CaptionTrack> tracks, CaptionSource source, Locale locale) {
        if (tracks == null || source == null || locale == null) return tracks;
        try {
            List<String> nativeTargets = source.universalCaptions_targetLanguages(true);
            CaptionTranslationList.trace("translated-meta", "native=" + tracks.size()
                    + " targetCodes=" + (nativeTargets == null ? -1 : nativeTargets.size()));
            if (!tracks.isEmpty()) {
                List<CaptionTrack> augmented = CaptionTranslationList.augment(tracks, locale, nativeTargets);
                if (augmented.size() > tracks.size()) return augmented;
                // The native generator can choose a different original for each
                // target; use the current audio's verified default for extras.
                OriginalCaptionSource selected = candidate(
                        source.universalCaptions_nativeDirect(true), source);
                if (selected == null) {
                    CaptionTranslationList.trace("mixed-resolution", "reason=no-safe-default");
                    return augmented;
                }
                return CaptionTranslationList.mergeFromSelectedSource(
                        augmented, selected, locale, nativeTargets);
            }
            OriginalCaptionSource candidate = candidate(source.universalCaptions_nativeDirect(true), source);
            if (candidate == null) {
                CaptionTranslationList.trace("translated-empty", "reason=no-safe-source");
                return tracks;
            }
            List<CaptionTrack> result = CaptionTranslationList.fromSource(candidate, locale, nativeTargets);
            CaptionTranslationList.trace("translated-empty", "generated=" + result.size());
            return result.isEmpty() ? tracks : result;
        } catch (RuntimeException | LinkageError ex) {
            CaptionTranslationList.trace("translated-error", ex.getClass().getSimpleName());
            return tracks;
        }
    }

    private static OriginalCaptionSource candidate(List<CaptionTrack> tracks, CaptionSource source) {
        if (tracks == null) return null;
        OriginalCaptionSource first = null, matchedDefault = null;
        CaptionTrack nativeDefault = source.universalCaptions_defaultSource(true);
        String video = null, firstVss = null, firstUrl = null;
        boolean mixedSources = false;
        for (CaptionTrack track : tracks) {
            if (track == null) return null;
            String language = track.universalCaptions_getLanguageCode();
            if ("DISABLE_CAPTIONS_OPTION".equals(language) || AUTO.equals(language)) continue;
            OriginalCaptionSource original =
                    OriginalCaptionSource.from(track);
            if (original == null || video != null && !video.equals(original.videoId())) return null;
            video = original.videoId();
            String vss = track.universalCaptions_getVssId();
            if (first == null) {
                first = original;
                firstVss = vss;
                firstUrl = track.universalCaptions_getCaptionUrl();
            } else if (!firstVss.equals(vss)
                    || !firstUrl.equals(track.universalCaptions_getCaptionUrl())) mixedSources = true;
            if (nativeDefault != null && matchedDefault == null
                    && vss.equals(nativeDefault.universalCaptions_getVssId())
                    && track.universalCaptions_getCaptionUrl().equals(nativeDefault.universalCaptions_getCaptionUrl())) {
                matchedDefault = original;
            }
        }
        // Native caption eligibility is track-specific, even within the same language.
        return matchedDefault != null ? matchedDefault : mixedSources ? null : first;
    }
}
