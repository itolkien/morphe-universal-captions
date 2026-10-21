/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3754
 * https://github.com/MorpheApp/morphe-patches/pull/3891
 * Adapted from https://github.com/itolkien/morphe-universal-captions
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.youtube.patches;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.requests.Requester;
import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class AutoTranslateCaptionsPatch {

    /**
     * Interface to use obfuscated methods.
     */
    public interface CaptionTrackInterface extends PreferredCaptionLanguagePatch.CaptionTrackInterface {
        // Methods are added during patching.

        String patch_getUrl();

        /**
         * @return A copy of this track with the given values.
         */
        CaptionTrackInterface patch_copyTranslation(String languageCode, String url,
                                                    String vssId, CharSequence label);
    }

    /** Native translation source for the currently active audio track. */
    public interface CaptionManagerInterface {
        PreferredCaptionLanguagePatch.CaptionTrackInterface patch_getActiveTranslationSource();
    }

    private static final Pattern LANGUAGE_CODE_PATTERN = Pattern.compile("[a-z]{2,8}(-[A-Za-z0-9]{2,8})*");
    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{11}");
    private static final Pattern VSS_ID_SUFFIX_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,128}");

    /**
     * Translation languages added if YouTube does not offer them.
     * Language codes are the codes YouTube uses for translations
     * (player translation languages of video FlcePQVkmM4, 2026-10-16).
     * Not all languages may be available for all videos.
     */
    private static final String[] TRANSLATION_LANGUAGES = {
            "aa", "ab", "af", "ak", "am", "ar", "as", "ay", "az", "ba", "be", "bg", "bho", "bn",
            "bo", "br", "bs", "ca", "ceb", "co", "crs", "cs", "cy", "da", "de", "dv", "dz", "ee",
            "el", "en", "eo", "es", "et", "eu", "fa", "fi", "fil", "fj", "fo", "fr", "fy", "ga",
            "gaa", "gd", "gl", "gn", "gu", "gv", "ha", "haw", "hi", "hmn", "hr", "ht", "hu", "hy",
            "id", "ig", "is", "it", "iu", "iw", "ja", "jv", "ka", "kha", "kk", "kl", "km", "kn",
            "ko", "kri", "ku", "ky", "la", "lb", "lg", "ln", "lo", "lt", "lua", "luo", "lv", "mfe",
            "mg", "mi", "mk", "ml", "mn", "mr", "ms", "mt", "my", "ne", "new", "nl", "no", "nso",
            "ny", "oc", "om", "or", "os", "pa", "pam", "pl", "ps", "pt", "pt-PT", "qu", "rn", "ro",
            "ru", "rw", "sa", "sd", "sg", "si", "sk", "sl", "sm", "sn", "so", "sq", "sr", "ss",
            "st", "su", "sv", "sw", "ta", "te", "tg", "th", "ti", "tk", "tn", "to", "tr", "ts",
            "tt", "tum", "ug", "uk", "ur", "uz", "ve", "vi", "war", "wo", "xh", "yi", "yo",
            "zh-Hans", "zh-Hant", "zu"
    };

    /** Extend the native list only when its translated tracks share one verified source. */
    static List<CaptionTrackInterface> addTranslationLanguages(List<CaptionTrackInterface> tracks,
                                                               Locale locale) {
        if (tracks == null || tracks.isEmpty() || locale == null) return tracks;
        CaptionTrackInterface template = tracks.get(0);
        String templateCode = template.patch_getLanguageCode();
        String vss = template.patch_getVssId();
        if (templateCode == null || !LANGUAGE_CODE_PATTERN.matcher(templateCode).matches()
                || vss == null || !vss.startsWith("t") || !vss.endsWith("." + templateCode)) return tracks;
        String templateUrl = template.patch_getUrl();
        int targetIndex = targetValueOffset(templateUrl, templateCode);
        String identity = translationSourceIdentity(template);
        if (targetIndex < 0 || identity == null) return tracks;
        for (CaptionTrackInterface track : tracks) {
            if (!identity.equals(translationSourceIdentity(track))) return tracks;
        }
        String sourceLanguage = captionParameters(templateUrl).get("lang");
        String vssIdPrefix = removeSuffix(vss, templateCode.length());
        String urlPrefix = templateUrl.substring(0, targetIndex);
        String urlSuffix = templateUrl.substring(targetIndex + templateCode.length());

        Set<String> existing = new HashSet<>();
        for (CaptionTrackInterface track : tracks) {
            existing.add(exactTargetKey(track.patch_getLanguageCode()));
        }
        List<CaptionTrackInterface> result = new ArrayList<>(tracks);
        String sourceKey = exactTargetKey(sourceLanguage);
        for (String code : TRANSLATION_LANGUAGES) {
            String key = exactTargetKey(code);
            if (key.equals(sourceKey) || existing.contains(key)) continue;
            CaptionTrackInterface translated = copyTranslation(template, code,
                    urlPrefix + code + urlSuffix, vssIdPrefix + code, locale);
            if (translated != null) {
                existing.add(key);
                result.add(translated);
            }
        }
        return result.size() == tracks.size() ? tracks : sortTranslationTracks(result, locale);
    }

    private static List<CaptionTrackInterface> sortTranslationTracks(List<CaptionTrackInterface> result, Locale locale) {
        Collator collator = Collator.getInstance(locale);
        collator.setStrength(Collator.SECONDARY);
        collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        // Track toString() is the displayed name.
        result.sort((a, b) -> collator.compare(a.toString(), b.toString()));

        return result;
    }

    /** Restore a missing Auto-translate row only for a verified source. */
    public static int getTranslationCount(int nativeCount,
            List<? extends PreferredCaptionLanguagePatch.CaptionTrackInterface> directTracks,
            CaptionManagerInterface manager) {
        try {
            // Preserve existing native menu counts without reading the source getter.
            if (nativeCount != 0 || !Settings.AUTO_TRANSLATE_ADDITIONAL_LANGUAGES.get()
                    || manager == null) return nativeCount;
            return restoredMenuCount(0, directTracks,
                    manager.patch_getActiveTranslationSource());
        } catch (RuntimeException | LinkageError ignored) {
            return nativeCount;
        }
    }

    static int restoredMenuCount(int nativeCount,
            List<? extends PreferredCaptionLanguagePatch.CaptionTrackInterface> directTracks,
            PreferredCaptionLanguagePatch.CaptionTrackInterface audioSource) {
        if (nativeCount != 0) return nativeCount;
        return findFallbackSource(directTracks, audioSource) == null ? 0 : 1;
    }

    /** Keep native active-audio source access inside Java's error boundary. */
    public static List<CaptionTrackInterface> restoreTranslationTracks(List<CaptionTrackInterface> tracks,
            List<? extends PreferredCaptionLanguagePatch.CaptionTrackInterface> directTracks,
            CaptionManagerInterface manager) {
        try {
            if (tracks == null || !Settings.AUTO_TRANSLATE_ADDITIONAL_LANGUAGES.get()) return tracks;
            return restoreTranslationTracksFromManager(tracks, directTracks, manager,
                    Requester.getAppLocale());
        } catch (RuntimeException ex) {
            Logger.printException(() -> "restoreTranslationTracks failure", ex);
            return tracks;
        } catch (LinkageError ignored) {
            return tracks;
        }
    }

    static List<CaptionTrackInterface> restoreTranslationTracksFromManager(
            List<CaptionTrackInterface> tracks,
            List<? extends PreferredCaptionLanguagePatch.CaptionTrackInterface> directTracks,
            CaptionManagerInterface manager, Locale locale) {
        if (tracks == null || manager == null || locale == null) return tracks;
        try {
            return restoreTranslationTracks(tracks, directTracks,
                    manager.patch_getActiveTranslationSource(), locale);
        } catch (RuntimeException | LinkageError ignored) {
            return tracks;
        }
    }

    static List<CaptionTrackInterface> restoreTranslationTracks(List<CaptionTrackInterface> tracks,
            List<? extends PreferredCaptionLanguagePatch.CaptionTrackInterface> directTracks,
            PreferredCaptionLanguagePatch.CaptionTrackInterface defaultSource, Locale locale) {
        if (tracks == null || locale == null) return tracks;
        CaptionTrackInterface source = findFallbackSource(directTracks, defaultSource);
        // A consistent native translated list can supply a template even without
        // a verified original; never clone mixed translation-source identities.
        if (source == null) return addTranslationLanguages(tracks, locale);
        String video = originalVideoId(source);
        // Existing target-specific native tracks are authoritative. Add only
        // missing choices from the source YouTube chose for the current audio.
        Set<String> represented = new HashSet<>();
        for (CaptionTrackInterface track : tracks) {
            if (translationSourceIdentity(track) == null) return tracks;
            Map<String, String> fields = captionParameters(track.patch_getUrl());
            if (fields == null || fields.get("v") != null && !video.equals(fields.get("v")))
                return tracks;
            represented.add(exactTargetKey(track.patch_getLanguageCode()));
        }
        // Validate the signed source once, not again for every one of 156 targets.
        String sourceLanguage = source.patch_getLanguageCode();
        String sourceUrl = source.patch_getUrl();
        int fragment = sourceUrl.indexOf('#');
        if (fragment < 0) fragment = sourceUrl.length();
        String urlPrefix = sourceUrl.substring(0, fragment) + "&tlang=";
        String urlSuffix = sourceUrl.substring(fragment);
        String vssPrefix = "t" + source.patch_getVssId() + ".";
        String sourceKey = exactTargetKey(sourceLanguage);
        List<CaptionTrackInterface> result = new ArrayList<>(tracks);
        for (String language : TRANSLATION_LANGUAGES) {
            String key = exactTargetKey(language);
            if (key.equals(sourceKey) || represented.contains(key)) continue;
            CaptionTrackInterface translated = copyTranslation(source, language,
                    urlPrefix + language + urlSuffix, vssPrefix + language, locale);
            if (translated != null) {
                represented.add(key);
                result.add(translated);
            }
        }
        return result.size() == tracks.size() ? tracks : sortTranslationTracks(result, locale);
    }

    /**
     * Prefer the source YouTube uses for translations (or its default caption track)
     * within this audio's originals, otherwise retain their order.
     */
    static CaptionTrackInterface findFallbackSource(
            List<? extends PreferredCaptionLanguagePatch.CaptionTrackInterface> tracks,
            PreferredCaptionLanguagePatch.CaptionTrackInterface defaultSource) {
        if (tracks == null) return null;
        CaptionTrackInterface first = null, preferred = null;
        String video = null;
        boolean mixed = false;
        for (PreferredCaptionLanguagePatch.CaptionTrackInterface track : tracks) {
            if (track == null) return null;
            String code = track.patch_getLanguageCode();
            if (PreferredCaptionLanguagePatch.DISABLE_OPTION.equals(code)
                    || PreferredCaptionLanguagePatch.AUTO_TRANSLATE_OPTION.equals(code)) continue;
            if (!(track instanceof CaptionTrackInterface candidate)) return null;
            String identity = originalVideoId(candidate);
            if (identity == null || video != null && !video.equals(identity)) return null;
            video = identity;
            if (first == null) first = candidate;
            else if (!first.patch_getVssId().equals(candidate.patch_getVssId())
                    || !first.patch_getUrl().equals(candidate.patch_getUrl())) mixed = true;
            if (defaultSource instanceof CaptionTrackInterface nativeDefault
                    && candidate.patch_getVssId().equals(nativeDefault.patch_getVssId())
                    && candidate.patch_getUrl().equals(nativeDefault.patch_getUrl())) preferred = candidate;
        }
        return preferred != null ? preferred : mixed ? null : first;
    }

    static String translationLanguage(String language) {
        if (language == null || !LANGUAGE_CODE_PATTERN.matcher(language).matches()) return null;
        for (String code : TRANSLATION_LANGUAGES) {
            if (code.equalsIgnoreCase(language)) return code;
        }
        for (String code : TRANSLATION_LANGUAGES) {
            if (getLanguageKey(code).equals(getLanguageKey(language))) return code;
        }
        return null;
    }

    static CaptionTrackInterface createTranslation(CaptionTrackInterface source, String language, Locale locale) {
        if (source == null || language == null || !LANGUAGE_CODE_PATTERN.matcher(language).matches()
                || originalVideoId(source) == null
                || exactTargetKey(language).equals(exactTargetKey(source.patch_getLanguageCode()))) return null;
        String url = source.patch_getUrl();
        int fragment = url.indexOf('#');
        if (fragment < 0) fragment = url.length();
        return copyTranslation(source, language,
                url.substring(0, fragment) + "&tlang=" + language + url.substring(fragment),
                "t" + source.patch_getVssId() + "." + language, locale);
    }

    /** The only place that creates a native translated track and its localized label. */
    private static CaptionTrackInterface copyTranslation(CaptionTrackInterface source,
            String language, String url, String vss, Locale locale) {
        String name = Locale.forLanguageTag(language).getDisplayName(locale);
        return source.patch_copyTranslation(language, url, vss, name.isEmpty() ? language : name);
    }

    /** Shared validation for original and translated caption URLs. */
    private static Map<String, String> captionParameters(String url) {
        if (url == null) return null;
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null
                    || !("youtube.com".equalsIgnoreCase(host)
                    || host.toLowerCase(Locale.ROOT).endsWith(".youtube.com"))
                    || uri.getUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443)
                    || !"/api/timedtext".equals(uri.getRawPath()) || uri.getRawQuery() == null) return null;
            Map<String, String> parameters = new HashMap<>();
            for (String part : uri.getRawQuery().split("&", -1)) {
                int split = part.indexOf('=');
                String rawKey = split < 0 ? part : part.substring(0, split);
                String key = URLDecoder.decode(rawKey, "UTF-8");
                if (!List.of("v", "lang", "kind", "name", "tlang").contains(key)) continue;
                if (!key.equals(rawKey) || parameters.containsKey(key)) return null;
                parameters.put(key, URLDecoder.decode(split < 0 ? "" : part.substring(split + 1), "UTF-8"));
            }
            return parameters;
        } catch (IllegalArgumentException | UnsupportedEncodingException ignored) {
            return null;
        }
    }

    private static String originalVideoId(CaptionTrackInterface source) {
        if (source == null) return null;
        String language = source.patch_getLanguageCode();
        if (language == null || !LANGUAGE_CODE_PATTERN.matcher(language).matches()) return null;
        String vss = source.patch_getVssId();
        boolean asr = originalVss(vss, "a." + language);
        if (!asr && !originalVss(vss, "." + language)) return null;
        Map<String, String> parameters = captionParameters(source.patch_getUrl());
        if (parameters == null || parameters.containsKey("tlang")) return null;
        String video = parameters.get("v"), sourceLanguage = parameters.get("lang");
        String kind = parameters.get("kind");
        return video != null && VIDEO_ID_PATTERN.matcher(video).matches()
                && sourceLanguage != null && LANGUAGE_CODE_PATTERN.matcher(sourceLanguage).matches()
                && getLanguageKey(language).equals(getLanguageKey(sourceLanguage))
                && (asr ? "asr".equals(kind) : kind == null || kind.isEmpty())
                ? video : null;
    }

    /** Reject target-specific source mixes even if their language labels match. */
    static String translationSourceIdentity(CaptionTrackInterface track) {
        if (track == null) return null;
        String code = track.patch_getLanguageCode();
        String vss = track.patch_getVssId();
        if (code == null || !LANGUAGE_CODE_PATTERN.matcher(code).matches()
                || vss == null || !vss.startsWith("t") || !vss.endsWith("." + code))
            return null;
        String url = track.patch_getUrl();
        Map<String, String> fields = captionParameters(url);
        if (fields == null || !code.equals(fields.get("tlang"))) return null;
        String lang = fields.get("lang"), video = fields.get("v");
        if (lang == null || !LANGUAGE_CODE_PATTERN.matcher(lang).matches()
                || video != null && !video.matches("[A-Za-z0-9_-]{1,128}")) return null;
        String sourceVss = vss.substring(1, vss.length() - code.length() - 1);
        if (!originalVss(sourceVss, "a." + lang) && !originalVss(sourceVss, "." + lang))
            return null;
        int target = targetValueOffset(url, code);
        return target < 0 ? null : sourceVss + "|" + url.substring(0, target)
                + '\0' + url.substring(target + code.length());
    }

    private static boolean originalVss(String value, String prefix) {
        return value != null && (value.equals(prefix) || value.startsWith(prefix + ".")
                && VSS_ID_SUFFIX_PATTERN.matcher(value.substring(prefix.length() + 1)).matches());
    }

    /**
     * Offset of the exact raw query target. An encoded value, duplicate key,
     * signed token or fragment can never be used as a replacement site.
     */
    private static int targetValueOffset(String url, String language) {
        if (url == null || language == null) return -1;
        int question = url.indexOf('?');
        if (question < 0) return -1;
        int fragment = url.indexOf('#', question + 1);
        int end = fragment < 0 ? url.length() : fragment, found = -1;
        for (int offset = question + 1; offset < end; ) {
            int next = url.indexOf('&', offset);
            if (next < 0 || next > end) next = end;
            String parameter = url.substring(offset, next);
            int equal = parameter.indexOf('=');
            String rawKey = equal < 0 ? parameter : parameter.substring(0, equal);
            try {
                if ("tlang".equals(URLDecoder.decode(rawKey, "UTF-8"))) {
                    if (found >= 0 || !rawKey.equals("tlang")
                            || !parameter.equals("tlang=" + language)) return -1;
                    found = offset + "tlang=".length();
                }
            } catch (IllegalArgumentException | UnsupportedEncodingException ignored) {
                return -1;
            }
            offset = next + 1;
        }
        return found;
    }

    private static String removeSuffix(String value, int suffixLength) {
        return value.substring(0, value.length() - suffixLength);
    }

    /** Target identity must retain region/script distinctions when expanding native lists. */
    private static String exactTargetKey(String raw) {
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

    /**
     * @return Key that is the same for all codes of a language.
     */
    private static String getLanguageKey(String languageCode) {
        String code = languageCode.replace('_', '-').toLowerCase(Locale.ROOT);
        String primary = code.split("-")[0];
        switch (primary) {
            // Legacy ISO 639 codes used by Java Locale and YouTube.
            case "iw": return "he";
            case "in": return "id";
            case "ji": return "yi";
            case "tl": return "fil";
            case "nb": return "no";
            // Traditional and Simplified Chinese are different translations.
            case "zh":
                // Script takes priority over region.
                if (code.contains("-hant")) return "zh-hant";
                if (code.contains("-hans")) return "zh-hans";
                if (code.endsWith("-tw") || code.endsWith("-hk")) return "zh-hant";
                if (code.endsWith("-cn") || code.endsWith("-sg")) return "zh-hans";
                return code;
            // YouTube translates to Brazilian (pt) and European (pt-PT) Portuguese.
            case "pt":
                return code.endsWith("-pt") ? "pt-pt" : "pt";
            default:
                return primary;
        }
    }
}
