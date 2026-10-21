/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3837
 * https://github.com/MorpheApp/morphe-patches/pull/3891
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.youtube.patches;

import androidx.annotation.Nullable;


@SuppressWarnings("unused")
public class TranslatedCaptionPensPatch {

    /**
     * Interface to use obfuscated methods.
     */
    public interface CaptionRequestInterface {
        // Method is added during patching.

        /**
         * @return The requested object. A caption track if this is a caption request.
         */
        @Nullable
        Object patch_getCaptionTrack();
    }

    /**
     * Pen id that is never declared by a timed text document.
     * Looking it up returns the default pen, which uses the user caption style.
     */
    private static final int DEFAULT_PEN_ID = Integer.MIN_VALUE;

    /** A linked scope needs one allocation per decoder, with no separate deque. */
    private static final class Scope {
        final Object request;
        final boolean translated;
        final Scope previous;

        Scope(Object request, boolean translated, Scope previous) {
            this.request = request;
            this.translated = translated;
            this.previous = previous;
        }
    }

    private static final ThreadLocal<Scope> scopes = new ThreadLocal<>();

    /** Enter exactly one cached or network caption decoder invocation. */
    public static void beginCaption(@Nullable Object request) {
        boolean translated = false;
        if (request instanceof CaptionRequestInterface captionRequest) {
            try {
                Object track = captionRequest.patch_getCaptionTrack();
                // Pen rendering depends on the native translated track, not whether
                // its URL is suitable for cloning into an additional language.
                // YouTube may supply translated tracks without a canonical timedtext URL.
                if (track instanceof PreferredCaptionLanguagePatch.CaptionTrackInterface caption) {
                    String vss = caption.patch_getVssId();
                    String language = caption.patch_getLanguageCode();
                    translated = vss != null && language != null
                            && (vss.startsWith("t.") || vss.startsWith("ta."))
                            && vss.endsWith("." + language);
                }
            } catch (RuntimeException | LinkageError ignored) {
                // Unknown native layouts must not modify the original pen styling.
            }
        }
        scopes.set(new Scope(request, translated, scopes.get()));
    }

    /** Identity-checked cleanup, including native exception handlers. */
    public static void endCaption(@Nullable Object request) {
        Scope current = scopes.get();
        if (current == null || current.request != request) return;
        if (current.previous == null) scopes.remove();
        else scopes.set(current.previous);
    }

    /**
     * Injection point.
     *
     * Auto-translated tracks keep the pen ids of the original track, but the translated text
     * does not line up with the original styled segments. Channels that use an invisible pen
     * for padding characters can end up with the entire translated line using that pen,
     * which shows only the caption background.
     *
     * @param penId Pen id of a timed text element.
     * @return The pen id to use.
     */
    public static int getPenId(int penId) {
        Scope current = scopes.get();
        return current != null && current.translated
                ? DEFAULT_PEN_ID : penId;
    }
}
