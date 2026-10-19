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
| Native per-video target metadata has codes | Use the declared codes and omit unsafe duplicates | Native/invalid catalog regressions | Some upstream builds may not expose metadata |
| Native target list empty or absent | Use the 156-code compatibility snapshot to restore missing menus | Empty and null metadata regressions | Repeated protobuf fields do not distinguish an unset field from an empty one |
| Snapshot targets | Labels and URLs are generated locally only for a validated original source | 156×155 source/target cases, four UI locales | YouTube can reject entries independently of menus |
| ASR vs uploaded/named tracks | Derive only from an identity-validated original timedtext track | ASR/manual/named, VSS, kind and URL tests | Whether an individual native track is translatable is not exposed |
| Multiple audio/source languages | Prefer exact native default; do not guess when unmatched languages coexist | Mixed sources, different video IDs, native-default tests | No guaranteed access to Web's full audio-track index |
| Regional targets and old aliases | Keep `pt` and `pt-PT`, both Chinese scripts, and language-tag regions distinct | Alias and regional-code tests | Web localization may use its own preferred display name |
| Web's active target ordering | Preserve native selection; sort translated language choices alphabetically | Sort, stable labels and pseudo-option tests | Web can pin the currently selected target above the sorted list; we do not have a native selected-track getter in this addon |
| Dubbed audio tracks | Prefer an exact current-audio/default caption source rather than the alphabetically first language | Default source, mixed-language, cross-video tests | Without audioTracks[].captionTrackIndices/defaultAudioTrackIndex we cannot prove a precise match for every dub |
| Selected track, CC Off/On and preferences | Never force CC visibility, overwrite manual selection or auto-enable tracks | Preferred policy and Off/integration tests | Morphe preference patch has its own regional matching |
| Styled translations | Replace incompatible pen IDs with a default for translated srv3; preserve creator style for originals | Unit tests, MPP compilation and embedded class check | **Request/parser concurrency is not correctly correlated yet** |
| Signed YouTube requests | Replace only the actual `tlang` value; retain the rest of the opaque URL | Encoded key, fragment, duplicate, HTTPS/host tests | A valid URL is not proof of a nonempty server response |

## Source-of-truth architecture

1. `OriginalCaptionSource`: one validated original source (video ID, language,
   VSS, signed URL). No cross-video caching. `TimedTextUrl` is the shared
   parser for original sources, native translations and caption style requests.
2. `CaptionTranslationList`: validate native translation prototypes, use video
   target metadata, deduplicate and display-sort without changing native
   caption selection.
3. `EmptyTranslationFallback`: restore YouTube's actual menu row and missing
   translations only if there is a safe source and at least one target.
4. `CaptionPenStyles` and `CaptionPenHooks`: the adapted #3837 implementation
   owned by Universal Captions, not a separately duplicated upstream patch.
5. `NativeTranslationBridge`: standalone extension entry point for locale and
   manager hooks. The separate `CaptionIntegration` native-coexistence adapter
   was removed from this 3.0 development branch because the Morphe port will
   replace the old auto-translate and pen hooks. The final integrated port may
   call Morphe's locale API directly, eliminating the remaining reflection.

## Important open engineering questions

### Proto list presence is not the same as Web JSON presence

[Protocol Buffers field presence](https://protobuf.dev/programming-guides/field_presence/)
states that repeated fields do not track whether an empty list was explicitly
sent or was omitted. The native YouTube target metadata is read from a repeated
list; therefore `null` versus `[]` from our Java adapter is **not proven**
to correspond to Web's optional `translationLanguages` property. The
compatibility snapshot intentionally remains available for both empty and
missing native lists, preserving the original missing-menu behavior. When
YouTube provides a nonempty list, its entries are authoritative. A dependable
`isTranslatable` or presence signal would allow a stricter policy after
native structure verification.

### Selective creator styling versus a blanket pen reset

The #3837 workaround intentionally discards **all** creator-defined pen IDs
on translated documents. It solves the observed invisible-text problem but
cannot reproduce every styled YouTube Web translation: intentionally colored
or emphasized translated words would also fall back to the user's default
style. The proper target is to preserve valid translated styling where
possible and override only genuinely invisible or invalid pen applications.
That requires access to the document's pen definitions and rendered segment
context, which are not exposed at the current `getPenId(int)` hook.

The current default-pen behavior is kept as an explicitly documented
**readability-first fallback**, not described as exact Web visual parity.
Do not guess invisible pen IDs from a global integer constant; IDs belong
to individual timedtext documents.

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

### 3. Active subtitle and multi-audio Web semantics

A recent [YouTube Web walkthrough](https://polytested.com/blog/translate-youtube-video-subtitles-auto-dub)
shows the currently chosen Auto-translate target pinned above an otherwise
alphabetical language list. We currently sort all translated entries; the
source-aware manager's selected caption track is not exposed through our
extension ABI. Never infer "selected" from list order, language preferences
or the last network request — those can differ from the user's actual choice.

[YouTube's automatic dubbing documentation](https://support.google.com/youtube/answer/15569972?hl=en-EN)
confirms that videos can offer multiple selectable audio tracks. The Web
captions player response keeps `audioTracks` and `defaultAudioTrackIndex`
alongside `captionTracks`. An unordered or alphabetically first caption
source is not necessarily the original/current audio. This code uses an
exact native default match and rejects ambiguous mixed-language sources
rather than silently translating the wrong dub. Full Web parity needs a
verified mapping from the active audio-track index to its eligible
caption tracks.

### 4. Native display names

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
