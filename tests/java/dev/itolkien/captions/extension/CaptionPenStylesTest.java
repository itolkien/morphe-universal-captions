package dev.itolkien.captions.extension;

import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import dev.itolkien.captions.extension.CaptionTranslationList.CaptionTrack;
import static org.junit.jupiter.api.Assertions.*;

class CaptionPenStylesTest {
    private static final String ORIGINAL =
            "https://www.youtube.com/api/timedtext?v=PlsODqqqwFs&lang=en&sig=a%2Bb";
    private static final int DEFAULT_PEN = Integer.MIN_VALUE;

    static final class Track implements CaptionTrack {
        final String language, url, vss;
        Track(String language, String url, String vss) {
            this.language = language;
            this.url = url;
            this.vss = vss;
        }
        public String universalCaptions_getLanguageCode() { return language; }
        public String universalCaptions_getVssId() { return vss; }
        public String universalCaptions_getCaptionUrl() { return url; }
        public CharSequence universalCaptions_getCaptionLabel() { return language; }
        public CaptionTrack universalCaptions_copyTranslation(
                String language, String url, String vss, CharSequence label) {
            return new Track(language, url, vss);
        }
    }

    static final class Carrier implements CaptionPenStyles.CaptionCarrier {
        final CaptionTrack track;
        Carrier(CaptionTrack track) { this.track = track; }
        public CaptionTrack universalCaptions_styleTrack() { return track; }
    }

    private static Carrier original() { return new Carrier(new Track("en", ORIGINAL, ".en")); }
    private static Carrier translated() {
        return new Carrier(new Track("tr", ORIGINAL + "&tlang=tr", "t.en.tr"));
    }

    @Test void translatedAndOriginalStylesAreScopedToTheirOwnDocuments() {
        assertEquals(7, CaptionPenStyles.getPenId(7));
        CaptionPenStyles.beginCaption(translated());
        try {
            assertEquals(DEFAULT_PEN, CaptionPenStyles.getPenId(7));
            assertEquals(DEFAULT_PEN, CaptionPenStyles.getPenId(0));
        } finally {
            CaptionPenStyles.endCaption();
        }
        assertEquals(7, CaptionPenStyles.getPenId(7));
        CaptionPenStyles.beginCaption(original());
        try {
            assertEquals(7, CaptionPenStyles.getPenId(7));
        } finally {
            CaptionPenStyles.endCaption();
        }
    }

    @Test void invalidOrAmbiguousTargetsKeepOriginalPenIds() {
        for (String url : new String[] {
                ORIGINAL + "&tlang=",
                ORIGINAL + "&tlang=tr&tlang=de",
                ORIGINAL + "&tlang=tr&%74lang=de",
                ORIGINAL + "&tlang=tr%26lang%3Dde",
                ORIGINAL + "&tlang=bad%20value",
                "https://youtube.com.evil.example/api/timedtext?v=PlsODqqqwFs&lang=en&tlang=tr"
        }) {
            CaptionPenStyles.beginCaption(new Carrier(new Track("tr", url, "t.en.tr")));
            try {
                assertEquals(8, CaptionPenStyles.getPenId(8), url);
            } finally {
                CaptionPenStyles.endCaption();
            }
        }
        assertEquals(8, CaptionPenStyles.getPenId(8));
    }

    @Test void unknownContextNeverInheritsTranslatedStyle() {
        CaptionPenStyles.beginCaption(translated());
        try {
            assertEquals(DEFAULT_PEN, CaptionPenStyles.getPenId(3));
            CaptionPenStyles.beginCaption(new Object());
            try {
                assertEquals(3, CaptionPenStyles.getPenId(3));
            } finally {
                CaptionPenStyles.endCaption();
            }
            assertEquals(DEFAULT_PEN, CaptionPenStyles.getPenId(3));
        } finally {
            CaptionPenStyles.endCaption();
        }
        CaptionPenStyles.endCaption(); // extra cleanup is idempotent
        assertEquals(3, CaptionPenStyles.getPenId(3));
    }

    @Test void sourceAndTargetIdentitiesMustAgree() {
        CaptionPenStyles.beginCaption(new Carrier(
                new Track("tr", ORIGINAL + "&tlang=tr", "t.en.fr")));
        try {
            assertEquals(5, CaptionPenStyles.getPenId(5));
        } finally {
            CaptionPenStyles.endCaption();
        }
    }

    @Test void concurrentOriginalAndTranslatedParsersDoNotBleedAcrossThreads() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> translated = workers.submit(() -> {
                CaptionPenStyles.beginCaption(translated());
                try {
                    barrier.await(3, TimeUnit.SECONDS);
                    return CaptionPenStyles.getPenId(12);
                } finally {
                    CaptionPenStyles.endCaption();
                }
            });
            Future<Integer> original = workers.submit(() -> {
                CaptionPenStyles.beginCaption(original());
                try {
                    barrier.await(3, TimeUnit.SECONDS);
                    return CaptionPenStyles.getPenId(12);
                } finally {
                    CaptionPenStyles.endCaption();
                }
            });
            assertEquals(DEFAULT_PEN, translated.get(5, TimeUnit.SECONDS));
            assertEquals(12, original.get(5, TimeUnit.SECONDS));
            Future<Integer> reused = workers.submit(() -> CaptionPenStyles.getPenId(12));
            assertEquals(12, reused.get(5, TimeUnit.SECONDS));
        } finally {
            workers.shutdownNow();
        }
    }
}
