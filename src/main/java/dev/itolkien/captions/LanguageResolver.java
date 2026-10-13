package dev.itolkien.captions;

import java.util.Collection;
import java.util.Locale;

/** Pure language resolver. Does not invent unavailable YouTube caption tracks. */
public final class LanguageResolver {
    private LanguageResolver() {}

    public static String normalize(String code) {
        if (code == null) return "";
        String s = code.trim().replace('_', '-').toLowerCase(Locale.ROOT);
        if (s.equals("iw") || s.startsWith("iw-")) s = "he" + s.substring(2);
        if (s.equals("in") || s.startsWith("in-")) s = "id" + s.substring(2);
        if (s.equals("ji") || s.startsWith("ji-")) s = "yi" + s.substring(2);
        return s.matches("[a-z]{2,8}(-[a-z0-9]{2,8})*") ? s : "";
    }

    public static boolean matches(String candidate, String preferred) {
        String c = normalize(candidate), p = normalize(preferred);
        if (c.isEmpty() || p.isEmpty()) return false;
        if (c.equals(p)) return true;
        if (!primary(c).equals(primary(p))) return false;
        if (primary(c).equals("zh") && isTraditional(c) != isTraditional(p)
                && (hasChineseVariant(c) || hasChineseVariant(p))) return false;
        return true;
    }

    private static String primary(String s) {
        int i = s.indexOf('-');
        return i < 0 ? s : s.substring(0, i);
    }
    private static boolean hasChineseVariant(String s) {
        return s.contains("hans") || s.contains("hant") || s.endsWith("-cn")
                || s.endsWith("-tw") || s.endsWith("-hk");
    }
    private static boolean isTraditional(String s) {
        return s.contains("hant") || s.endsWith("-tw") || s.endsWith("-hk");
    }

    /**
     * Resolve a genuinely existing track. Null means YouTube has not exposed that
     * target, not that we can safely manufacture a track identifier.
     */
    public static String findExistingTarget(Collection<String> available, String preferred) {
        if (available == null || preferred == null) return null;
        String target = normalize(preferred);
        if (target.isEmpty()) return null;
        for (String lang : available) if (target.equals(normalize(lang))) return lang;
        for (String lang : available) if (matches(lang, target)) return lang;
        return null;
    }
}
