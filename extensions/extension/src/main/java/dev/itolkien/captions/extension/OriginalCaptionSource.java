/*
 * Copyright 2026 itolkien.
 * Licensed under GPL-3.0-or-later; see LICENSE.
 */
package dev.itolkien.captions.extension;

import java.util.Locale;
import dev.itolkien.captions.extension.CaptionTranslationList.CaptionTrack;

/** One validated original caption source, bound to its video and signed URL. */
final class OriginalCaptionSource {
    private final CaptionTrack track;
    private final String videoId;
    private final String languageKey;
    private final String urlPrefix;
    private final String urlSuffix;
    private final String vssPrefix;

    private OriginalCaptionSource(CaptionTrack track, String language, String videoId, String url, String vss) {
        this.track = track;
        this.videoId = videoId;
        this.languageKey = CaptionTranslationList.languageKey(language);
        int fragment = url.indexOf('#');
        this.urlPrefix = (fragment < 0 ? url : url.substring(0, fragment)) + "&tlang=";
        this.urlSuffix = fragment < 0 ? "" : url.substring(fragment);
        this.vssPrefix = "t" + vss + ".";
    }

    static boolean originalVss(String vss, String prefix) {
        return vss != null && (vss.equals(prefix) || vss.startsWith(prefix + ".")
                && vss.substring(prefix.length() + 1).matches("[A-Za-z0-9_-]{1,128}"));
    }

    /** Reject malformed, translated, non-YouTube and cross-video caption sources. */
    static OriginalCaptionSource from(CaptionTrack track) {
        if (track == null) return null;
        String language = track.universalCaptions_getLanguageCode();
        if (!CaptionTranslationList.validLanguageCode(language)) return null;
        String vss = track.universalCaptions_getVssId();
        boolean asr = originalVss(vss, "a." + language);
        if (!asr && !originalVss(vss, "." + language)) return null;

        try {
            String url = track.universalCaptions_getCaptionUrl();
            TimedTextUrl timedText = TimedTextUrl.parse(url);
            if (timedText == null || !timedText.hasQueryContent()
                    || timedText.field("tlang") != null) return null;
            String video = timedText.field("v");
            String urlLanguage = timedText.field("lang");
            String kind = timedText.field("kind");
            if (video == null || !video.matches("[A-Za-z0-9_-]{11}")
                    || !CaptionTranslationList.validLanguageCode(urlLanguage)
                    || !CaptionTranslationList.languageKey(language).equals(CaptionTranslationList.languageKey(urlLanguage))
                    || (asr ? !"asr".equals(kind) : kind != null && !kind.isEmpty())) return null;
            return new OriginalCaptionSource(track, language, video, url, vss);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return null;
        }
    }

    String videoId() {
        return videoId;
    }

    boolean canTranslate(String language) {
        return CaptionTranslationList.validLanguageCode(language)
                && !languageKey.equals(CaptionTranslationList.languageKey(language));
    }

    CaptionTrack translate(String language, Locale locale) {
        if (locale == null || !canTranslate(language)) return null;
        String label = Locale.forLanguageTag(language).getDisplayName(locale);
        if (label.isEmpty()) label = language;
        return track.universalCaptions_copyTranslation(language, urlPrefix + language + urlSuffix,
                vssPrefix + language, label);
    }

}
