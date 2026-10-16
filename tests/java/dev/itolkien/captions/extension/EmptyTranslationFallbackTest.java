package dev.itolkien.captions.extension;
import org.junit.jupiter.api.Test;
import java.util.*;
import dev.itolkien.captions.extension.CaptionTranslationList.CaptionTrack;
import static org.junit.jupiter.api.Assertions.*;

class EmptyTranslationFallbackTest {
    static final String URL = "https://www.youtube.com/api/timedtext?v=PlsODqqqwFs&lang=ar&kind=asr&sig=a%2Bb";
    static class Track extends CaptionIntegrationTest.Track {
        Track(String code, String url, String vss) { super(code, url, vss, code); }
    }
    static class Source implements EmptyTranslationFallback.CaptionSource {
        List<CaptionTrack> direct = List.of(new Track("DISABLE_CAPTIONS_OPTION", "", ""),
                new Track("ar", URL, "a.ar"));
        List<CaptionTrack> translations = List.of();
        List<String> targets = List.of();
        CaptionTrack preferred;
        public List<String> universalCaptions_targetLanguages(boolean unused) { return targets; }
        public CaptionTrack universalCaptions_defaultSource(boolean unused) { return preferred; }
        final CaptionTrack option = new Track("AUTO_TRANSLATE_CAPTIONS_OPTION", "", "");
        public List<CaptionTrack> universalCaptions_nativeDirect(boolean bypassFallback) { return direct; }
        public List<CaptionTrack> universalCaptions_nativeTranslated(boolean bypassFallback) { return translations; }
        public CaptionTrack universalCaptions_autoTranslateOption() { return option; }
    }
    @Test void missingMenuGetsNativeOptionWithoutChangingOriginalOrOffEntry() {
        Source source = new Source();
        List<CaptionTrack> result = EmptyTranslationFallback.direct(source.direct, source, Locale.ENGLISH);
        assertEquals(3, result.size());
        assertSame(source.direct.get(0), result.get(0));
        assertSame(source.direct.get(1), result.get(1));
        assertSame(source.option, result.get(2));
        assertEquals(2, source.direct.size());
    }
    @Test void emptyTranslationMenuUsesSameSignedArabicSourceForAllTargets() {
        Source source = new Source();
        List<CaptionTrack> result = EmptyTranslationFallback.translated(source.translations, source, Locale.ENGLISH);
        assertTrue(result.size() > 100);
        CaptionTrack tr = result.stream().filter(t -> "tr".equals(t.universalCaptions_getLanguageCode())).findFirst().orElseThrow();
        assertEquals(URL + "&tlang=tr", tr.universalCaptions_getCaptionUrl());
        assertEquals("ta.ar.tr", tr.universalCaptions_getVssId());
        assertEquals("Turkish", tr.universalCaptions_getCaptionLabel());
        assertFalse(result.stream().anyMatch(t -> "ar".equals(t.universalCaptions_getLanguageCode())));
        assertEquals(0, source.translations.size());
        assertEquals(URL, source.direct.get(1).universalCaptions_getCaptionUrl());
    }
    @Test void nonemptyNativeTranslationsAndExistingMenuRemainExactInputs() {
        Source source = new Source();
        source.translations = List.of(new Track("de", "invalid", "ta.ar.de"));
        assertSame(source.direct, EmptyTranslationFallback.direct(source.direct, source, Locale.ENGLISH));
        assertEquals(source.translations, EmptyTranslationFallback.translated(source.translations, source, Locale.ENGLISH));
        source.translations = List.of();
        source.direct = List.of(source.direct.get(0), source.direct.get(1), source.option);
        assertSame(source.direct, EmptyTranslationFallback.direct(source.direct, source, Locale.ENGLISH));
        assertTrue(EmptyTranslationFallback.translated(source.translations, source, Locale.ENGLISH).size() > 100);
    }
    @Test void multipleSourcesAndMalformedTracksAreUntouched() {
        Source source = new Source();
        for (List<CaptionTrack> tracks : List.of(
                List.<CaptionTrack>of(new Track("ar", URL.replace("PlsODqqqwFs", "short"), "a.ar")),
                List.<CaptionTrack>of(new Track("ar", URL, ".ar")),
                List.<CaptionTrack>of(new Track("ar", URL + "&tlang=de", "a.ar")),
                List.<CaptionTrack>of(new Track("en", URL, "a.en")),
                List.<CaptionTrack>of(new Track("ar", URL + "&v=otherVideo1", "a.ar")),
                List.<CaptionTrack>of(new Track("ar", URL.replace("&lang=ar", "&lang=en"), "a.ar")),
                List.<CaptionTrack>of(new Track("ar", URL.replace("&sig=", "&%74lang=de&sig="), "a.ar")),
                List.<CaptionTrack>of(new Track("ar", URL.replace("&kind=asr", ""), "a.ar")),
                List.<CaptionTrack>of(new Track("ar", URL, "a.ar"), new Track("ar", URL.replace("PlsODqqqwFs", "abcdefghijk"), "a.ar")),
                Collections.<CaptionTrack>emptyList())) {
            source.direct = tracks;
            assertSame(tracks, EmptyTranslationFallback.direct(tracks, source, Locale.ENGLISH));
            assertEquals(source.translations, EmptyTranslationFallback.translated(source.translations, source, Locale.ENGLISH));
        }
    }
    @Test void sourceIsPerManagerAndNativeIntegratedTracksCanUseOnlyTheExplicitFallback() {
        Source first = new Source();
        Source second = new Source();
        second.direct = List.of(new Track("ar", URL.replace("PlsODqqqwFs", "short"), "a.ar"));
        assertEquals(3, EmptyTranslationFallback.direct(first.direct, first, Locale.ENGLISH).size());
        assertSame(second.direct, EmptyTranslationFallback.direct(second.direct, second, Locale.ENGLISH));
        class NativeAsr extends Track implements app.morphe.extension.youtube.patches.AutoTranslateCaptionsPatch.CaptionTrackInterface {
            NativeAsr() { super("ar", URL, "a.ar"); }
        }
        first.direct = List.of(new NativeAsr());
        assertTrue(EmptyTranslationFallback.translated(first.translations, first, Locale.ENGLISH).size() > 100);
    }
    @Test void nullsAndFactoryFailuresFailClosed() {
        Source source = new Source();
        assertNull(EmptyTranslationFallback.direct(null, source, Locale.ENGLISH));
        assertNull(EmptyTranslationFallback.translated(null, source, Locale.ENGLISH));
        assertSame(source.direct, EmptyTranslationFallback.direct(source.direct, null, Locale.ENGLISH));
        Source broken = new Source() {
            public CaptionTrack universalCaptions_autoTranslateOption() { throw new IllegalStateException("native failure"); }
        };
        assertSame(broken.direct, EmptyTranslationFallback.direct(broken.direct, broken, Locale.ENGLISH));
    }
    @Test void namedEnglishSourceGetsNativeMenuAndTurkishTranslationWithoutAnAsrFlag() {
        Source source = new Source();
        String url = "https://www.youtube.com/api/timedtext?v=FlcePQVkmM4&lang=en&name=896wrPZbXJE&sig=a%2Bb";
        CaptionTrack english = new Track("en", url, ".en.78oJUrdTLnk");
        source.direct = List.of(source.direct.get(0), english);
        List<CaptionTrack> direct = EmptyTranslationFallback.direct(source.direct, source, Locale.ENGLISH);
        assertEquals(3, direct.size());
        assertSame(english, direct.get(1));
        assertSame(source.option, direct.get(2));
        List<CaptionTrack> translated = EmptyTranslationFallback.translated(source.translations, source, Locale.ENGLISH);
        CaptionTrack tr = translated.stream().filter(t -> "tr".equals(t.universalCaptions_getLanguageCode())).findFirst().orElseThrow();
        assertEquals(url + "&tlang=tr", tr.universalCaptions_getCaptionUrl());
        assertEquals("t.en.78oJUrdTLnk.tr", tr.universalCaptions_getVssId());
        assertFalse(translated.stream().anyMatch(t -> "en".equals(t.universalCaptions_getLanguageCode())));
    }

