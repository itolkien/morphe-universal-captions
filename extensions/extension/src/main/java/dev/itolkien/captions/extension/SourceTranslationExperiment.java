/* Copyright 2026 itolkien. Licensed under GPL-3.0-or-later; see LICENSE. */
package dev.itolkien.captions.extension;

import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Derives translations from one validated original ASR or manual caption source. */
public final class SourceTranslationExperiment {
    private SourceTranslationExperiment() { }
    private static boolean language(String code) {
        return code != null && code.matches("[a-z]{2,8}(-[A-Za-z0-9]{2,8})*");
    }

    private static boolean originalVss(String vss, String prefix) {
        if (vss == null) return false;
        if (vss.equals(prefix)) return true;
        return vss.startsWith(prefix + ".")
                && vss.substring(prefix.length() + 1).matches("[A-Za-z0-9_-]{1,128}");
    }

    /** Validate identity/query consistency without rewriting signed bytes or guessing a source. */
    static URI originalUri(CaptionTranslationList.CaptionTrack source) {
        if (source == null) return null;
        String code = source.universalCaptions_getLanguageCode();
        String vss = source.universalCaptions_getVssId();
        if (!language(code)) return null;
        boolean asr = originalVss(vss, "a." + code);
        if (!asr && !originalVss(vss, "." + code)) return null;
        try {
            URI uri = URI.create(source.universalCaptions_getCaptionUrl());
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null
                    || !("youtube.com".equalsIgnoreCase(host)
                    || host.toLowerCase(Locale.ROOT).endsWith(".youtube.com"))
                    || uri.getUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443)
                    || !"/api/timedtext".equals(uri.getRawPath())
                    || uri.getRawQuery() == null || uri.getRawQuery().isEmpty()) return null;
            Map<String, String> parameters = new HashMap<>();
            for (String part : uri.getRawQuery().split("&", -1)) {
                int split = part.indexOf('=');
                String rawKey = split < 0 ? part : part.substring(0, split);
                String key = URLDecoder.decode(rawKey, "UTF-8");
                if (!key.equals("v") && !key.equals("lang") && !key.equals("kind")
                        && !key.equals("name") && !key.equals("tlang")) continue;
                String value = URLDecoder.decode(split < 0 ? "" : part.substring(split + 1), "UTF-8");
                if (!key.equals(rawKey) || parameters.put(key, value) != null || key.equals("tlang")) return null;
            }
            String video = parameters.get("v");
            String urlLanguage = parameters.get("lang");
            String kind = parameters.get("kind");
            if (video == null || !video.matches("[A-Za-z0-9_-]{11}") || !language(urlLanguage)
                    || !CaptionTranslationList.languageKey(code).equals(CaptionTranslationList.languageKey(urlLanguage))) return null;
            if (asr ? !"asr".equals(kind) : kind != null && !kind.isEmpty()) return null;
            return uri;
        } catch (IllegalArgumentException | NullPointerException | java.io.UnsupportedEncodingException ex) {
            return null;
        }
    }

    static String videoId(URI uri) {
        for (String parameter : uri.getRawQuery().split("&")) {
            if (parameter.startsWith("v=")) {
                try { return URLDecoder.decode(parameter.substring(2), "UTF-8"); }
                catch (java.io.UnsupportedEncodingException impossible) { throw new AssertionError(impossible); }
            }
        }
        return "";
    }

    public static CaptionTranslationList.CaptionTrack preview(
            CaptionTranslationList.CaptionTrack source, String target, Locale locale) {
        if (!language(target) || locale == null || originalUri(source) == null) return null;
        String sourceCode = source.universalCaptions_getLanguageCode();
        if (CaptionTranslationList.languageKey(sourceCode).equals(CaptionTranslationList.languageKey(target))) return null;
        String url = source.universalCaptions_getCaptionUrl();
        int fragment = url.indexOf('#');
        String base = fragment < 0 ? url : url.substring(0, fragment);
        String suffix = fragment < 0 ? "" : url.substring(fragment);
        String name = Locale.forLanguageTag(target).getDisplayName(locale);
        if (name.isEmpty()) name = target;
        return source.universalCaptions_copyTranslation(target, base + "&tlang=" + target + suffix,
                "t" + source.universalCaptions_getVssId() + "." + target, name);
    }
}
