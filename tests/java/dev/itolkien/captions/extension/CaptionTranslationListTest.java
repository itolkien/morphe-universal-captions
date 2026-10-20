package dev.itolkien.captions.extension;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

public class CaptionTranslationListTest {
    private static final String URL = "https://www.youtube.com/api/timedtext?v=video&lang=en&sig=a%2Bb&tlang=de&fmt=srv3";

    private static final class Track implements CaptionTranslationList.CaptionTrack {
        final String code, url, vss;
        final CharSequence label;

        Track(String code, String url, String vss, CharSequence label) {
            this.code = code;
            this.url = url;
            this.vss = vss;
            this.label = label;
        }

        @Override public String universalCaptions_getLanguageCode() { return code; }
        @Override public String universalCaptions_getVssId() { return vss; }
        @Override public String universalCaptions_getCaptionUrl() { return url; }
        @Override public CharSequence universalCaptions_getCaptionLabel() { return label; }
        @Override public CaptionTranslationList.CaptionTrack universalCaptions_copyTranslation(
                String language, String url, String vss, CharSequence label) {
            return new Track(language, url, vss, label);
        }
    }

    private Track german() { return new Track("de", URL, "ta.en.de", "German"); }

    private CaptionTranslationList.CaptionTrack language(List<CaptionTranslationList.CaptionTrack> tracks, String code) {
        return tracks.stream().filter(t -> t != null && code.equals(t.universalCaptions_getLanguageCode()))
                .findFirst().orElseThrow(AssertionError::new);
    }

    @Test public void smallNativeCatalogStillOffersAdditionalTargetsWhenSourceIsKnown() {
        Track nativeGerman = german();
        List<CaptionTranslationList.CaptionTrack> expanded = CaptionTranslationList.augment(
                List.of(nativeGerman), Locale.ENGLISH, List.of("de", "fr", "ru"));
        assertTrue(expanded.size() > 100, "Native 3-language catalog must not cap user-enabled addon");
        assertSame(nativeGerman, language(expanded, "de"));
        assertEquals("Turkish", language(expanded, "tr").universalCaptions_getCaptionLabel());
        assertFalse(expanded.stream().anyMatch(track ->
                "en".equals(track.universalCaptions_getLanguageCode())));
    }

    @Test public void neverReusesTheOriginalLanguageAsATranslationTarget() {
        // The native list is translated from English to German; the available
        // target catalog also contains English, which must remain an original.
        List<CaptionTranslationList.CaptionTrack> result = CaptionTranslationList.augment(
                List.of(german()), Locale.ENGLISH, List.of("de", "en", "el"));
        assertTrue(result.size() > 100);
        assertEquals("de", result.get(0).universalCaptions_getLanguageCode());
        assertEquals("el", result.get(1).universalCaptions_getLanguageCode());
        assertFalse(result.stream().anyMatch(t -> "en".equals(t.universalCaptions_getLanguageCode())));

        // Legacy aliases must not generate a self-translation either.
        Track fromLegacyHebrew = new Track("tr", URL.replace("lang=en", "lang=iw").replace("tlang=de", "tlang=tr"),
                "ta.iw.tr", "Turkish");
        var aliasResult = CaptionTranslationList.augment(
                List.of(fromLegacyHebrew), Locale.ENGLISH, List.of("he", "tr", "fr"));
        assertTrue(aliasResult.size() > 100);
        assertFalse(aliasResult.stream().anyMatch(t -> "he".equals(t.universalCaptions_getLanguageCode())));
    }

    @Test public void addsGreekUsingTheSameSignedSourceAndKeepsNativeObjects() {
        Track nativeGerman = german();
        List<CaptionTranslationList.CaptionTrack> input = List.of(nativeGerman);
        List<CaptionTranslationList.CaptionTrack> result = CaptionTranslationList.augment(input, Locale.ENGLISH);
        CaptionTranslationList.CaptionTrack greek = language(result, "el");
        assertEquals("https://www.youtube.com/api/timedtext?v=video&lang=en&sig=a%2Bb&tlang=el&fmt=srv3", greek.universalCaptions_getCaptionUrl());
        assertEquals("ta.en.el", greek.universalCaptions_getVssId());
        assertEquals("Greek", greek.universalCaptions_getCaptionLabel());
        assertSame(nativeGerman, language(result, "de"));
        assertEquals(List.of(nativeGerman), input);
    }

    @Test public void legacyLanguageAliasesPreserveRegionalSubtags() {
        assertEquals("he", CaptionTranslationList.languageKey("iw"));
        assertEquals("he-il", CaptionTranslationList.languageKey("iw-IL"));
        assertEquals("id-id", CaptionTranslationList.languageKey("in-ID"));
        assertEquals("yi-001", CaptionTranslationList.languageKey("ji-001"));
        assertEquals("fil-ph", CaptionTranslationList.languageKey("tl-PH"));
        assertEquals("no-no", CaptionTranslationList.languageKey("nb-NO"));
        assertNotEquals(CaptionTranslationList.languageKey("iw-IL"),
                CaptionTranslationList.languageKey("iw-US"));
    }

