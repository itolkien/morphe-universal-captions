package dev.itolkien.captions.extension;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TimedTextUrlTest {
    private static final String URL =
            "https://www.youtube.com/api/timedtext?v=abcdefghijk&lang=en&sig=a%2Bb&tlang=de&fmt=srv3";

    @Test void extractsIdentityWithoutChangingSignedQueryBytes() {
        TimedTextUrl url = TimedTextUrl.parse(URL + "&opaque=?tlang=de#tlang=de");
        assertNotNull(url);
        assertEquals("abcdefghijk", url.field("v"));
        assertEquals("en", url.field("lang"));
        assertEquals("de", url.field("tlang"));
        int start = url.targetValueOffset("de");
        assertEquals("de", (URL + "&opaque=?tlang=de#tlang=de").substring(start, start + 2));
        assertEquals(-1, url.targetValueOffset("fr"));
    }

    @Test void rejectsDuplicateAndEncodedIdentityParameters() {
        for (String suffix : new String[]{
                "&tlang=fr", "&%74lang=fr", "&lang=fr", "&v=other",
                "&%6cang=en", "&name=a&name=b", "&tlang", "&tlang=%ZZ"
        }) {
            assertNull(TimedTextUrl.parse(URL + suffix), suffix);
        }
    }

    @Test void onlyTrustsTheOfficialTimedtextHostAndPath() {
        assertNull(TimedTextUrl.parse(URL.replace("https:", "http:")));
        assertNull(TimedTextUrl.parse(URL.replace("www.youtube.com", "youtube.com.evil.example")));
        assertNull(TimedTextUrl.parse(URL.replace("/api/timedtext", "/watch")));
        assertNotNull(TimedTextUrl.parse(URL.replace("www.youtube.com", "youtube.com")));
    }
}
