/*
 * Styled translated-caption fix adapted from Morphe PR #3837.
 * Copyright 2026 Morphe and itolkien. See NOTICE for GPLv3 Section 7 terms.
 */
package dev.itolkien.captions.extension;

import java.util.ArrayDeque;
import dev.itolkien.captions.extension.CaptionTranslationList.CaptionTrack;

/**
 * Caption styles belong to the document being parsed, not the most recent URL
 * requested by the player. Native parser hooks enter and exit on that parser's
 * own thread, including cached captions and concurrent requests.
 */
public final class CaptionPenStyles {
    private CaptionPenStyles() { }

    public interface CaptionCarrier {
        CaptionTrack universalCaptions_styleTrack();
    }

    private static final int DEFAULT_PEN = Integer.MIN_VALUE;
    private static final ThreadLocal<ArrayDeque<Boolean>> SCOPES = new ThreadLocal<>();

    /** Called at a synchronous caption parser invocation, never at request dispatch. */
    public static void beginCaption(Object request) {
        boolean translated = false;
        if (request instanceof CaptionCarrier) {
            try {
                CaptionTrack track = ((CaptionCarrier) request).universalCaptions_styleTrack();
                if (track != null) {
                    String code = track.universalCaptions_getLanguageCode();
                    String vss = track.universalCaptions_getVssId();
                    TimedTextUrl url = TimedTextUrl.parse(track.universalCaptions_getCaptionUrl());
                    translated = CaptionTranslationList.validLanguageCode(code)
                            && vss != null && vss.startsWith("t") && vss.endsWith("." + code)
                            && url != null && code.equals(url.field("tlang"))
                            && url.targetValueOffset(code) >= 0;
                }
            } catch (RuntimeException | LinkageError ignored) {
                // An unknown native track must never alter the original's creator styles.
            }
        }
        ArrayDeque<Boolean> scopes = SCOPES.get();
        if (scopes == null) {
            scopes = new ArrayDeque<>();
            SCOPES.set(scopes);
        }
        scopes.push(translated);
    }

    /** Called on parser success and exception exits. */
    public static void endCaption() {
        ArrayDeque<Boolean> scopes = SCOPES.get();
        if (scopes == null || scopes.isEmpty()) return;
        scopes.pop();
        if (scopes.isEmpty()) SCOPES.remove();
    }

    /** Native pen lookup can occur during a translated or original document parse. */
    public static int getPenId(int penId) {
        ArrayDeque<Boolean> scopes = SCOPES.get();
        return scopes != null && !scopes.isEmpty() && scopes.peek()
                ? DEFAULT_PEN : penId;
    }
}
