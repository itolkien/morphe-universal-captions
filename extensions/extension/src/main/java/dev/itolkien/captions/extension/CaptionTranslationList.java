/*
 * Copyright 2026 itolkien.
 * Licensed under GPL-3.0-or-later; see LICENSE.
 */

package dev.itolkien.captions.extension;

import java.net.URI;
import java.net.URLDecoder;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Extends a native translation list without selecting a caption track or changing caption visibility. */
public final class CaptionTranslationList {
    private CaptionTranslationList() { }

    /** Implemented by the native track class at patch time. */
    public interface CaptionTrack {
        String universalCaptions_getLanguageCode();
        String universalCaptions_getVssId();
        String universalCaptions_getCaptionUrl();
        CharSequence universalCaptions_getCaptionLabel();
        CaptionTrack universalCaptions_copyTranslation(String language, String url, String vss, CharSequence label);
    }

    private static final Pattern LANGUAGE_CODE = Pattern.compile("[a-z]{2,8}(-[A-Za-z0-9]{2,8})*");

    // YouTube player translationLanguages snapshot, 2026-10-16, video FlcePQVkmM4.
    // Used only when native metadata is absent; runtime metadata takes precedence.
    static final String[] TARGETS = "ab aa af ak sq am ar hy as ay az bn ba eu be bho bs br bg my ca ceb zh-Hans zh-Hant co hr cs da dv nl dz en eo et ee fo fj fil fi fr gaa gl lg ka de el gn gu ht ha haw iw hi hmn hu is ig id iu ga it ja jv kl kn kk kha km rw ko kri ku ky lo la lv ln lt lua luo lb mk mg ms ml mt gv mi mr mn mfe ne new nso no ny oc or om os pam ps fa pl pt pt-PT pa qu ro rn ru sm sg sa gd sr crs sn sd si sk sl so st es su sw ss sv tg ta tt te th bo ti to ts tn tum tr tk uk ur ug uz ve vi war cy fy wo xh yi yo zu".split(" ");

    static boolean validLanguageCode(String code) {
        return code != null && LANGUAGE_CODE.matcher(code).matches();
    }

