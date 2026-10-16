/*
 * Copyright 2026 itolkien.
 * Licensed under GPL-3.0-or-later; see LICENSE.
 */
package dev.itolkien.captions.extension;

import java.net.URI;
import java.util.Locale;

/**
 * Pure source-to-translation construction used by the opt-in empty-menu fallback.
 *
 * Unlike CaptionTranslationList.augment(), this accepts an original direct
 * caption track, including YouTube ASR tracks such as a.ar. Whether YouTube
 * actually serves the resulting URL must be verified on-device.
 */
public final class SourceTranslationExperiment {
    private SourceTranslationExperiment() { }

    public static CaptionTranslationList.CaptionTrack preview(
            CaptionTranslationList.CaptionTrack source, String target, Locale locale) {
        if (source == null || target == null || locale == null
                || !target.matches("[a-z]{2,8}(-[A-Za-z0-9]{2,8})*")) return null;
        String sourceCode = source.universalCaptions_getLanguageCode();
        String sourceVss = source.universalCaptions_getVssId();
        String url = source.universalCaptions_getCaptionUrl();
        if (sourceCode == null || !sourceCode.matches("[a-z]{2,8}(-[A-Za-z0-9]{2,8})*")
                || sourceCode.equalsIgnoreCase(target) || sourceVss == null || url == null) return null;
        // Do not manufacture translations of synthetic menu options or translated tracks.
        if (!(sourceVss.equals("." + sourceCode) || sourceVss.equals("a." + sourceCode))) return null;
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null
                    || !("youtube.com".equalsIgnoreCase(host)
                    || host.toLowerCase(Locale.ROOT).endsWith(".youtube.com"))
                    || uri.getUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443)
                    || !"/api/timedtext".equals(uri.getRawPath())
                    || uri.getRawQuery() == null || uri.getRawQuery().isEmpty()) return null;
            for (String parameter : uri.getRawQuery().split("&", -1)) {
                if (parameter.startsWith("tlang=") || parameter.equals("tlang")) return null;
            }
            int fragment = url.indexOf('#');
            String base = fragment < 0 ? url : url.substring(0, fragment);
            String suffix = fragment < 0 ? "" : url.substring(fragment);
            String languageName = Locale.forLanguageTag(target).getDisplayName(locale);
            if (languageName.isEmpty()) languageName = target;
            return source.universalCaptions_copyTranslation(
                    target, base + "&tlang=" + target + suffix,
                    "t" + sourceVss + "." + target, languageName);
        } catch (IllegalArgumentException | NullPointerException ex) {
            return null;
        }
    }
}
