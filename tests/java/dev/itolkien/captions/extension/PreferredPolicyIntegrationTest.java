package dev.itolkien.captions.extension;

import java.util.*;
import org.junit.jupiter.api.Test;
import app.morphe.extension.youtube.patches.PreferredCaptionLanguagePatch;
import app.morphe.extension.youtube.settings.Settings;
import dev.itolkien.captions.extension.CaptionTranslationList.CaptionTrack;
import static org.junit.jupiter.api.Assertions.*;

/** Runs the actual upstream preference implementation, not an imitation of its selection policy. */
class PreferredPolicyIntegrationTest {
    static class Track extends EmptyTranslationFallbackTest.Track implements PreferredCaptionLanguagePatch.CaptionTrackInterface {
        Track(String lang, String url, String vss) { super(lang, url, vss); }
        public String patch_getLanguageCode() { return universalCaptions_getLanguageCode(); }
        public String patch_getVssId() { return universalCaptions_getVssId(); }
        @Override public CaptionTrack universalCaptions_copyTranslation(String lang, String url, String vss, CharSequence label) {
            return new Track(lang, url, vss);
        }
    }
    static class Manager implements PreferredCaptionLanguagePatch.SubtitleManagerInterface {
        final EmptyTranslationFallbackTest.Source source = new EmptyTranslationFallbackTest.Source();
        Manager() {
            source.direct = List.of(new Track("DISABLE_CAPTIONS_OPTION", "", "-"),
                    new Track("ar", EmptyTranslationFallbackTest.URL, "a.ar"));
        }
        private List<PreferredCaptionLanguagePatch.CaptionTrackInterface> adapt(List<CaptionTrack> tracks) {
            List<PreferredCaptionLanguagePatch.CaptionTrackInterface> result = new ArrayList<>();
            for (CaptionTrack t : tracks) result.add(new Track(t.universalCaptions_getLanguageCode(), t.universalCaptions_getCaptionUrl(), t.universalCaptions_getVssId()));
            return result;
        }
        public List<PreferredCaptionLanguagePatch.CaptionTrackInterface> patch_getDirectCaptionTracks() {
            return adapt(EmptyTranslationFallback.direct(source.direct, source, Locale.ENGLISH));
        }
        public List<PreferredCaptionLanguagePatch.CaptionTrackInterface> patch_getAutoTranslateCaptionTracks() {
            return adapt(EmptyTranslationFallback.translated(List.of(), source, Locale.ENGLISH));
        }
    }
    @Test void allCatalogLanguagesReachTheNativePolicyWithItsExistingRegionalMatching() {
        Manager manager = new Manager();
        Track original = (Track) manager.source.direct.get(1);
        for (String target : CaptionTranslationList.TARGETS) {
            PreferredCaptionLanguagePatch.newVideoStarted(null);
            Settings.PREFERRED_CAPTION_LANGUAGE.value = target;
            var selected = PreferredCaptionLanguagePatch.getPreferredCaptionTrack(manager, original);
            // Upstream policy normalizes legacy aliases; region matching follows its existing policy.
            String expected = target.equals("pt-PT") ? "pt" : target;
            assertEquals(CaptionTranslationList.languageKey(expected), CaptionTranslationList.languageKey(selected.patch_getLanguageCode()), target);
        }
        Settings.PREFERRED_CAPTION_LANGUAGE.value = "off";
    }
    @Test void offExplicitDisableDirectProviderAndManualUserChoiceArePreserved() throws Exception {
        Manager manager = new Manager();
        Track original = (Track) manager.source.direct.get(1);
        PreferredCaptionLanguagePatch.newVideoStarted(null);
        Settings.PREFERRED_CAPTION_LANGUAGE.value = "off";
        assertSame(original, PreferredCaptionLanguagePatch.getPreferredCaptionTrack(manager, original));
        Settings.PREFERRED_CAPTION_LANGUAGE.value = "tr";
        Track disabled = (Track) manager.source.direct.get(0);
        assertSame(disabled, PreferredCaptionLanguagePatch.getPreferredCaptionTrack(manager, disabled));
        Track provider = new Track("tr", EmptyTranslationFallbackTest.URL.replace("lang=ar&kind=asr", "lang=tr"), ".tr");
        manager.source.direct = List.of(disabled, original, provider);
        assertEquals(".tr", PreferredCaptionLanguagePatch.getPreferredCaptionTrack(manager, original).patch_getVssId());
        PreferredCaptionLanguagePatch.videoInformationLoaded();
        // Simulate the end of the native 500 ms initial-load grace period without sleeping.
        var start = PreferredCaptionLanguagePatch.class.getDeclaredField("videoStartTime");
        start.setAccessible(true); start.setLong(null, System.currentTimeMillis() - 1000);
        enum Selection { PREFERRED_TRACK }
        assertSame(original, PreferredCaptionLanguagePatch.onSetSubtitleTrack(manager, original, Selection.PREFERRED_TRACK));
        assertSame(original, PreferredCaptionLanguagePatch.getPreferredCaptionTrack(manager, original));
        PreferredCaptionLanguagePatch.newVideoStarted(null);
        assertEquals("tr", PreferredCaptionLanguagePatch.getPreferredCaptionTrack(manager, original).patch_getLanguageCode());
        Settings.PREFERRED_CAPTION_LANGUAGE.value = "off";
    }
}
