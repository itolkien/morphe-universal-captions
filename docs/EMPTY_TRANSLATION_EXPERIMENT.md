> Historical experiment record. Stable **2.1.0** promotes this runtime; use the [current installation guide](../README.md) and [release evidence](RELEASE_2.1.0.md). References below to draft status, pending device tests, unverified versions and an unchanged feed describe earlier stages. Later user feedback confirmed device operation across **20.21.37–21.42.241**, including intermediate versions; see the release evidence for scope.

# Comprehensive translation menu experiment

Version: **2.0.1-experiment.4**. Branch: `experiment/empty-translation-fallback`. [PR #5](https://github.com/itolkien/morphe-universal-captions/pull/5) remains draft.

## Scope and use

Import the experimental MPP into Morphe Manager and select **Universal Captions - experimental empty translation fallback** alongside Morphe **Captions**, replacing any other Universal Captions version. Supported APK targets: **21.20.405** and **21.41.525**. **21.42.241 remains unverified.**

The fallback accepts original ASR and uploaded/manual captions, including named tracks, across videos and source languages. When the native translation list is empty, it restores YouTube's own localized Auto-translate option and derives translations from a validated original source. An already-present option is preserved and its empty translation list can also be repaired.

For multiple originals, the native current-audio default is used if its full VSS and URL match an eligible direct source. If YouTube declares no usable default, the first original in its current-audio direct-list order is used. All original caption options remain available. Sources from another manager, video or audio's excluded caption indices are never imported. This chooses a translation source; it does not claim to follow a separately selected manual source in every native UI flow.

## Target languages

The patch structurally resolves the native caption metadata list and reads its target codes directly. A valid future target code needs no addon allowlist update. A nonempty native catalog is authoritative; malformed entries are rejected. Distinct regional targets such as `pt` and `pt-PT`, and Simplified/Traditional Chinese, remain distinct. Known legacy aliases are deduplicated.

Only when the native catalog is absent or empty, the addon uses a **156-code YouTube player-response snapshot observed 2026-10-16**, from [FlcePQVkmM4](https://www.youtube.com/watch?v=FlcePQVkmM4). The clean code-only fixture is in `tests/fixtures/`. This snapshot is a fallback, not a claim that YouTube has a permanent fixed language count. Labels use the app locale and available Android locale data, with the code as a fallback; menus are sorted by localized labels.

Existing native translation entries and their source are preserved. When Morphe's native additional-language integration is present, supplementing a nonempty menu respects **Add additional auto-translate languages**. The explicitly selected empty-menu fallback remains independent of that setting. Missing or unknown setting APIs fail closed for nonempty native menus.

## Identity and architecture

- Original VSS: `a.<language>` (ASR) or `.<language>` (manual), optionally one native name identifier. Preserve the complete identifier.
- Only HTTPS YouTube `/api/timedtext`, consistent source language, valid video identity, and matching ASR kind are accepted. Reject translated sources, conflicting video identities and duplicated/encoded protected query keys.
- Preserve signed URL bytes and append `tlang` before any fragment. No external translation service, cookies, network interception or source cache.
- Preserve original method names, signatures and anchors for Morphe v2 discovery. Boolean-parameter raw/metadata/default helpers avoid parameterless fingerprint collisions. Resolve the complete ABI before mutation.
- Morphe owns preferred language, explicit user choice, caption visibility and Off. Its policy classes are untouched.

## Evidence and limits

The runtime matrix covers all **156 × 155** distinct source/target pairs for both manual and ASR source shapes in four display locales: **193,440 generated translations** with exact URL, VSS and label checks. This tests construction and isolation, not server playback. Dynamic catalogs, malformed data, multiple sources, setting ownership and native policy behavior have separate regressions.

The tests compile and run the **unmodified upstream Morphe preferred-language implementation** (test-only fixture with its NOTICE/license). All catalog languages are offered to that policy; Off, provider priority, explicit user selection and new-video reset are checked. Its existing broad regional matching can select generic `pt` for preference `pt-PT`; both menu options exist and can be selected manually. The addon deliberately does not replace that policy.

YouTube may withhold a translation even when captions exist. The backend `isTranslatable` flag is not exposed by this bridge. Unsupported sources, account restrictions, server errors and absent original captions cannot be repaired by menu construction. No device is attached; experiment.4 playback has not been verified on Android. Previous user reports of working Arabic/English examples are not evidence for every language or this new artifact.

## Device checks still needed

1. Test actual translated text on `PlsODqqqwFs` and `FlcePQVkmM4`, including Turkish and a newly exposed language.
2. Test a video with multiple originals and multiple audio tracks; switch audio, languages and videos.
3. Check preferred-language selection, Off, manual selection and the native additional-language toggle.
4. Report APK/Manager/Morphe versions and source/target language if a menu is present but text is missing. Do not publish signed caption URLs.

The stable Manager feed and released artifacts are unchanged. Exact build/APK results are recorded after final validation below.

## Exact local validation (2026-10-16)

- Clean 21.20.405 and 21.41.525: **44 passed, 1 native-only check skipped**, per APK.
- Native Captions plus experiment.4 on 21.41.525, **both application orders**: **45/45 passed**, per order. Original host anchors, live metadata/default helpers and both native/addon interfaces/hooks were verified from the resulting DEX.
- All four new APKs passed SDK DEX, cross-DEX hierarchy, alignment, disposable signing and APK verification. Native caption-policy classes remained byte-for-byte equivalent to their baseline. No APK or signing key is distributed.
- Final MPP: 758,441 bytes; root Android DEX coverage **19 JVM / 22 DEX classes**; required embedded runtime types, ZIP integrity, DEX SHA-1/Adler-32 checksums, exact embedded MPE equality, version and declared target metadata passed. A deliberately corrupted DEX was rejected.
- SHA-256: `f3a3251d82b67babc8f52ac0bd30a733ea5aa1b894ba9283adfceb7e65214637`.
- Independent final source review found no Important regressions. Android playback/server availability and APK 21.42.241 remain unverified.

## Device confirmation and stable promotion

After the experiment.4 package was built, the user reported successful device testing on **21.20.405**, including the manual test sequence for translations, Off/on, audio/source transitions and video switching. The app-process capture showed no observed crash; it did not contain detailed caption-operation diagnostics, so individual visual outcomes are user-reported. Earlier “no device verification” notes above describe the state at the time of the experimental build.

The unchanged runtime is promoted to **2.1.0**; see [release evidence](RELEASE_2.1.0.md). Regional preference matching and other stated limits remain documented.
