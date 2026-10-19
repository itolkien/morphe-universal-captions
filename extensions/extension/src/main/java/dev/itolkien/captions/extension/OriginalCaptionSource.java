/*
 * Copyright 2026 itolkien.
 * Licensed under GPL-3.0-or-later; see LICENSE.
 */
package dev.itolkien.captions.extension;

import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import dev.itolkien.captions.extension.CaptionTranslationList.CaptionTrack;

/** One validated original caption source, bound to its video and signed URL. */
final class OriginalCaptionSource {
    private final CaptionTrack track;
    private final String videoId;
    private final String languageKey;
    private final String urlPrefix;
    private final String urlSuffix;
    private final String vssPrefix;

    private OriginalCaptionSource(CaptionTrack track, String videoId, String url, String vss) {
        this.track = track;
        this.videoId = videoId;
        this.languageKey = CaptionTranslationList.languageKey(track.universalCaptions_getLanguageCode());
        int fragment = url.indexOf('#');
        this.urlPrefix = (fragment < 0 ? url : url.substring(0, fragment)) + "&tlang=";
        this.urlSuffix = fragment < 0 ? "" : url.substring(fragment);
        this.vssPrefix = "t" + vss + ".";
    }

    /** Check the timedtext endpoint without decoding or rewriting signed URL bytes. */
    static URI timedTextUri(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null
                    || !("youtube.com".equalsIgnoreCase(host)
                    || host.toLowerCase(Locale.ROOT).endsWith(".youtube.com"))
                    || uri.getUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443)
                    || !"/api/timedtext".equals(uri.getRawPath()) || uri.getRawQuery() == null) return null;
            return uri;
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return null;
        }
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
            URI uri = timedTextUri(url);
            if (uri == null || uri.getRawQuery().isEmpty()) return null;
            Map<String, String> fields = new HashMap<>();
            for (String parameter : uri.getRawQuery().split("&", -1)) {
                int equals = parameter.indexOf('=');
                String rawKey = equals < 0 ? parameter : parameter.substring(0, equals);
                String key = URLDecoder.decode(rawKey, "UTF-8");
                if (!key.equals("v") && !key.equals("lang") && !key.equals("kind")
                        && !key.equals("name") && !key.equals("tlang")) continue;
                String value = URLDecoder.decode(equals < 0 ? "" : parameter.substring(equals + 1), "UTF-8");
                if (!rawKey.equals(key) || fields.put(key, value) != null || key.equals("tlang")) return null;
            }
            String video = fields.get("v");
            String urlLanguage = fields.get("lang");
            String kind = fields.get("kind");
            if (video == null || !video.matches("[A-Za-z0-9_-]{11}")
                    || !CaptionTranslationList.validLanguageCode(urlLanguage)
                    || !CaptionTranslationList.languageKey(language).equals(CaptionTranslationList.languageKey(urlLanguage))
                    || (asr ? !"asr".equals(kind) : kind != null && !kind.isEmpty())) return null;
            return new OriginalCaptionSource(track, video, url, vss);
        } catch (IllegalArgumentException | NullPointerException | java.io.UnsupportedEncodingException ignored) {
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