    @Test void singleSourcesInOtherLanguagesAndVideosNeedNoAllowlist() {
        Source source = new Source();
        for (String language : List.of("de", "hi", "ja", "fr", "zh-Hans")) {
            String url = "https://www.youtube.com/api/timedtext?v=abcdefghijk&lang=" + language + "&kind=asr&sig=a%2Bb";
            source.direct = List.of(new Track(language, url, "a." + language));
            assertEquals(2, EmptyTranslationFallback.direct(source.direct, source, Locale.ENGLISH).size());
            List<CaptionTrack> translations = EmptyTranslationFallback.translated(source.translations, source, Locale.ENGLISH);
            assertTrue(translations.size() > 100);
            assertFalse(translations.stream().anyMatch(t -> language.equals(t.universalCaptions_getLanguageCode())));
        }
    }

    @Test void validSourcesInDifferentManagersNeverReuseEachOthersUrls() {
        Source arabic = new Source();
        Source english = new Source();
        String url = "https://www.youtube.com/api/timedtext?v=FlcePQVkmM4&lang=en&name=source";
        english.direct = List.of(new Track("en", url, ".en.source"));
        List<CaptionTrack> first = EmptyTranslationFallback.translated(arabic.translations, arabic, Locale.ENGLISH);
        List<CaptionTrack> second = EmptyTranslationFallback.translated(english.translations, english, Locale.ENGLISH);
        assertTrue(first.stream().allMatch(t -> t.universalCaptions_getCaptionUrl().contains("v=PlsODqqqwFs&")));
        assertTrue(second.size() > 100);
        assertTrue(second.stream().allMatch(t -> t.universalCaptions_getCaptionUrl().contains("v=FlcePQVkmM4&")));
        assertTrue(first.stream().noneMatch(t -> t.universalCaptions_getCaptionUrl().contains("FlcePQVkmM4")));
    }