    @Test public void doesNotDuplicateAliasesOrCollapseChineseScripts() {
        List<CaptionTranslationList.CaptionTrack> result = CaptionTranslationList.augment(List.of(
                german(), new Track("iw", "", "", "Hebrew"),
                new Track("zh-Hans-TW", "", "", "Chinese (Simplified)")
        ), Locale.ENGLISH);
        assertFalse(result.stream().anyMatch(t -> "he".equals(t.universalCaptions_getLanguageCode())));
        assertFalse(result.stream().anyMatch(t -> "zh-Hans".equals(t.universalCaptions_getLanguageCode())));
        assertNotNull(language(result, "zh-Hant"));
        assertEquals(1, result.stream().filter(t -> "iw".equals(t.universalCaptions_getLanguageCode())).count());
    }

    @Test public void sortsDisplayedLabelsAndLeavesSpecialEntriesInPlace() {
        Track off = new Track("DISABLE_CAPTIONS_OPTION", "", "-", "Off");
        Track unknown = new Track("", "", "", "Unknown");
        Track zulu = new Track("zu", "", "", "Zulu");
        Track armenian = new Track("hy", "", "", "Armenian");
        Track german = new Track("de", "", "", "German");
        List<CaptionTranslationList.CaptionTrack> input = Arrays.asList(off, zulu, unknown, german, null, armenian);
        List<CaptionTranslationList.CaptionTrack> result = CaptionTranslationList.augment(input, Locale.ENGLISH);
        assertEquals(Arrays.asList(off, armenian, unknown, german, null, zulu), result);
        assertSame(zulu, input.get(1));
    }

    @Test public void usesLocaleCollationAndStableOrderForEquivalentLabels() {
        Track z = new Track("zu", "", "", "Zulu");
        Track a = new Track("af", "", "", "Älg");
        Track same = new Track("am", "", "", "älg");
        assertEquals(List.of(z, a, same), CaptionTranslationList.augment(List.of(a, same, z), Locale.forLanguageTag("sv")));
        assertEquals(List.of(a, same, z), CaptionTranslationList.augment(List.of(z, a, same), Locale.GERMAN));
    }

    @Test public void doesNotInventTranslationSupportForEmptyOrNonTranslatedLists() {
        assertNull(CaptionTranslationList.augment(null, Locale.ENGLISH));
        List<CaptionTranslationList.CaptionTrack> empty = List.of();
        assertSame(empty, CaptionTranslationList.augment(empty, Locale.ENGLISH));
        List<CaptionTranslationList.CaptionTrack> direct = List.of(new Track("en", URL.replace("&tlang=de", ""), ".en", "English"));
        assertEquals(direct, CaptionTranslationList.augment(direct, Locale.ENGLISH));
    }

    @Test public void absentOrEmptyProtoCatalogUsesTheCompatibilitySnapshot() {
        assertEquals(156, CaptionTranslationList.targets(null).size());
        assertEquals(156, CaptionTranslationList.targets(List.of()).size());

        List<CaptionTranslationList.CaptionTrack> input = List.of(german());
        assertTrue(CaptionTranslationList.augment(input, Locale.ENGLISH, List.of()).size() > 100);
        assertTrue(CaptionTranslationList.augment(input, Locale.ENGLISH, null).size() > 100);
        assertTrue(CaptionTranslationList.targets(List.of("invalid&code")).isEmpty());
    }

    @Test public void translatedPrototypeMustMatchItsOriginalSourceLanguage() {
        Track mismatched = new Track("de", URL.replace("lang=en", "lang=fr"), "ta.en.de", "German");
        assertEquals(List.of(mismatched),
                CaptionTranslationList.augment(List.of(mismatched), Locale.ENGLISH));

        Track duplicate = new Track("de", URL + "&lang=fr", "ta.en.de", "German");
        assertEquals(List.of(duplicate),
                CaptionTranslationList.augment(List.of(duplicate), Locale.ENGLISH));
    }

    @Test public void rejectsUntrustedUrlsAndInconsistentTrackIdentities() {
        for (String url : List.of(URL.replace("https:", "http:"),
                URL.replace("www.youtube.com", "youtube.com.evil.test"),
                URL.replace("/api/timedtext", "/other"), URL + "&tlang=de",
                URL.replace("tlang=de", "tlang=fr"))) {
            Track track = new Track("de", url, "ta.en.de", "German");
            assertEquals(List.of(track), CaptionTranslationList.augment(List.of(track), Locale.ENGLISH));
        }
        Track wrongVss = new Track("de", URL, ".en", "German");
        assertEquals(List.of(wrongVss), CaptionTranslationList.augment(List.of(wrongVss), Locale.ENGLISH));
    }

    @Test public void nativeTranslationsFromDifferentVideosAreNotMixed() {
        Track first = german();
        Track second = new Track("fr",
                URL.replace("v=video", "v=otherVideo").replace("tlang=de", "tlang=fr"),
                "ta.en.fr", "French");
        List<CaptionTranslationList.CaptionTrack> nativeTracks = List.of(second, first);
        List<CaptionTranslationList.CaptionTrack> result =
                CaptionTranslationList.augment(nativeTracks, Locale.ENGLISH, List.of("el"));
        assertEquals(2, result.size());
        assertTrue(result.contains(first));
        assertTrue(result.contains(second));
    }

