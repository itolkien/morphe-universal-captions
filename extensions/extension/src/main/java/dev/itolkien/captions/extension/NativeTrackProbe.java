package dev.itolkien.captions.extension;

import android.util.Log;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Diagnostic only: never changes YouTube's list contents or ordering. */
@SuppressWarnings("unused")
public final class NativeTrackProbe {
    private static final String TAG = "MorpheUniversalCaptions";
    private static final AtomicInteger DIRECT = new AtomicInteger();
    private static final AtomicInteger TRANSLATED = new AtomicInteger();
    private NativeTrackProbe() {}

    /** Stable entry point shared by all hooks. */
    public static <T> List<T> observe(List<T> tracks) {
        report("ANY", DIRECT.incrementAndGet(), tracks);
        return tracks;
    }

    public static <T> List<T> observeDirect(List<T> tracks) {
        report("DIRECT", DIRECT.incrementAndGet(), tracks);
        return tracks;
    }

    public static <T> List<T> observeTranslated(List<T> tracks) {
        report("TRANSLATED", TRANSLATED.incrementAndGet(), tracks);
        return tracks;
    }

    private static void report(String kind, int count, List<?> tracks) {
        try {
            String message = "Probe " + kind + " call=" + count + " candidates="
                    + (tracks == null ? "null" : tracks.size());
            Log.e(TAG, message); // ERROR to survive restrictive logcat filters.
        } catch (Throwable ignored) {
            // Never interfere with playback.
        }
    }
}
