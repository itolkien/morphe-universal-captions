package dev.itolkien.captions.extension;

import java.util.*;

public final class TranslationTargetsTest {
    private static void check(boolean value, String why) {
        if (!value) throw new AssertionError(why);
    }
    public static void main(String[] args) {
        List<String> added = TranslationTargets.missing(Arrays.asList("en", "de"), Collections.emptySet());
        check(Collections.frequency(added, "el") == 1, "Greek must be offered once when missing");
        check(!TranslationTargets.missing(Arrays.asList("EL_gr"), Collections.emptySet()).contains("el"),
                "native regional Greek must not be duplicated");
        check(!TranslationTargets.missing(Collections.emptySet(), Arrays.asList("el-GR")).contains("el"),
                "direct Greek captions must remain the choice");
        check(!TranslationTargets.missing(Arrays.asList("iw", "tl"), Collections.emptySet()).contains("he"),
                "native legacy Hebrew must not be duplicated");
        check(!TranslationTargets.missing(Arrays.asList("iw", "tl"), Collections.emptySet()).contains("fil"),
                "native legacy Filipino must not be duplicated");
        check(TranslationTargets.missing(Arrays.asList("zh-Hans"), Collections.emptySet()).contains("zh-Hant"),
                "simplified Chinese must not hide traditional Chinese");
        check(TranslationTargets.missing(Arrays.asList("zh-Hant"), Collections.emptySet()).contains("zh-Hans"),
                "traditional Chinese must not hide simplified Chinese");
        check(TranslationTargets.missing(Arrays.asList("zh-Hans-HK"), Collections.emptySet()).contains("zh-Hant"),
                "explicit Simplified script takes priority over Hong Kong region");
        check(TranslationTargets.missing(Arrays.asList("zh-Hant-CN"), Collections.emptySet()).contains("zh-Hans"),
                "explicit Traditional script takes priority over China region");
        check(added.contains("he") && added.contains("fil") && !added.contains("iw") && !added.contains("tl"),
                "catalogue must not generate legacy alias duplicates");
        check(new HashSet<>(added).size() == added.size(), "no duplicate generated codes");
        check(TranslationTargets.missing(Arrays.asList(null, "", "AUTO_TRANSLATE_CAPTIONS_OPTION"),
                Collections.emptySet()).contains("el"), "menu sentinel must not hide Greek");
        System.out.println("TranslationTargets: all tests passed");
    }
}
