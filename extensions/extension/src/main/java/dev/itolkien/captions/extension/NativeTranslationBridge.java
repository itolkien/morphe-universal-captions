package dev.itolkien.captions.extension;

import android.net.Uri;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    public static List<?> augment(List<?> original) {
        if (original == null || original.isEmpty()) return original;
        try {
            Object prototype = null;
            Set<String> present = new HashSet<>();
            for (Object track : original) {
                String lc = language(track);
                if (lc != null) present.add(lc.toLowerCase(Locale.ROOT));
                String u = url(track);
                if (prototype == null && u != null && u.contains("/api/timedtext")
                        && u.startsWith("https://")) prototype = track;
            }
            if (prototype == null) return original;
            List<Object> result = new ArrayList<>(original);
            int added = 0;
            for (String code : TARGETS) {
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
