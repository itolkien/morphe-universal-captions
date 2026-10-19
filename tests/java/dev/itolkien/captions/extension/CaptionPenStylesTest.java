package dev.itolkien.captions.extension;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CaptionPenStylesTest {
    private static final String ORIGINAL =
            "https://www.youtube.com/api/timedtext?v=PlsODqqqwFs&lang=en&sig=a%2Bb";
    private static final int DEFAULT_PEN = Integer.MIN_VALUE;

    @Test void translatedTracksUseDefaultPenAndOriginalTracksKeepCreatorStyles() {
        CaptionPenStyles.setTimedTextUrl(ORIGINAL + "&tlang=he");
        assertEquals(DEFAULT_PEN, CaptionPenStyles.getPenId(1));
        assertEquals(DEFAULT_PEN, CaptionPenStyles.getPenId(0));

        CaptionPenStyles.setTimedTextUrl(ORIGINAL);
        assertEquals(1, CaptionPenStyles.getPenId(1));
        assertEquals(-1, CaptionPenStyles.getPenId(-1));
    }

    @Test void ignoresTargetsEmbeddedInOpaqueValuesAndFragments() {
        for (String url : new String[] {
                ORIGINAL + "&sig2=%26tlang%3Dfr",
                ORIGINAL + "&opaque=tlang=fr",
                ORIGINAL + "#&tlang=fr",
                ORIGINAL + "&%74lang=fr"
        }) {
            CaptionPenStyles.setTimedTextUrl(url);
            assertEquals(4, CaptionPenStyles.getPenId(4), url);
        }
    }

    @Test void unrelatedRequestsMustNotOverrideTheLastTimedtextSelection() {
        CaptionPenStyles.setTimedTextUrl(ORIGINAL + "&tlang=ar");
        CaptionPenStyles.setTimedTextUrl("https://www.youtube.com/watch?v=PlsODqqqwFs");
        CaptionPenStyles.setTimedTextUrl(null);
        CaptionPenStyles.setTimedTextUrl("https://youtube.com.evil.example/api/timedtext?&tlang=ar");
        assertEquals(DEFAULT_PEN, CaptionPenStyles.getPenId(7));

        CaptionPenStyles.setTimedTextUrl(ORIGINAL);
        assertEquals(7, CaptionPenStyles.getPenId(7));
    }

    @Test void recognizesTargetBeforeOtherQueryParameters() {
        CaptionPenStyles.setTimedTextUrl(
                "https://www.youtube.com/api/timedtext?tlang=fa&v=PlsODqqqwFs&lang=en");
        assertEquals(DEFAULT_PEN, CaptionPenStyles.getPenId(2));
        CaptionPenStyles.setTimedTextUrl(ORIGINAL);
    }
}
