# YouTube Web behavior audit — Universal Captions 3.0 development

This document is a source-of-truth **behavior contract**, not a claim that an
unreleased Android build is already equivalent to the YouTube Web player.
Device/APK playback verification is deliberately postponed until the code is
ready for integration.

## Evidence for the comparison

- [YouTube Help: Manage caption settings on Android](https://support.google.com/youtube/answer/100078?co=GENIE.Platform%3DAndroid&hl=en):
  CC Off and On are explicit, and the player offers native captions before an
  optional Auto-translate language menu.
- [YouTube.js PlayerCaptionsTracklist](https://ytjs.dev/api/youtubei.js/namespaces/YTNodes/classes/PlayerCaptionsTracklist):
  `caption_tracks`, `audio_tracks`, `default_audio_track_index`, and
  optional `translation_languages` are separate player-response properties.
  The available audio track also lists its caption-track indices.
- [YouTube Help: caption customization](https://support.google.com/youtube/answer/100078?co=GENIE.Platform%3DDesktop&hl=en-GB):
  native caption font/color/background choices remain user-controlled.
- [YTT/SRV3 format notes](https://github.com/FyraLabs/yttml/blob/main/crates/srv3-ttml/internals/srv3-format.md):
  `<pen>` styles are declared in the document header and referenced by
  paragraphs and spans. Translations can inherit incompatible pen references.
- [Morphe #3837](https://github.com/MorpheApp/morphe-patches/pull/3837):
  confirmed Android rendering symptom, pen lookup strategy and original
  author/source. Its global latest-request race is explicitly acknowledged.

## Runtime decisions

| Behavior | Universal Captions 3.0 design | Automated coverage | Remaining uncertainty |
| --- | --- | --- | --- |
| No original caption track | Do not fabricate transcription or expose a fake translation source | Empty / malformed original tests | Backend-specific exceptions cannot be proven offline |
| Native translations exist | Keep native objects and add only missing, valid targets | Signed URL, dedup, alias and idempotence tests | Server availability can still vary per target |
| Native per-video target metadata exists | Use only declared codes; **explicit empty list means no target** | Present/empty/invalid catalog regressions | Some upstream builds may not expose metadata |
| Native metadata absent | Use dated 156-code snapshot, not a live promise that all translations exist | 156×155 source/target cases, four UI locales | YouTube can reject entries independently of menus |
| ASR vs uploaded/named tracks | Derive only from an identity-validated original timedtext track | ASR/manual/named, VSS, kind and URL tests | Whether an individual native track is translatable is not exposed |
| Multiple audio/source languages | Prefer exact native default; do not guess when unmatched languages coexist | Mixed sources, different video IDs, native-default tests | No guaranteed access to Web's full audio-track index |
| Regional targets and old aliases | Keep `pt` and `pt-PT`, both Chinese scripts, and language-tag regions distinct | Alias and regional-code tests | Web localization may use its own preferred display name |
| Selected track, CC Off/On and preferences | Never force CC visibility, overwrite manual selection or auto-enable tracks | Preferred policy and Off/integration tests | Morphe preference patch has its own regional matching |
| Styled translations | Replace incompatible pen IDs with a default for translated srv3; preserve creator style for originals | Unit tests, MPP compilation and embedded class check | **Request/parser concurrency is not correctly correlated yet** |
| Signed YouTube requests | Replace only the actual `tlang` value; retain the rest of the opaque URL | Encoded key, fragment, duplicate, HTTPS/host tests | A valid URL is not proof of a nonempty server response |

## Source-of-truth architecture

1. `OriginalCaptionSource`: one validated original source (video ID, language,
   VSS, signed URL). No cross-video caching.
2. `CaptionTranslationList`: validate native translation prototypes, use video
   target metadata, deduplicate and display-sort without changing native
   caption selection.
3. `EmptyTranslationFallback`: restore YouTube's actual menu row and missing
   translations only if there is a safe source and at least one target.
4. `CaptionPenStyles` and `CaptionPenHooks`: the adapted #3837 implementation
   owned by Universal Captions, not a separately duplicated upstream patch.
5. `CaptionIntegration` and `NativeTranslationBridge`: compatibility adapters
   for the **currently existing** Morphe native implementation. They can be
   simplified or removed only when that implementation has been replaced.

## Important open engineering questions

### 1. Request-to-document association for styled captions

A raw `/api/timedtext?tlang=...` URL is known when the network request is
built, but the pen lookup receives an integer ID **without the request URL**.
The existing process-wide flag can be overwritten by a different request
before the parser handles this document. `volatile`, `ThreadLocal`, a
time window, or a FIFO alone cannot guarantee request/response pairing.

**Required proper fix:** trace the Cronet callback or XML parser creation to
find an object shared across the request and parsed document. Bind translated
status to that object (e.g. a field or weak-key association), then read it
using the parser's own identity. This requires bytecode structure inspection
and fixture-based integration tests; we must not claim it is solved before
those hooks are demonstrated. Until then, malformed/ambiguous URL requests
are handled conservatively but overlapping valid requests remain a risk.

### 2. Per-track translatability and server errors

YouTube player responses also contain track-specific `isTranslatable` flags.
Our current track interface does not carry that flag, and metadata targets
alone cannot prove the timedtext endpoint will return nonempty captions. A
reliable implementation needs a *structurally verified* native accessor or
real response-state feedback, rather than inventing successful translations
from a language-code allowlist.

### 3. Native display names

The Web response offers localized `translationLanguages[].languageName`.
The addon currently derives names with the app's `Locale`. This is sensible
but not always guaranteed to match server-provided labels. Import native
names only when the ABI can locate them without adding fragile guesses.

## Morphe replacement (after this repository is reviewed)

- Port the implementation in this repository into Morphe's extension and
  bytecode patch packages with one code path.
- Remove the old native `AutoTranslateCaptionsPatch` and
  `TranslatedCaptionPensPatch` implementations and their old dependency
  wiring **as part of the same change**. Do not keep duplicate URL/pen hooks.
- Retain unrelated `Captions` functionality: user preference, CC visibility,
  subtitle auto-on rules, caption cookies and settings group.
- Check that cookie headers, timedtext URL hooks and any other patch modifying
  the same Cronet method have correct insertion order and fingerprints.
- Run the standalone Java regressions and native Morphe Kotlin/Android builds.
  Only after code review should clean APK/device tests be run on both declared
  versions, including rapid video/source changes and creator-styled tracks.

## Validation status

Code-level JVM tests and the Android patch *bundle* compile in CI. This is
**not** YouTube runtime proof. Clean-target APK patch/app playback tests,
cross-request concurrency verification, and full web parity comparisons are
still open until the final integration phase.
