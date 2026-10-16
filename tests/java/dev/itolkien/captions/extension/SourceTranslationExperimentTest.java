package dev.itolkien.captions.extension;

import org.junit.jupiter.api.Test;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;

class SourceTranslationExperimentTest {
    private static final class Track implements CaptionTranslationList.CaptionTrack {
        final String lang, url, vss;
        final CharSequence label;
        Track(String lang, String url, String vss, CharSequence label) {
            this.lang = lang; this.url = url; this.vss = vss; this.label = label;
        }
        public String universalCaptions_getLanguageCode() { return lang; }
        public String universalCaptions_getVssId() { return vss; }
        public String universalCaptions_getCaptionUrl() { return url; }
        public CharSequence universalCaptions_getCaptionLabel() { return label; }
        public CaptionTranslationList.CaptionTrack universalCaptions_copyTranslation(
                String lang, String url, String vss, CharSequence label) {
            return new Track(lang, url, vss, label);
        }
    }
    private static final String URL =
            "https://www.youtube.com/api/timedtext?v=PlsODqqqwFs&lang=ar&sig=a%2Bb";

    @Test void canDeriveTurkishFromArabicAsrWithoutExistingTranslatedTrack() {
        Track arabic = new Track("ar", URL, "a.ar", "Arabic (auto-generated)");
        CaptionTranslationList.CaptionTrack result =
                SourceTranslationExperiment.preview(arabic, "tr", Locale.forLanguageTag("tr"));
        assertNotNull(result);
        assertEquals(URL + "&tlang=tr", result.universalCaptions_getCaptionUrl());
        assertEquals("ta.ar.tr", result.universalCaptions_getVssId());
        assertEquals("Türkçe", result.universalCaptions_getCaptionLabel());
        assertEquals(URL, arabic.url);
    }

    @Test void rejectsTranslatedAndPseudoTracks() {
        assertNull(SourceTranslationExperiment.preview(
                new Track("de", URL + "&tlang=de", "ta.ar.de", "German"), "tr", Locale.ENGLISH));
        assertNull(SourceTranslationExperiment.preview(
                new Track("AUTO_TRANSLATE_CAPTIONS_OPTION", URL, "-", "Auto translate"),
                "tr", Locale.ENGLISH));
    }

    @Test void rejectsUnsafeAndNonNativeUrls() {
        for (String url : new String[]{
                "http://www.youtube.com/api/timedtext?v=x&lang=ar",
                "https://youtube.com.evil.example/api/timedtext?v=x&lang=ar",
                "https://www.youtube.com/other?v=x&lang=ar",
                URL + "&tlang=fr"
        }) {
            assertNull(SourceTranslationExperiment.preview(
                    new Track("ar", url, "a.ar", "Arabic"), "tr", Locale.ENGLISH));
        }
    }
}
