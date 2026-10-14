package dev.itolkien.captions.extension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Curated native translation targets. Inclusion does not guarantee server availability. */
public final class TranslationTargets {
    private TranslationTargets() {}
    private static final String[] TARGETS = ("af am ar as az be bg bho bn bs ca ceb co cs cy da de " +
        "dv el en eo es et eu fa fi fil fr fy ga gd gl gu ha haw he hi hmn hr ht hu hy " +
        "id ig is it ja jv ka kk km kn ko ku ky la lb lo lt lv mg mi mk ml mn mr ms " +
        "mt my ne nl no ny or pa pl ps pt ro ru rw sd si sk sl sm sn so sq sr st su " +
        "sv sw ta te tg th tk tr tt ug uk ur uz vi xh yi yo zh-Hans zh-Hant zu").split(" ");

    /** Preserve native entries; add only targets not already represented in either menu. */
    public static List<String> missing(Collection<String> present, Collection<String> manual) {
        Set<String> represented = new HashSet<>();
        for (String code : present) represented.add(key(code));
        for (String code : manual) represented.add(key(code));
        List<String> result = new ArrayList<>();
        for (String code : TARGETS) {
            if (represented.add(key(code))) result.add(code);
        }
        return result;
    }

    private static String key(String raw) {
        if (raw == null) return "";
        String code = raw.trim().replace('_', '-').toLowerCase(Locale.ROOT);
        if (!code.matches("[a-z]{2,8}(-[a-z0-9]{2,8})*")) return "";
        String primary = code.split("-")[0];
        if (primary.equals("iw")) return "he";
        if (primary.equals("in")) return "id";
        if (primary.equals("ji")) return "yi";
        if (primary.equals("tl")) return "fil";
        // Keep Chinese script choices distinct, including common region aliases.
        if (primary.equals("zh")) {
            if (code.contains("-hant")) return "zh-hant";
            if (code.contains("-hans")) return "zh-hans";
            if (code.endsWith("-tw") || code.endsWith("-hk")) return "zh-hant";
            if (code.endsWith("-cn") || code.endsWith("-sg")) return "zh-hans";
            return code;
        }
        return primary;
    }
}
