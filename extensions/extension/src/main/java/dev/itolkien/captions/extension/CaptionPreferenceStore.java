package dev.itolkien.captions.extension;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Locale;

/** Durable caption preference. Writes must originate from explicit user actions only. */
public final class CaptionPreferenceStore {
    private static final String STORE = "universal_captions_preference";
    private static final String LANGUAGE = "preferred_language";
    private static final String DISABLED = "captions_explicitly_disabled";
    private CaptionPreferenceStore() {}

    public static String preferredLanguage(Context context) {
        if (context == null) return "";
        return context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
                .getString(LANGUAGE, "");
    }

    public static boolean explicitlyDisabled(Context context) {
        return context != null && context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
                .getBoolean(DISABLED, false);
    }

    public static void onExplicitLanguageSelection(Context context, String code) {
        if (context == null || code == null) return;
        String normalized = code.trim().replace('_', '-').toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z]{2,8}(-[a-z0-9]{2,8})*")) return;
        SharedPreferences.Editor edit = context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit();
        edit.putString(LANGUAGE, normalized).putBoolean(DISABLED, false).apply();
    }

    public static void onExplicitOff(Context context) {
        if (context == null) return;
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit()
                .putBoolean(DISABLED, true).apply();
    }
}
