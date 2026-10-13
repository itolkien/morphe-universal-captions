package dev.itolkien.captions.extension;

import android.net.Uri;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

/** Native captions only: clones verified host track objects. No external translator. */
@SuppressWarnings({"unused","unchecked"})
public final class NativeTranslationBridge {
    private static final String TAG = "MorpheUniversalCaptions";
    // Initial broad target catalogue; YouTube support varies by video and account.
    private static final String[] TARGETS = ("af am ar as az be bg bho bn bs ca ceb co cs cy da de " +
        "dv el en eo es et eu fa fi fil fr fy ga gd gl gu ha haw he hi hmn hr ht hu hy " +
        "id ig is it iw ja jv ka kk km kn ko ku ky la lb lo lt lv mg mi mk ml mn mr ms " +
        "mt my ne nl no ny or pa pl ps pt ro ru rw sd si sk sl sm sn so sq sr st su " +
        "sv sw ta te tg th tk tl tr tt ug uk ur uz vi xh yi yo zh-Hans zh-Hant zu").split(" ");
    private NativeTranslationBridge() {}

    public static String language(Object track) { return ""; } // bound in APK bytecode
    public static String vss(Object track) { return ""; }
    public static String url(Object track) { return ""; }
    public static Object cloneTranslation(Object track, String code) { return null; }

    public static String translatedUrl(String source, String code) {
        if (source == null || code == null || !code.matches("[A-Za-z]{2,8}(-[A-Za-z0-9]{2,8})*")) return source;
        try {
            Uri uri = Uri.parse(source);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) ||
                    !("www.youtube.com".equalsIgnoreCase(host) || "youtube.com".equalsIgnoreCase(host)) ||
                    !"/api/timedtext".equals(uri.getPath())) return source;
            Uri.Builder b = uri.buildUpon().clearQuery();
            for (String name : uri.getQueryParameterNames()) {
                if (!"tlang".equalsIgnoreCase(name)) {
                    for (String value : uri.getQueryParameters(name)) b.appendQueryParameter(name, value);
                }
            }
            b.appendQueryParameter("tlang", code);
            return b.build().toString();
        } catch (Throwable ignored) { return source; }
    }

    public static String translatedVss(String source, String code) {
        int dot = source == null ? -1 : source.indexOf('.');
        return "t" + code + (dot < 0 ? "" : source.substring(dot));
    }

    public static CharSequence translatedName(String code) {
        if ("zh-Hans".equals(code)) return "Chinese (Simplified)";
        if ("zh-Hant".equals(code)) return "Chinese (Traditional)";
        Locale l = Locale.forLanguageTag(code);
        String label = l.getDisplayName(Locale.getDefault());
        return label.isEmpty() ? code : label;
    }

    /** Returns MAX_VALUE for virtual/menu entries and previously translated tracks. */
    static int sourceRank(Object track) {
        try {
            String source = url(track);
            if (source == null) return Integer.MAX_VALUE;
            Uri uri = Uri.parse(source);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) ||
                    !("youtube.com".equalsIgnoreCase(host) || "www.youtube.com".equalsIgnoreCase(host)) ||
                    !"/api/timedtext".equals(uri.getPath())) return Integer.MAX_VALUE;
            String target = uri.getQueryParameter("tlang");
            if (target != null && !target.isEmpty()) return Integer.MAX_VALUE;
            String code = language(track);
            if (code == null || code.isEmpty() || code.endsWith("_OPTION"))
                return Integer.MAX_VALUE;
            // Manual source tracks usually have a plain VSS ID; ASR uses "a.".
            String trackId = vss(track);
            return trackId != null && trackId.startsWith("a.") ? 1 : 0;
        } catch (Throwable ignored) {
            return Integer.MAX_VALUE;
        }
    }

    // Keep direct caption languages separate by video, so tracks do not leak across videos.
    private static final Map<String, Set<String>> MANUAL_LANGUAGES =
            Collections.synchronizedMap(new java.util.LinkedHashMap<String, Set<String>>() {
                @Override protected boolean removeEldestEntry(Map.Entry<String, Set<String>> eldest) {
                    return size() > 8;
                }
            });

    private static String videoId(String source) {
        if (source == null) return "";
        try {
            String id = Uri.parse(source).getQueryParameter("v");
            return id == null ? "" : id;
        } catch (Throwable ignored) { return ""; }
    }

    /** Receives YouTube's original tracks before the auto-translate list is built. */
    public static List<?> observeDirect(List<?> tracks) {
        if (tracks == null || tracks.isEmpty()) return tracks;
        try {
            Map<String, Set<String>> perVideo = new HashMap<>();
            for (Object track : tracks) {
                String id = videoId(url(track));
                String code = language(track);
                String trackId = vss(track);
                if (id.isEmpty()) continue;
                Set<String> langs = perVideo.get(id);
                if (langs == null) { langs = new HashSet<>(); perVideo.put(id, langs); }
                if (code != null && !code.isEmpty() && !code.endsWith("_OPTION") &&
                        trackId != null && !trackId.startsWith("a.") &&
                        sourceRank(track) != Integer.MAX_VALUE)
                    langs.add(code.toLowerCase(Locale.ROOT));
            }
            synchronized (MANUAL_LANGUAGES) { MANUAL_LANGUAGES.putAll(perVideo); }
        } catch (Throwable e) {
            Log.e(TAG, "Direct track observation failed: " + e.getClass().getSimpleName());
        }
        return tracks;
    }

    public static List<?> augment(List<?> original) {
        if (original == null || original.isEmpty()) return original;
        try {
            Object prototype = null;
            int bestSourceRank = Integer.MAX_VALUE;
            Set<String> present = new HashSet<>();
            for (Object track : original) {
                String lc = language(track);
                if (lc != null) present.add(lc.toLowerCase(Locale.ROOT));
                // Do not translate an already translated track again.
                // Rank valid originals by YouTube track type, preserving the
                // original menu order when candidates have equal priority.
                int rank = sourceRank(track);
                if (rank < bestSourceRank) {
                    prototype = track;
                    bestSourceRank = rank;
                }
            }
            if (prototype == null) {
                // The auto-translate list may already contain derived tracks rather
                // than original tracks. Keep the known-working selection behavior
                // instead of hiding every additional language.
                for (Object track : original) {
                    String u = url(track);
                    if (u != null && u.startsWith("https://") && u.contains("/api/timedtext")) {
                        prototype = track;
                        break;
                    }
                }
                Log.e(TAG, "Source selection: using legacy fallback");
            } else {
                Log.e(TAG, "Selected original caption source rank=" + bestSourceRank);
            }
            if (prototype == null) return original;
            Set<String> manual = Collections.emptySet();
            String id = videoId(url(prototype));
            if (!id.isEmpty()) {
                synchronized (MANUAL_LANGUAGES) {
                    Set<String> known = MANUAL_LANGUAGES.get(id);
                    if (known != null) manual = new HashSet<>(known);
                }
            }
            List<Object> result = new ArrayList<>(original);
            int added = 0;
            for (String code : TARGETS) {
                if (manual.contains(code.toLowerCase(Locale.ROOT))) continue;
                if (!present.add(code.toLowerCase(Locale.ROOT))) continue;
                Object newTrack = cloneTranslation(prototype, code);
                if (newTrack != null) { result.add(newTrack); added++; }
            }
            Log.e(TAG, "Native augmentation original=" + original.size() + " added=" + added);
            return added == 0 ? original : result;
        } catch (Throwable e) {
            Log.e(TAG, "Native augmentation failed: " + e.getClass().getSimpleName());
            return original;
        }
    }
}
