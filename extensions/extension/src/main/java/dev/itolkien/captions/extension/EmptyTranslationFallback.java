/* Copyright 2026 itolkien. Licensed under GPL-3.0-or-later; see LICENSE. */
package dev.itolkien.captions.extension;

import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import dev.itolkien.captions.extension.CaptionTranslationList.CaptionTrack;

/** Video-scoped opt-in experiment. Never stores state or selects/enables a caption. */
public final class EmptyTranslationFallback {
    private EmptyTranslationFallback() { }
    private static final String AUTO = "AUTO_TRANSLATE_CAPTIONS_OPTION";

    /** Implemented by the native list model; getters bypass the fallback wrappers. */
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
            // Exactly one original Arabic ASR source, no native auto option or other tracks.
            if (candidate != null || !"ar".equals(language)
                    || !"a.ar".equals(track.universalCaptions_getVssId())
                    || SourceTranslationExperiment.preview(track, "en", Locale.ROOT) == null) return null;
            URI uri = URI.create(track.universalCaptions_getCaptionUrl());
            Map<String, String> parameters = new HashMap<>();
            for (String part : uri.getRawQuery().split("&", -1)) {
                int split = part.indexOf('=');
                String rawKey = split < 0 ? part : part.substring(0, split);
                String key;
                String value;
                try {
                    key = URLDecoder.decode(rawKey, "UTF-8");
                    value = URLDecoder.decode(split < 0 ? "" : part.substring(split + 1), "UTF-8");
                } catch (java.io.UnsupportedEncodingException impossible) {
                    return null;
                }
                if (!key.equals("v") && !key.equals("lang") && !key.equals("kind") && !key.equals("tlang")) continue;
                if (!key.equals(rawKey) || parameters.put(key, value) != null || key.equals("tlang")) return null;
            }
            if (!"PlsODqqqwFs".equals(parameters.get("v"))
                    || !"ar".equals(parameters.get("lang"))
                    || !"asr".equals(parameters.get("kind"))) return null;
            candidate = track;
        }
        return candidate;
    }
}
