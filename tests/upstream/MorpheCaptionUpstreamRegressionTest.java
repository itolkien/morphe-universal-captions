/*
 * Copyright 2026.
 * Caption-only regression tests against the unmodified official Morphe runtime.
 * Installed into the temporary CI checkout's test directory, never production.
 */
package app.morphe.extension.youtube.patches;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

public final class MorpheCaptionUpstreamRegressionTest {
    private static final String BASE =
            "https://www.youtube.com/api/timedtext?v=abcdefghijk&lang=en&sig=a%2Bb";

    private static final class Track implements AutoTranslateCaptionsPatch.CaptionTrackInterface {
        private final String code;
        private final String url;
        private final String vss;

        private Track(String code, String url, String vss) {
            this.code = code;
            this.url = url;
            this.vss = vss;
        }

        @Override public String patch_getLanguageCode() { return code; }
        @Override public String patch_getUrl() { return url; }
        @Override public String patch_getVssId() { return vss; }
        @Override public Track patch_copyTranslation(
                String languageCode, String newUrl, String vssId, CharSequence label) {
            return new Track(languageCode, newUrl, vssId);
        }
        @Override public String toString() { return code; }
    }

    private static Track translated(String target, String url) {
        return new Track(target, url, "ta.en." + target);
    }

    private static List<AutoTranslateCaptionsPatch.CaptionTrackInterface> add(List<Track> tracks) {
        return AutoTranslateCaptionsPatch.addTranslationLanguages(
                new ArrayList<>(tracks), Locale.ENGLISH);
    }

    private static AutoTranslateCaptionsPatch.CaptionTrackInterface find(
            List<AutoTranslateCaptionsPatch.CaptionTrackInterface> tracks, String target) {
        for (AutoTranslateCaptionsPatch.CaptionTrackInterface track : tracks) {
            if (target.equals(track.patch_getLanguageCode())) return track;
        }
        throw new AssertionError("Missing target: " + target);
    }

    @Test public void opaqueSignedSuffixAndFragmentSurviveTranslationClone() {
        String original = BASE + "&tlang=de&fmt=srv3&token=x%2Fy%3D#opaque&tlang=ru";
        List<AutoTranslateCaptionsPatch.CaptionTrackInterface> expanded =
                add(List.of(translated("de", original)));
        assertEquals(BASE + "&tlang=tr&fmt=srv3&token=x%2Fy%3D#opaque&tlang=ru",
                find(expanded, "tr").patch_getUrl());
    }

    @Test public void mixedSignaturesDoNotProduceSyntheticTracks() {
        Track german = translated("de", BASE + "&tlang=de");
        Track french = translated("fr", BASE.replace("sig=a%2Bb", "sig=different") + "&tlang=fr");
        List<AutoTranslateCaptionsPatch.CaptionTrackInterface> originals =
                new ArrayList<>(List.of(german, french));
        assertSame(originals, AutoTranslateCaptionsPatch.addTranslationLanguages(
                originals, Locale.ENGLISH));
        assertEquals(2, originals.size());
    }

    @Test public void nativeUnknownLanguageRemainsAlongsideWebLanguages() {
        Track nativeOnly = translated("qaa", BASE + "&tlang=qaa");
        List<AutoTranslateCaptionsPatch.CaptionTrackInterface> result = add(List.of(nativeOnly));
        assertSame(nativeOnly, find(result, "qaa"));
        find(result, "tr");
        assertEquals(156, result.size());
    }

    @Test public void malformedTargetParameterCannotBeUsedAsTemplate() {
        for (String unsafe : List.of(
                BASE + "&tlang=de&tlang=de",
                BASE + "&%74lang=de",
                BASE + "#fragment&tlang=de",
                BASE + "&tlang=de-DE",
                BASE.replace("https://", "http://") + "&tlang=de",
                BASE.replace("youtube.com", "youtube.com.evil.test") + "&tlang=de")) {
            List<AutoTranslateCaptionsPatch.CaptionTrackInterface> original =
                    new ArrayList<>(List.of(translated("de", unsafe)));
            assertSame(unsafe, original,
                    AutoTranslateCaptionsPatch.addTranslationLanguages(original, Locale.ENGLISH));
        }
    }

    @Test public void aliasesNeverTranslateAnOriginalLanguageIntoItself() {
        String url = BASE.replace("lang=en", "lang=iw") + "&tlang=de";
        Track german = new Track("de", url, "ta.iw.de");
        List<AutoTranslateCaptionsPatch.CaptionTrackInterface> expanded =
                add(List.of(german));
        assertFalse(expanded.stream().anyMatch(t -> "iw".equals(t.patch_getLanguageCode())));
        find(expanded, "tr");
    }

    @Test public void originalCaptionFromAnotherVideoIsNotAValidSource() {
        Track english = new Track("en", BASE, ".en");
        Track anotherVideo = new Track("fr",
                "https://www.youtube.com/api/timedtext?v=zyxwvutsrqp&lang=fr&kind=asr",
                "a.fr");
        assertNull(AutoTranslateCaptionsPatch.findFallbackSource(
                List.of(english, anotherVideo), null));
    }

    @Test public void invalidCaptionVssCannotBecomeACloneTemplate() {
        Track malformed = new Track("de", BASE + "&tlang=de", "ta.fr.de");
        List<AutoTranslateCaptionsPatch.CaptionTrackInterface> originals =
                new ArrayList<>(List.of(malformed));
        assertSame(originals, AutoTranslateCaptionsPatch.addTranslationLanguages(
                originals, Locale.ENGLISH));
    }

    @Test public void nativeStyleGetterFailureDoesNotLeakTranslatedPen() {
        TranslatedCaptionPensPatch.setParsedRequest(
                (TranslatedCaptionPensPatch.CaptionRequestInterface) () ->
                        translated("tr", BASE + "&tlang=tr"));
        assertEquals(Integer.MIN_VALUE, TranslatedCaptionPensPatch.getPenId(9));

        PreferredCaptionLanguagePatch.CaptionTrackInterface badTrack =
                new PreferredCaptionLanguagePatch.CaptionTrackInterface() {
                    @Override public String patch_getLanguageCode() { return "tr"; }
                    @Override public String patch_getVssId() {
                        throw new IllegalStateException("native getter failed");
                    }
                };
        TranslatedCaptionPensPatch.setParsedRequest(
                (TranslatedCaptionPensPatch.CaptionRequestInterface) () -> badTrack);
        assertEquals(9, TranslatedCaptionPensPatch.getPenId(9));
    }
}
