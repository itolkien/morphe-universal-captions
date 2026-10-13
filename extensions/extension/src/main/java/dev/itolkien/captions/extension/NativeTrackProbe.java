package dev.itolkien.captions.extension;

import android.util.Log;
import java.util.List;

/** Native track-list probe. No track modification or network requests. */
@SuppressWarnings("unused")
public final class NativeTrackProbe {
    private NativeTrackProbe() {}

    public static <T> List<T> observe(List<T> tracks) {
        try {
            Log.i("MorpheUniversalCaptions",
                    "YouTube native auto-translate candidate count: "
                    + (tracks == null ? "null" : tracks.size()));
        } catch (Throwable ignored) {
            // Never interfere with original caption selection.
        }
        return tracks;
    }
}
