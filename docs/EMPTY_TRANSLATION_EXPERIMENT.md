# Empty translation menu experiment

Version: **2.0.1-experiment.2**. Branch: `experiment/empty-translation-fallback`. PR #5 stays draft. This is a test bundle, not a stable release or a confirmed device fix.

## Scope

Selecting **Universal Captions - experimental empty translation fallback** explicitly enables this experiment. Use it in place of the stable Universal Captions addon, alongside Morphe Captions. Do not select two Universal Captions versions together.

The fallback runs only when the current manager returns exactly one original Arabic ASR caption (`ar`, `a.ar`), its HTTPS YouTube timedtext URL identifies `PlsODqqqwFs`, `lang=ar`, `kind=asr`, and the native translated list is empty. Existing Auto-translate options, nonempty translations, multiple direct sources, other videos/languages, malformed/ambiguous URLs, and failures preserve the original input. No URL or source is cached across videos/managers.

## Hook and Morphe v2 coexistence

The original direct and translated methods retain their names, semantic string anchors and original instructions, so Morphe can discover them regardless of application order. Each original gets an extra saved manager register and a fallback call at every return; an entry move preserves the original code's register layout even where it overwrites its former `p0`.

Copies named `universalCaptions_nativeDirect(boolean)` and `universalCaptions_nativeTranslated(boolean)` bypass the fallback. Their extra unused boolean parameter keeps them out of Morphe's parameterless fingerprints while preserving the original `p0` register position. The runtime calls these raw methods with `true`; there is no global or per-thread bypass state.

The missing Auto-translate row is built by an extracted, validated straight-line copy of YouTube's own option factory. It retains the app's localized label and option flags. Unknown factory shapes fail patching before host mutation. Translated options copy the current original ASR track and append `tlang` without rebuilding or decoding the signed URL. The target list is the existing supported language list, excludes the source language, and sorts by the app locale. Turkish is one target, not a hardcoded preference.

The existing v2 `CaptionIntegration` marker/delegation remains on the raw translated list, so nonempty native menus and both native additional-language setting states stay owned by Morphe. This separately selected experiment fills only the video-scoped empty case, irrespective of the native additional-language toggle. It does not hook caption visibility, Off, explicit selection, default source, account settings, preferences, or network requests. Morphe preferred-language helpers continue to resolve the original native method identities. The addon does not add its own preferred-selection policy.

## Device test

1. Import this MPP as a local source in Morphe Manager and select its experimental patch alongside Captions. Use a clean YouTube **21.20.405** or **21.41.525** APK. Leave stable Universal Captions unselected.
2. Open `https://www.youtube.com/watch?v=PlsODqqqwFs` and manually open Captions. The original Arabic ASR and Off should remain. Auto-translate should be added if all guards match.
3. Open Auto-translate and manually choose Turkish or another target. Check that actual translated lines appear; a menu entry does not prove YouTube served text.
4. Select Off, switch videos, and revisit. Check normal native auto-translate menus with Morphe's additional-language setting both off and on. Other videos must not gain this fallback.

No connected Android device is available in this work session. Live playback, server acceptance of generated URLs, and account-specific response behavior remain unverified. Compilation and APK/DEX checks cannot establish those facts. The stable Manager feed and releases are not changed.

## Local validation (2026-10-16, experiment.2)

- Standalone **21.20.405** and **21.41.525**: 29 tests passed, 1 skipped per APK (native coexistence is inapplicable to standalone APKs). Both original method identities remain discoverable.
- Native Captions then addon, **21.41.525**: all 30 tests passed.
- Addon then native Captions, **21.41.525**: all 30 tests passed. The earlier `SubtitleManagerDirectTracksFingerprint` failure was reproduced, fixed by retaining original native identities, and the new identity regression went RED to GREEN.
- Both combined runs contain both native and addon interfaces/list hooks. Native caption policy classes match the native baseline byte for byte in both application orders. Native additional-language off/on ownership is covered by the JVM regression cases.
- All four final APK builds passed SDK DEX, cross-DEX hierarchy, alignment, disposable test-key signing and APK verification. Modified APKs and keys are not distributed.
- MPP: 17 JVM patch classes covered by Android DEX, 20 total patch DEX classes; embedded runtime DEX has all 7 required runtime/interface types and no bundled native Morphe caption implementation. ZIP integrity, DEX checksums, metadata version and APK target declarations verified on the outgoing file.
- Independent review of the revised implementation found no Critical/Important defects. Unknown future option-factory register shapes remain a hardening opportunity; only the two declared APK versions are supported.

Live translated-caption playback is unverified because no Android device is connected. Menu availability and passing APK/DEX checks do not establish YouTube server acceptance of generated translation URLs.
