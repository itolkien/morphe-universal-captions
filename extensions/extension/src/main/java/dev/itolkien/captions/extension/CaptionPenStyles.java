/*
 * Styled translated-caption fix adapted from Morphe PR #3837.
 * Copyright 2026 Morphe and itolkien. See NOTICE for GPLv3 Section 7 terms.
 */
package dev.itolkien.captions.extension;

import java.net.URI;
import java.net.URLDecoder;

/**
 * Prevents translated srv3 caption text from inheriting invisible creator-defined pens.
 * Original captions keep their creator styles.
 */
public final class CaptionPenStyles {
    private CaptionPenStyles() { }

    private static final int DEFAULT_PEN = Integer.MIN_VALUE;

    // A timedtext request can be dispatched on a different thread from the srv3 parser.
    // The latest-request state is therefore process-wide; concurrent requests remain
    // a known limitation until request/parser identities can be paired.
    private static volatile boolean translated;

    /** Called before YouTube constructs a timedtext network request. */
    public static void setTimedTextUrl(String url) {
        if (url == null || !url.contains("/api/timedtext?")) return;
        URI uri = OriginalCaptionSource.timedTextUri(url);
        if (uri == null) return;
        boolean hasTarget = false;
        try {
            for (String part : uri.getRawQuery().split("&", -1)) {
                int equals = part.indexOf('=');
                String rawKey = equals < 0 ? part : part.substring(0, equals);
                if (!"tlang".equals(URLDecoder.decode(rawKey, "UTF-8"))) continue;
                // Invalid, encoded or duplicate translation targets are ambiguous.
                if (hasTarget || !rawKey.equals("tlang") || equals < 0 ||
                        !CaptionTranslationList.validLanguageCode(part.substring(equals + 1))) {
                    translated = false;
                    return;
                }
                hasTarget = true;
            }
            translated = hasTarget;
        } catch (IllegalArgumentException | java.io.UnsupportedEncodingException ignored) {
            translated = false;
        }
    }

    /** Called at the shared srv3 pen lookup, for pen/paragraph/segment styles. */
    public static int getPenId(int penId) {
        return translated ? DEFAULT_PEN : penId;
    }
}
