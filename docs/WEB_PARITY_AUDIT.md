# YouTube Web behavior audit — Universal Captions 3.0

This compares the implementation with YouTube Web. The 3.0 development builds
have been tested manually; full parity and backend coverage are not claimed.

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

## Runtime decisions

| Behavior | Universal Captions 3.0 design | Automated coverage | Remaining uncertainty |
| --- | --- | --- | --- |
| No original caption track | Do not fabricate transcription or expose a fake translation source | Empty / malformed original tests | Backend-specific exceptions cannot be proven offline |
| Native translations exist | Keep native objects and add only missing, valid targets | Signed URL, dedup, alias and idempotence tests | Server availability can still vary per target |
| Native per-video target metadata has codes | Preserve native codes/tracks; add missing snapshot targets if the source has a compatible native target | Native 3/17-code catalog and self/invalid-code regressions | Added targets may have no server response |
| Native target list empty or absent | Use the 156-code compatibility snapshot to restore missing menus | Empty and null metadata regressions | Repeated protobuf fields do not distinguish an unset field from an empty one |
| Snapshot targets | Labels and URLs are generated locally only for a validated original source | 156×155 source/target cases, four UI locales | YouTube can reject entries independently of menus |
| ASR vs uploaded/named tracks | Derive only from an identity-validated original timedtext track | ASR/manual/named, VSS, kind and URL tests | Whether an individual native track is translatable is not exposed |
| Multiple audio/source tracks | Intersect native general source indices with the **current** audio's caption indices; preserve YouTube's existing per-target native translations | Mixed sources, distinct VSS, cross-video, active-audio and native-eligible-source tests | Snapshot extras still cannot prove backend support for every target |
| Regional targets and old aliases | Keep `pt` and `pt-PT`, both Chinese scripts, and language-tag regions distinct | Alias and regional-code tests | Web localization may use its own preferred display name |
| Web's active target ordering | Preserve native selection; sort translated language choices alphabetically | Sort, stable labels and pseudo-option tests | Web can pin the currently selected target above the sorted list; we do not have a native selected-track getter in this addon |
| Dubbed audio tracks | Use the native audio entry's caption indices, then exact default/unambiguous source only if needed | Current-audio source indices, source-switch and cross-video tests | A later manually selected subtitle is not necessarily the native audio default |
| Selected track, CC Off/On and preferences | Never force CC visibility, overwrite manual selection or auto-enable tracks | Preferred policy and Off/integration tests | Morphe preference patch has its own regional matching |
| Styled translations | Scope creator-pen reset to the native track being decoded (network and cached paths), not the latest request | Cross-thread/nested scope and request-matched cleanup tests, MPP build, original 21.20.405 DEX inspection | Itemized smoke tests for the 3.0.1 cleanup, uncaught exceptions and full style parity remain open |
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
4. `CaptionPenStyles` and `CaptionPenHooks`: the parser-scoped style fix
   owned by Universal Captions, with separate network and cache contexts.
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
compatibility snapshot remains available for both empty and missing native
lists. The 3.0 runtime preserves nonempty native entries, but offers
additional snapshot targets when the list contains a valid source-compatible
target. These extras are explicitly best-effort and are not proof of server
support. The native current-audio eligible-source bridge restricts the
original used for missing languages, but cannot prove backend availability.

### Selective creator styling versus a blanket pen reset

The current styling fix discards **all** creator-defined pen IDs on
translated documents. It solves the observed invisible-text problem but
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

The dev.7 patch no longer infers the parser's identity from the process's
last Cronet request. Structural hooks at the **actual decoder invocation**
(network callback and file cache) expose that callback's original native
caption track. A ThreadLocal scope is opened and closed **on the synchronous
parser thread**, not at asynchronous network request construction. This
isolates simultaneous caption documents and preserves original styles.
The original YouTube 21.20.405 DEX contained exactly one matching method for
each parser route; Java concurrency tests cover overlapping originals and
translations. Separate patched-APK testing is still required on both declared
YouTube versions, including unexpected parser exceptions. More precise Web-
compatible creator styles remain future work.

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
confirms that videos can offer multiple selectable audio tracks. The Android 21.20.405 DEX confirms that the native manager updates both its active audio entry and index upon an audio-change event. The Web
captions player response keeps `audioTracks` and `defaultAudioTrackIndex`
alongside `captionTracks`. An unordered or alphabetically first caption
source is not necessarily the original/current audio. This code now intersects the native active-audio caption indices with
the eligible original source indices, rather than guessing from language
order. The manually selected subtitle source can still differ from the
active audio's default.

### 4. Native display names

The Web response offers localized `translationLanguages[].languageName`.
The addon currently derives names with the app's `Locale`. This is sensible
but not always guaranteed to match server-provided labels. Import native
names only when the ABI can locate them without adding fragile guesses.

## Morphe integration

The Morphe maintainers invited the author to integrate this standalone
project into their patch set. The integration should use this code as the
starting point, without keeping a second copy of the existing caption hooks.

- Bring the implementation in this repository into Morphe's extension and
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

JVM tests and Android MPP packaging run in CI. APK-specific regressions need
user-provided clean/patched YouTube files and are skipped on hosted CI. The
3.0 development builds were manually tested on both declared versions,
including real translated text. Itemized 3.0.1 on-device tests for the
request-matched cleanup and unusual parser exceptions remain open.