    @Test void nativeTargetCatalogIncludesNewAndRegionalCodesWithoutAnAllowlist() {
        Source source = new Source();
        source.targets = List.of("gaa", "pt", "pt-PT", "zh-Hans", "zh-Hant", "iw", "he", "bad&x=1");
        List<CaptionTrack> result = EmptyTranslationFallback.translated(source.translations, source, Locale.ENGLISH);
        assertEquals(Set.of("gaa", "pt", "pt-PT", "zh-Hans", "zh-Hant", "iw"),
                result.stream().map(CaptionTrack::universalCaptions_getLanguageCode).collect(java.util.stream.Collectors.toSet()));
    }
    @Test void multipleSourcesUseNativeDefaultAndRemainBoundToTheCurrentAudio() {
        Source source = new Source();
        CaptionTrack english = new Track("en", URL.replace("lang=ar&kind=asr", "lang=en"), ".en.upload");
        source.direct = List.of(source.direct.get(0), source.direct.get(1), english);
        source.preferred = english;
        source.targets = List.of("tr", "ja");
        List<CaptionTrack> result = EmptyTranslationFallback.translated(source.translations, source, Locale.ENGLISH);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(t -> t.universalCaptions_getCaptionUrl().contains("lang=en&")));
        assertEquals(4, EmptyTranslationFallback.direct(source.direct, source, Locale.ENGLISH).size());
        source.preferred = new Track("de", URL.replace("lang=ar", "lang=de"), "a.de");
        result = EmptyTranslationFallback.translated(source.translations, source, Locale.ENGLISH);
        assertTrue(result.stream().allMatch(t -> t.universalCaptions_getCaptionUrl().contains("lang=ar&")));
    }

    @Test void sourceOnlyOrInvalidCatalogCannotExposeAnEmptySubmenu() {
        Source source = new Source();
        for (List<String> targets : List.of(List.of("ar"), List.of("bad&query=1"))) {
            source.targets = targets;
            assertSame(source.direct, EmptyTranslationFallback.direct(source.direct, source, Locale.ENGLISH));
            assertSame(source.translations, EmptyTranslationFallback.translated(source.translations, source, Locale.ENGLISH));
        }
    }
    @Test void everySnapshotSourceAndTargetKeepsItsExactLanguageAndSignedIdentity() {
        for (String language : CaptionTranslationList.TARGETS) {
            for (boolean asr : List.of(false, true)) {
            for (Locale locale : List.of(Locale.ENGLISH, Locale.forLanguageTag("tr"), Locale.forLanguageTag("ar"), Locale.JAPANESE)) {
                Source source = new Source();
                String url = "https://www.youtube.com/api/timedtext?v=abcdefghijk&lang=" + language + (asr ? "&kind=asr" : "") + "&sig=a%2Bb";
                source.direct = List.of(new Track(language, url, (asr ? "a." : ".") + language));
                source.targets = Arrays.asList(CaptionTranslationList.TARGETS);
                List<CaptionTrack> result = EmptyTranslationFallback.translated(source.translations, source, locale);
                assertEquals(155, result.size(), language);
                assertEquals(155, result.stream().map(CaptionTrack::universalCaptions_getLanguageCode).distinct().count());
                for (CaptionTrack target : result) {
                    assertEquals(url + "&tlang=" + target.universalCaptions_getLanguageCode(), target.universalCaptions_getCaptionUrl());
                    assertEquals((asr ? "ta." : "t.") + language + "." + target.universalCaptions_getLanguageCode(), target.universalCaptions_getVssId());
                    assertFalse(target.universalCaptions_getCaptionLabel().toString().isBlank());
                }
            }
            }
        }
    }

}
