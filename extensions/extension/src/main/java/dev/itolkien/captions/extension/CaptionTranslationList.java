/*
 * Copyright 2026 itolkien.
 * Licensed under GPL-3.0-or-later; see LICENSE.
 */

package dev.itolkien.captions.extension;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.function.Supplier;

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
    // Preserve native target entries; use snapshot codes only for compatible missing choices.
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
        // Keep the native catalog as the first source of valid target codes.
        return result;
    }

    /** Widen a proven source-compatible native catalog without replacing its native tracks.
     * Extra snapshot targets are best-effort: the server may reject an unsupported tlang.
     */
    static List<String> expandedTargets(List<String> nativeTargets,
            java.util.function.Predicate<String> sourceAllows) {
        List<String> nativeCodes = targets(nativeTargets);
        if (nativeTargets == null || nativeTargets.isEmpty() ||
                nativeCodes.stream().noneMatch(sourceAllows)) return nativeCodes;

        List<String> expanded = new ArrayList<>(nativeCodes);
        Set<String> seen = new HashSet<>();
        for (String code : nativeCodes) seen.add(languageKey(code));
        for (String code : TARGETS) {
            if (seen.add(languageKey(code))) expanded.add(code);
        }
        return expanded;
    }

    /**
     * Expand both nonempty native translation lists and empty menus in one path.
     * Prefer a validated native translation prototype; only consult the current
     * audio's original source when it cannot add missing languages.
     */
    static List<CaptionTrack> augment(List<CaptionTrack> tracks, Locale locale, List<String> nativeTargets,
            Supplier<OriginalCaptionSource> nativeOriginal) {
        if (tracks == null || locale == null || (tracks.isEmpty() && nativeOriginal == null)) return tracks;

        List<CaptionTrack> result = new ArrayList<>(tracks);
        Set<String> represented = new HashSet<>();
        CaptionTrack prototype = null;
        String sourceIdentity = null;
        boolean mixedSources = false;
        for (CaptionTrack track : tracks) {
            if (track == null) continue;
            represented.add(languageKey(track.universalCaptions_getLanguageCode()));
            String identity = translationSourceIdentity(track);
            if (identity == null) continue;
            if (sourceIdentity != null && !sourceIdentity.equals(identity)) {
                mixedSources = true;
                break;
            }
            if (prototype == null) {
                prototype = track;
                sourceIdentity = identity;
            }
        }

        if (!mixedSources && prototype != null) {
            String oldLanguage = prototype.universalCaptions_getLanguageCode();
            String oldUrl = prototype.universalCaptions_getCaptionUrl();
            String oldVss = prototype.universalCaptions_getVssId();
            TimedTextUrl url = TimedTextUrl.parse(oldUrl);
            int targetAt = url == null ? -1 : url.targetValueOffset(oldLanguage);
            if (targetAt >= 0) {
                String urlPrefix = oldUrl.substring(0, targetAt);
                String urlSuffix = oldUrl.substring(targetAt + oldLanguage.length());
                String vssPrefix = oldVss.substring(0, oldVss.length() - oldLanguage.length());
                String sourceKey = languageKey(url.field("lang"));
                for (String code : expandedTargets(nativeTargets,
                        candidate -> !sourceKey.equals(languageKey(candidate)))) {
                    String key = languageKey(code);
                    if (sourceKey.equals(key) || !represented.add(key)) continue;
                    String name = Locale.forLanguageTag(code).getDisplayName(locale);
                    if (name.isEmpty()) name = code;
                    result.add(prototype.universalCaptions_copyTranslation(
                            code, urlPrefix + code + urlSuffix, vssPrefix + code, name));
                }
            }
        }
        if (result.size() > tracks.size()) return sorted(result, locale);
        if (nativeOriginal == null) return sorted(result, locale);

        // Preserve every native target and fail closed if a native track is
        // malformed or comes from a different video than the selected original.
        OriginalCaptionSource selected = nativeOriginal.get();
        if (selected == null) return tracks;
        represented.clear();
        for (CaptionTrack track : tracks) {
            if (track == null || translationSourceIdentity(track) == null) return tracks;
            TimedTextUrl url = TimedTextUrl.parse(track.universalCaptions_getCaptionUrl());
            if (url == null || url.field("v") != null && !selected.videoId().equals(url.field("v")))
                return tracks;
            represented.add(languageKey(track.universalCaptions_getLanguageCode()));
        }

        for (String target : expandedTargets(nativeTargets, selected::canTranslate)) {
            if (represented.contains(languageKey(target))) continue;
            CaptionTrack translation = selected.translate(target, locale);
            if (translation != null) {
                result.add(translation);
                represented.add(languageKey(target));
            }
        }
        return result.isEmpty() ? tracks : sorted(result, locale);
    }

    /** Validate each native translated track and identify its original source and video. */
    /** Shared with caption styling: malformed translated sources must never bypass native pens. */
    static String translationSourceIdentity(CaptionTrack track) {
        String code = track.universalCaptions_getLanguageCode();
        String vss = track.universalCaptions_getVssId();
        if (!validLanguageCode(code) || vss == null ||
                !vss.startsWith("t") || !vss.endsWith("." + code)) return null;

        TimedTextUrl url = TimedTextUrl.parse(track.universalCaptions_getCaptionUrl());
        if (url == null || !code.equals(url.field("tlang"))) return null;
        String sourceUrl = url.sourceIdentity(code);
        if (sourceUrl == null) return null;
        String sourceLanguage = url.field("lang");
        if (!validLanguageCode(sourceLanguage)) return null;
        String video = url.field("v");
        if (video != null && !video.matches("[A-Za-z0-9_-]{1,128}")) return null;

        String originalVss = vss.substring(1, vss.length() - code.length() - 1);
        if (!OriginalCaptionSource.originalVss(originalVss, "a." + sourceLanguage)
                && !OriginalCaptionSource.originalVss(originalVss, "." + sourceLanguage))
            return null;
        return vss.substring(0, vss.length() - code.length()) + "|" + sourceUrl;
    }

    static String languageKey(String raw) {
        if (raw == null) return "";
        String code = raw.trim().replace('_', '-').toLowerCase(Locale.ROOT);
        int dash = code.indexOf('-');
        String primary = dash < 0 ? code : code.substring(0, dash);
        String region = dash < 0 ? "" : code.substring(dash);
        switch (primary) {
            case "iw": return "he" + region;
            case "in": return "id" + region;
            case "ji": return "yi" + region;
            case "tl": return "fil" + region;
            case "nb": return "no" + region;
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
