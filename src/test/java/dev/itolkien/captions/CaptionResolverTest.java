package dev.itolkien.captions;

import java.util.Arrays;
import java.util.Collections;
import static dev.itolkien.captions.CaptionResolver.*;

public final class CaptionResolverTest {
    private static Track manual(String lang, boolean original) { return new Track(lang, Kind.MANUAL, original); }
    private static Track asr(String lang, boolean original) { return new Track(lang, Kind.ASR, original); }
    private static void expect(Action expected, String source, Choice actual) {
        if (actual.action != expected || (source != null && !source.equals(actual.source.language)))
            throw new AssertionError("Expected " + expected + "/" + source + " got " +
                actual.action + "/" + (actual.source == null ? "none" : actual.source.language));
    }
    public static void main(String[] args) {
        expect(Action.DIRECT, "tr", choose(Arrays.asList(manual("en", true), manual("tr", false)), "tr", false));
        expect(Action.DIRECT, "tr", choose(Arrays.asList(manual("en", true), asr("tr", false)), "tr", false));
        expect(Action.TRANSLATE, "ja", choose(Arrays.asList(manual("en", false), manual("ja", true)), "tr", false));
        expect(Action.TRANSLATE, "ja", choose(Arrays.asList(manual("en", false), asr("ja", true)), "tr", false));
        expect(Action.TRANSLATE, "de", choose(Arrays.asList(asr("en", false), manual("de", false)), "tr", false));
        expect(Action.TRANSLATE, "ko", choose(Collections.singletonList(asr("ko", false)), "fr", false));
        expect(Action.NONE, null, choose(Collections.singletonList(manual("ja", true)), "tr", true));
        expect(Action.NONE, null, choose(Collections.emptyList(), "tr", false));
        expect(Action.DIRECT, "de", choose(Arrays.asList(manual("ja", true), manual("de", false)), "de", false));
        expect(Action.DIRECT, "he", choose(Collections.singletonList(manual("he", true)), "iw", false));
        // Pairwise rank competition: each higher-priority class must defeat lower classes.
        expect(Action.DIRECT, "tr", choose(Arrays.asList(
            asr("tr", false), manual("tr", false), manual("ja", true)), "tr", false));
        expect(Action.DIRECT, "tr", choose(Arrays.asList(
            manual("en", true), asr("tr", false), manual("ko", false)), "tr", false));
        expect(Action.TRANSLATE, "ja", choose(Arrays.asList(
            asr("ja", true), manual("de", false), asr("en", false), manual("ja", true)), "tr", false));
        expect(Action.TRANSLATE, "ja", choose(Arrays.asList(
            manual("de", false), asr("ja", true), asr("en", false)), "tr", false));
        expect(Action.TRANSLATE, "de", choose(Arrays.asList(
            asr("en", false), manual("de", false)), "tr", false));
        expect(Action.TRANSLATE, "ko", choose(Arrays.asList(
            asr("ko", false)), "tr", false));
        expect(Action.NONE, null, choose(Collections.<Track>emptyList(), "tr", false));
        System.out.println("CaptionResolverTest passed");
    }
}
