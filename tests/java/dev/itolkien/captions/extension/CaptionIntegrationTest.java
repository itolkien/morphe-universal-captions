package dev.itolkien.captions.extension;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import dev.itolkien.captions.extension.CaptionTranslationList.CaptionTrack;
import static org.junit.jupiter.api.Assertions.*;

class CaptionIntegrationTest {
    interface NativeTrack { }
    static class Track implements CaptionTrack {
        final String code, url, vss;
        final CharSequence label;
        Track() { this("de", "https://www.youtube.com/api/timedtext?lang=en&tlang=de", "ta.en.de", "German"); }
        Track(String code, String url, String vss, CharSequence label) {
            this.code = code; this.url = url; this.vss = vss; this.label = label;
        }
        public String universalCaptions_getLanguageCode() { return code; }
        public String universalCaptions_getVssId() { return vss; }
        public String universalCaptions_getCaptionUrl() { return url; }
        public CharSequence universalCaptions_getCaptionLabel() { return label; }
        public CaptionTrack universalCaptions_copyTranslation(String code, String url, String vss, CharSequence label) {
            return new Track(code, url, vss, label);
        }
    }
    static final class IntegratedTrack extends Track implements NativeTrack,
            app.morphe.extension.youtube.patches.AutoTranslateCaptionsPatch.CaptionTrackInterface { }

    @Test void nativeIntegrationOwnsTheMenuEvenWhenItsSettingIsOff() {
        List<CaptionTrack> input = List.of(new IntegratedTrack());
        assertSame(input, CaptionIntegration.augment(input, Locale.ENGLISH, NativeTrack.class));
        assertSame(input, CaptionIntegration.augment(input, Locale.ENGLISH));
        // A native hook before or after this hook remains the only menu owner.
        assertSame(input, CaptionIntegration.augment(
                CaptionIntegration.augment(input, Locale.ENGLISH, NativeTrack.class), Locale.ENGLISH, NativeTrack.class));
    }

    @Test void bundledExtensionWithoutInjectedTrackInterfaceDoesNotDisableAddon() {
        List<CaptionTrack> input = List.of(new Track());
        assertFalse(CaptionIntegration.isManagedByNative(input, NativeTrack.class));
        assertTrue(CaptionIntegration.augment(input, Locale.ENGLISH, NativeTrack.class).stream()
                .anyMatch(t -> "el".equals(t.universalCaptions_getLanguageCode())));
    }

    @Test void absentNativeExtensionAndEmptyListsRemainSafe() {
        assertFalse(CaptionIntegration.isManagedByNative(List.of(new Object()), null));
        assertTrue(CaptionIntegration.augment(List.of(new Track()), Locale.ENGLISH, null).stream()
                .anyMatch(t -> "el".equals(t.universalCaptions_getLanguageCode())));
        assertTrue(CaptionIntegration.augment(List.of(new Track()), Locale.ENGLISH).stream()
                .anyMatch(t -> "el".equals(t.universalCaptions_getLanguageCode())));
        assertFalse(CaptionIntegration.isManagedByNative(null, NativeTrack.class));
        assertFalse(CaptionIntegration.isManagedByNative(List.of(), NativeTrack.class));
        assertTrue(CaptionIntegration.isManagedByNative(Arrays.asList(null, new IntegratedTrack()), NativeTrack.class));
    }
}