    static List<String> targets(List<String> nativeTargets) {
        if (nativeTargets == null || nativeTargets.isEmpty()) return java.util.Arrays.asList(TARGETS);
        List<String> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String code : nativeTargets) {
            if (validLanguageCode(code) && seen.add(languageKey(code))) result.add(code);
        }
        // A nonempty native catalog is authoritative, even when every entry is invalid.
        return result;
    }

    public static List<CaptionTrack> augment(List<CaptionTrack> tracks, Locale locale) {
        return augment(tracks, locale, null);
    }

    static List<CaptionTrack> augment(List<CaptionTrack> tracks, Locale locale, List<String> nativeTargets) {
        if (tracks == null || tracks.isEmpty() || locale == null) return tracks;
        List<CaptionTrack> result = new ArrayList<>(tracks);
        Set<String> represented = new HashSet<>();
        CaptionTrack prototype = null;
        for (CaptionTrack track : tracks) {
            if (track == null) continue;
            represented.add(languageKey(track.universalCaptions_getLanguageCode()));
            if (prototype == null && isTranslationPrototype(track)) prototype = track;
        }
        if (prototype != null) {
            String oldLanguage = prototype.universalCaptions_getLanguageCode();
            String oldUrl = prototype.universalCaptions_getCaptionUrl();
            String oldVss = prototype.universalCaptions_getVssId();
            String vssPrefix = oldVss.substring(0, oldVss.length() - oldLanguage.length());

            // Find the actual query parameter, not "tlang=" inside an opaque value or fragment.
            int queryStart = oldUrl.indexOf('?') + 1;
            int queryEnd = oldUrl.indexOf('#', queryStart);
            if (queryEnd < 0) queryEnd = oldUrl.length();
            int offset = queryStart, targetAt = -1;
            for (String part : oldUrl.substring(queryStart, queryEnd).split("&", -1)) {
                if (part.equals("tlang=" + oldLanguage)) {
                    targetAt = offset + "tlang=".length();
                    break;
                }
                offset += part.length() + 1;
            }
            if (targetAt < 0) return sorted(result, locale);
            String urlPrefix = oldUrl.substring(0, targetAt);
            String urlSuffix = oldUrl.substring(targetAt + oldLanguage.length());

            for (String code : targets(nativeTargets)) {
                if (!represented.add(languageKey(code))) continue;
                String name = Locale.forLanguageTag(code).getDisplayName(locale);
                if (name.isEmpty()) name = code;
                result.add(prototype.universalCaptions_copyTranslation(
                        code, urlPrefix + code + urlSuffix, vssPrefix + code, name));
            }
        }
        return sorted(result, locale);
    }

    static List<CaptionTrack> fromSource(OriginalCaptionSource source, Locale locale, List<String> nativeTargets) {
        List<CaptionTrack> result = new ArrayList<>();
        if (locale == null || source == null) return result;
        for (String target : targets(nativeTargets)) {
            CaptionTrack translated = source.translate(target, locale);
            if (translated != null) result.add(translated);
        }
        return sorted(result, locale);
    }

    private static boolean isTranslationPrototype(CaptionTrack track) {
        String code = track.universalCaptions_getLanguageCode();
        String vss = track.universalCaptions_getVssId();
        if (!validLanguageCode(code) ||
                vss == null || !vss.startsWith("t") || !vss.endsWith("." + code)) return false;
        try {
            URI uri = OriginalCaptionSource.timedTextUri(track.universalCaptions_getCaptionUrl());
            if (uri == null) return false;
            int languageParameters = 0;
            for (String parameter : uri.getRawQuery().split("&", -1)) {
                int equals = parameter.indexOf('=');
                String rawKey = equals < 0 ? parameter : parameter.substring(0, equals);
                if (!"tlang".equals(URLDecoder.decode(rawKey, "UTF-8"))) continue;
                if (!"tlang".equals(rawKey) || !parameter.equals("tlang=" + code)) return false;
                languageParameters++;
            }
            return languageParameters == 1;
        } catch (IllegalArgumentException | NullPointerException | java.io.UnsupportedEncodingException ignored) {
            return false;
        }
    }

    static String languageKey(String raw) {
        if (raw == null) return "";
        String code = raw.trim().replace('_', '-').toLowerCase(Locale.ROOT);
        int dash = code.indexOf('-');
        String primary = dash < 0 ? code : code.substring(0, dash);
        switch (primary) {
            case "iw": return "he";
            case "in": return "id";
            case "ji": return "yi";
            case "tl": return "fil";
            case "nb": return "no";
            case "zh":
                if (code.contains("-hant")) return "zh-hant";
                if (code.contains("-hans")) return "zh-hans";
                if (code.endsWith("-tw") || code.endsWith("-hk")) return "zh-hant";
                if (code.endsWith("-cn") || code.endsWith("-sg")) return "zh-hans";
                return code;
            default: return code;
        }
    }

    private static List<CaptionTrack> sorted(List<CaptionTrack> tracks, Locale locale) {
        List<Integer> positions = new ArrayList<>();
        List<CaptionTrack> languages = new ArrayList<>();
        for (int i = 0; i < tracks.size(); i++) {
            CaptionTrack track = tracks.get(i);
            if (track == null) continue;
            String code = track.universalCaptions_getLanguageCode();
            CharSequence label = track.universalCaptions_getCaptionLabel();
            if (code == null || code.isEmpty() || code.endsWith("_OPTION") ||
                    label == null || label.toString().trim().isEmpty()) continue;
            positions.add(i);
            languages.add(track);
        }
        Collator collator = Collator.getInstance(locale);
        collator.setStrength(Collator.SECONDARY);
        collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        // Stable sorting preserves the native order of equivalent display names.
        languages.sort((a, b) -> collator.compare(a.universalCaptions_getCaptionLabel().toString().trim(),
                b.universalCaptions_getCaptionLabel().toString().trim()));
        for (int i = 0; i < positions.size(); i++) tracks.set(positions.get(i), languages.get(i));
        return tracks;
    }
}
