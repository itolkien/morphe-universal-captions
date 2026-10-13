package dev.itolkien.captions;

import java.util.Arrays;

public final class LanguageResolverTest {
    private static void check(boolean v, String message) {
        if (!v) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        check(LanguageResolver.matches("tr-TR", "tr"), "Turkish region");
        check(LanguageResolver.matches("iw", "he"), "Hebrew legacy code");
        check(LanguageResolver.matches("pt-BR", "pt"), "Portuguese region");
        check(!LanguageResolver.matches("zh-Hans", "zh-Hant"), "Chinese scripts");
        check(!LanguageResolver.matches("zh-CN", "zh-TW"), "Chinese regions");
        check(!LanguageResolver.matches("tr", "de"), "unrelated languages");
        check("tr".equals(LanguageResolver.findExistingTarget(Arrays.asList("en","tr","de"),"tr")),
                "existing target");
        check(LanguageResolver.findExistingTarget(Arrays.asList("en","de"),"tr")==null,
                "never invent unsupported target");
        System.out.println("LanguageResolver: all tests passed");
    }
}
