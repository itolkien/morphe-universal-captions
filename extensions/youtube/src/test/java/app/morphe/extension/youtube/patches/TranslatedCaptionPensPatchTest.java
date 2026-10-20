/*
 * Copyright 2026 Morphe.
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.youtube.patches;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class TranslatedCaptionPensPatchTest {
    private static final int DEFAULT_PEN = Integer.MIN_VALUE;
    private static final String ORIGINAL =
            "https://www.youtube.com/api/timedtext?v=abcdefghijk&lang=en&sig=x";

    private static final class Track implements AutoTranslateCaptionsPatch.CaptionTrackInterface {
        final String language, vss, url;
        Track(String language, String vss, String url) {
            this.language = language;
            this.vss = vss;
            this.url = url;
        }
        @Override public String patch_getLanguageCode() { return language; }
        @Override public String patch_getVssId() { return vss; }
        @Override public String patch_getUrl() { return url; }
        @Override public Track patch_copyTranslation(String code, String value, String identity,
                CharSequence label) {
            return new Track(code, identity, value);
        }
    }

    private static final class Request implements TranslatedCaptionPensPatch.CaptionRequestInterface {
        final Object track;
        Request(Object track) { this.track = track; }
        @Override public Object patch_getCaptionTrack() { return track; }
    }

    private static Request original() { return new Request(new Track("en", ".en", ORIGINAL)); }
    private static Request translated() {
        return new Request(new Track("tr", "t.en.tr", ORIGINAL + "&tlang=tr"));
    }

    private static void within(Object request, Runnable test) {
        TranslatedCaptionPensPatch.beginCaption(request);
        try {
            test.run();
        } finally {
            TranslatedCaptionPensPatch.endCaption(request);
        }
    }

    @Test public void translatedAndOriginalPensDoNotLeak() {
        assertEquals(7, TranslatedCaptionPensPatch.getPenId(7));
        within(translated(), () -> {
            assertEquals(DEFAULT_PEN, TranslatedCaptionPensPatch.getPenId(7));
            assertEquals(DEFAULT_PEN, TranslatedCaptionPensPatch.getPenId(0));
        });
        assertEquals(7, TranslatedCaptionPensPatch.getPenId(7));
        within(original(), () -> assertEquals(7, TranslatedCaptionPensPatch.getPenId(7)));
    }

    @Test public void unknownRequestsRestoreNestedTranslationScope() {
        within(translated(), () -> {
            within(new Object(), () -> assertEquals(7, TranslatedCaptionPensPatch.getPenId(7)));
            assertEquals(DEFAULT_PEN, TranslatedCaptionPensPatch.getPenId(7));
        });
        assertEquals(7, TranslatedCaptionPensPatch.getPenId(7));
    }

    @Test public void malformedOrUnsafeTargetDoesNotAlterOriginalPens() {
        for (String badUrl : List.of(ORIGINAL + "&tlang=tr&tlang=de",
                ORIGINAL + "&%74lang=tr", ORIGINAL + "&tlang=tr&%74lang=fr",
                ORIGINAL + "&tlang=tr%26lang%3Dfr",
                "https://youtube.com.evil.example/api/timedtext?v=abcdefghijk&lang=en&tlang=tr")) {
            within(new Request(new Track("tr", "t.en.tr", badUrl)),
                    () -> assertEquals(7, TranslatedCaptionPensPatch.getPenId(7)));
        }
        within(new Request(new Track("tr", "t.en.fr", ORIGINAL + "&tlang=tr")),
                () -> assertEquals(7, TranslatedCaptionPensPatch.getPenId(7)));
    }

    @Test public void failedNativeGettersFailClosed() {
        within(translated(), () -> {
            within((TranslatedCaptionPensPatch.CaptionRequestInterface) () -> {
                throw new IllegalStateException("obsolete native track getter");
            }, () -> assertEquals(7, TranslatedCaptionPensPatch.getPenId(7)));
            within(new Request(new Track("tr", null, ORIGINAL + "&tlang=tr")),
                    () -> assertEquals(7, TranslatedCaptionPensPatch.getPenId(7)));
            assertEquals(DEFAULT_PEN, TranslatedCaptionPensPatch.getPenId(7));
        });
    }

    @Test public void unmatchedExceptionCleanupCannotPopAnotherScope() {
        Request outer = translated(), inner = original();
        within(outer, () -> {
            TranslatedCaptionPensPatch.endCaption(inner);
            assertEquals(DEFAULT_PEN, TranslatedCaptionPensPatch.getPenId(7));
            within(inner, () -> {
                TranslatedCaptionPensPatch.endCaption(outer);
                assertEquals(7, TranslatedCaptionPensPatch.getPenId(7));
            });
            assertEquals(DEFAULT_PEN, TranslatedCaptionPensPatch.getPenId(7));
        });
        TranslatedCaptionPensPatch.endCaption(outer);
        assertEquals(7, TranslatedCaptionPensPatch.getPenId(7));
    }

    @Test public void exceptionExitRestoresOuterScope() {
        within(translated(), () -> {
            try {
                within(original(), () -> {
                    assertEquals(7, TranslatedCaptionPensPatch.getPenId(7));
                    throw new IllegalStateException("parser failed");
                });
                throw new AssertionError("Expected native parser failure");
            } catch (IllegalStateException expected) {
                assertEquals(DEFAULT_PEN, TranslatedCaptionPensPatch.getPenId(7));
            }
        });
        assertEquals(7, TranslatedCaptionPensPatch.getPenId(7));
    }

    @Test public void concurrentParsersKeepIndependentScopes() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        AtomicInteger first = new AtomicInteger(), second = new AtomicInteger();
        Thread a = new Thread(() -> within(translated(), () -> {
            ready.countDown();
            await(ready);
            first.set(TranslatedCaptionPensPatch.getPenId(7));
        }));
        Thread b = new Thread(() -> within(original(), () -> {
            ready.countDown();
            await(ready);
            second.set(TranslatedCaptionPensPatch.getPenId(7));
        }));
        a.start(); b.start(); a.join(); b.join();
        assertEquals(DEFAULT_PEN, first.get());
        assertEquals(7, second.get());
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
