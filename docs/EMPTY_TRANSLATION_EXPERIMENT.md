# Empty translation menu experiment

Version: **2.0.1-experiment.3**. Branch: `experiment/empty-translation-fallback`. PR #5 remains draft.

## Scope

Select **Universal Captions - experimental empty translation fallback** in place of the stable Universal Captions addon, alongside Morphe Captions. Do not select two Universal Captions versions together. Declared APK targets remain **21.20.405** and **21.41.525**; the 21.42.241 app mentioned in upstream issue #3780 has not been validated here.

Experiment.2 was deliberately restricted to `PlsODqqqwFs` and Arabic ASR. The user confirmed that case working on-device, then provided a screenshot of `FlcePQVkmM4` with one named English track and no Auto-translate row. Public player metadata for that video identifies `en`, a named manual VSS (`.en.78oJUrdTLnk`), no ASR kind, and `isTranslatable=true`. A named-source suffix is an identity component, not a language tag or a target.

Experiment.3 removes the video/source-language allowlist. It supports exactly one validated original ASR or manual caption, including named tracks, when the native translated list is empty and the direct list has no native Auto-translate row. Zero/multiple sources, existing native translations/options, translated/pseudo tracks, invalid URLs, inconsistent metadata and failures retain the original list. No source or URL is cached between managers/videos. Caption preferences and automatic source selection remain Morphe's responsibility.

## Identity and URL rules

- Original VSS is `a.<language>` (ASR) or `.<language>` (manual), optionally followed by one nonempty native name identifier using letters, digits, `_` or `-`.
- Use only HTTPS YouTube `/api/timedtext` URLs with an 11-character video ID and a source language consistent with the track. ASR requires `kind=asr`; manual sources have no kind or an empty kind.
- Reject translated URLs, duplicate or encoded protected query keys (`v`, `lang`, `kind`, `name`, `tlang`), and already translated/pseudo source identities.
- Preserve the entire original VSS suffix and all original URL bytes, including the encoded `name`, signature and other parameters. Append `tlang` before any fragment. Never replace the source name with the target label.
- Generate the existing supported target list with localized labels and sorting; exclude the original language and its known aliases.

The native source's backend `isTranslatable` flag is not exposed by this addon interface. Structural validation is not a promise that YouTube will serve a translation for every source. This remains an explicitly selected experiment.

## Morphe v2 coexistence

Original native method names, signatures and semantic anchors remain intact. The saved-manager register and boolean raw-source helpers from experiment.2 are unchanged. Raw getters bypass fallback without global/per-thread state and remain excluded from Morphe's parameterless fingerprints. The native option factory supplies the localized menu label and flags.

`CaptionIntegration` continues to leave nonempty native menus under Morphe's control in both additional-language setting states. The separately selected fallback fills only the single-source empty case, independent of that setting. The addon does not hook visibility, Off, explicit selection, preferences or network requests.

## Evidence and limits

- Upstream [issue #3780](https://github.com/MorpheApp/morphe-patches/issues/3780#issuecomment-6213848463) explains that the native additional-language feature needs an existing Auto-translate menu. It does not itself create one when missing.
- The user confirmed experiment.2 working for the Arabic video. This is user-reported device evidence for that example, not device evidence for the revised experiment.3.
- In this investigation, the new video's English timedtext request returned HTTP 200 with caption events. A translated Turkish request returned HTTP 429; translated text and Android playback were not verified. No authenticated cookies or device were used.
- Six added regressions failed against experiment.2 and passed with experiment.3: named English identity/name preservation, other video/source languages, source isolation, alias exclusion and malformed source rejection.
- Independent source review found no Important code defects. Unknown future native option-factory shapes remain outside the two declared APK targets.

## Device test

1. Import experiment.3 MPP into Morphe Manager, select its experimental patch with Captions, and patch a clean supported YouTube APK. Leave stable Universal Captions unselected.
2. Open `https://www.youtube.com/watch?v=FlcePQVkmM4`, open Captions, then Auto-translate and choose Turkish. Check actual translated lines, not only the new menu row.
3. Recheck `https://www.youtube.com/watch?v=PlsODqqqwFs`, Off, video switching and normal native translation menus. Source names must remain distinct and URLs must not leak across videos.

The stable Manager feed and stable releases are unchanged. Local build/APK verification results for this exact revision are recorded below after validation.

## Local validation (2026-10-16, experiment.3)

- Clean 21.20.405 and 21.41.525: 35 passed, 1 native-only test skipped per APK. Complete host ABI/discovery and unchanged caption-policy checks passed.
- Native Captions plus experiment.3 on 21.41.525: 36/36 passed, including both native/addon interfaces/hooks and byte-for-byte caption policy isolation.
- All three newly produced APKs passed SDK DEX, cross-DEX hierarchy, alignment, disposable signing and APK verification. No patched APK or key is distributed.
- The outgoing MPP passed ZIP integrity, root Android DEX coverage (17 JVM patch classes, 20 DEX classes), embedded runtime type/checksum checks, version/target metadata and SHA-256 verification.
- The hook implementation/ABI is unchanged from experiment.2, whose two application orders were tested. This revision repeats native-first combined validation and all runtime regressions; it does not claim a new reverse-order APK build.