    @Test public void duplicateNativeVideoKeysAreNotSafePrototypes() {
        Track duplicate = new Track("de", URL + "&v=secondVideo", "ta.en.de", "German");
        assertEquals(List.of(duplicate),
                CaptionTranslationList.augment(List.of(duplicate), Locale.ENGLISH));
    }

    @Test public void sameVssAndVideoWithDifferentNativeBaseUrlsDoNotMixSources() {
        Track german = german();
        Track french = new Track("fr", URL.replace("tlang=de", "tlang=fr") + "&name=other",
                "ta.en.fr", "French");
        List<CaptionTranslationList.CaptionTrack> nativeTracks = List.of(german, french);
        var result = CaptionTranslationList.augment(nativeTracks, Locale.ENGLISH, List.of("tr"));
        assertEquals(2, result.size());
        assertTrue(result.contains(german));
        assertTrue(result.contains(french));
    }

    @Test public void nativeTargetsFromExactlyTheSameBaseUrlCanStillExpand() {
        Track german = german();
        Track french = new Track("fr", URL.replace("tlang=de", "tlang=fr"),
                "ta.en.fr", "French");
        var result = CaptionTranslationList.augment(
                List.of(german, french), Locale.ENGLISH, List.of("tr"));
        assertTrue(result.size() > 100);
        assertEquals(URL.replace("tlang=de", "tlang=tr"),
                language(result, "tr").universalCaptions_getCaptionUrl());
    }

    @Test public void mixedNativeSourceLanguagesAreNotArbitrarilyExpanded() {
        Track englishSource = german();
        Track spanishSource = new Track("fr",
                URL.replace("lang=en", "lang=es").replace("tlang=de", "tlang=fr"),
                "ta.es.fr", "French");
        List<CaptionTranslationList.CaptionTrack> nativeTracks = List.of(spanishSource, englishSource);
        List<CaptionTranslationList.CaptionTrack> result =
                CaptionTranslationList.augment(nativeTracks, Locale.ENGLISH, List.of("el"));
        assertEquals(2, result.size());
        assertTrue(result.contains(englishSource));
        assertTrue(result.contains(spanishSource));
    }

    @Test public void nullLocaleKeepsNativeTracksUntouched() {
        List<CaptionTranslationList.CaptionTrack> nativeTracks = List.of(german());
        assertSame(nativeTracks, CaptionTranslationList.augment(nativeTracks, null));
    }

    @Test public void rejectsEncodedOrMalformedDuplicateTranslationParameters() {
        // URI.getRawQuery() preserves encoded keys. An additional encoded tlang must
        // not be overlooked when we clone an otherwise valid native translation.
        for (String suffix : List.of("&%74lang=fr", "&tlang", "&%74lang")) {
            Track track = new Track("de", URL + suffix, "ta.en.de", "German");
            List<CaptionTranslationList.CaptionTrack> input = List.of(track);
            assertEquals(input, CaptionTranslationList.augment(input, Locale.ENGLISH),
                    "unexpectedly augmented: " + suffix);
        }
    }

    @Test public void expandingTwiceDoesNotAddDuplicatesOrChangeNativeIdentities() {
        List<CaptionTranslationList.CaptionTrack> once = CaptionTranslationList.augment(List.of(german()), Locale.ENGLISH);
        List<CaptionTranslationList.CaptionTrack> twice = CaptionTranslationList.augment(once, Locale.ENGLISH);
        assertEquals(once, twice);
        List<CaptionTranslationList.CaptionTrack> sorted = new ArrayList<>(once);
        sorted.sort((a, b) -> java.text.Collator.getInstance(Locale.ENGLISH).compare(a.universalCaptions_getCaptionLabel(), b.universalCaptions_getCaptionLabel()));
        assertEquals(sorted, once);
    }

    @Test public void onlyReplacesTheTargetQueryParameter() {
        Track track = new Track("de", URL + "&opaque=tlang=de#tlang=de", "ta.en.de", "German");
        CaptionTranslationList.CaptionTrack greek = language(CaptionTranslationList.augment(List.of(track), Locale.ENGLISH), "el");
        assertEquals("https://www.youtube.com/api/timedtext?v=video&lang=en&sig=a%2Bb&tlang=el&fmt=srv3&opaque=tlang=de#tlang=de", greek.universalCaptions_getCaptionUrl());
    }

    @Test public void aQuestionMarkInsideAnOpaqueValueIsNotATargetParameter() {
        Track track = new Track("de", URL.replace("&lang=en", "&opaque=?tlang=de&lang=en"), "ta.en.de", "German");
        CaptionTranslationList.CaptionTrack greek = language(CaptionTranslationList.augment(List.of(track), Locale.ENGLISH), "el");
        assertEquals("https://www.youtube.com/api/timedtext?v=video&opaque=?tlang=de&lang=en&sig=a%2Bb&tlang=el&fmt=srv3", greek.universalCaptions_getCaptionUrl());
    }
}
