# YouTube Web behavior audit — Universal Captions 3.1.0 candidate

> **Historical 3.1.0 experimental analysis.** The design and release checks
> below describe retained experimental code, **not** the current official
> Morphe runtime. Since [PR #3891](https://github.com/MorpheApp/morphe-patches/pull/3891)
> was merged, the maintained workflow tests official Morphe without
> replacing its production sources. See [upstream validation](UPSTREAM_VALIDATION.md).


This compares the experimental 3.1.0 Morphe-based runtime with YouTube Web. Historical 3.0 builds had device tests; the current MPP received one positive but unversioned smoke test. Full two-APK coverage or server-side Web parity is not claimed.

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

| Behavior | Current 3.1.0 behavior | Automated coverage | Remaining uncertainty |
| --- | --- | --- | --- |
| No original caption track | Do not fabricate transcription or expose a fake translation source | Empty / malformed original tests | Backend-specific exceptions cannot be proven offline |
| Native translations exist | Keep native objects and add only missing, valid targets | Signed URL, dedup, alias and idempotence tests | Server availability can still vary per target |
| Native Android target-language metadata has codes | Not read or used as a gate; preserve existing native translated tracks and add missing 156-snapshot choices | Language-count, native-only track and source-identity tests | Individual target codes may not work server-side |
| Native translated list empty or absent | Restore a missing menu and target choices if a validated source exists | Menu-count, empty-list and source-selection tests | Actual server translation and menu behavior need device checks |
| Snapshot targets | Labels and URLs are generated locally only for a validated original source | 156×155 source/target cases, four UI locales | YouTube can reject entries independently of menus |
| ASR vs uploaded/named tracks | Derive only from an identity-validated original timedtext track | ASR/manual/named, VSS, kind and URL tests | Whether an individual native track is translatable is not exposed |
| Multiple audio/source tracks | Intersect native general source indices with the **current** audio's caption indices; preserve YouTube's existing per-target native translations | Mixed sources, distinct VSS, cross-video, active-audio and native-eligible-source tests | Snapshot extras still cannot prove backend support for every target |
| Regional targets and old aliases | Keep `pt` and `pt-PT`, both Chinese scripts, and language-tag regions distinct | Alias and regional-code tests | Web localization may use its own preferred display name |
| Web's active target ordering | Preserve native selection; sort translated language choices alphabetically | Sort, stable labels and pseudo-option tests | Web can pin the currently selected target above the sorted list; we do not have a native selected-track getter in this addon |
| Dubbed audio tracks | Use the native audio entry's caption indices, then exact default/unambiguous source only if needed | Current-audio source indices, source-switch and cross-video tests | A later manually selected subtitle is not necessarily the native audio default |
| Selected track, CC Off/On and preferences | Never force CC visibility, overwrite manual selection or auto-enable tracks | Preferred policy and Off/integration tests | Morphe preference patch has its own regional matching |
| Styled translations | Use network/cache decoder-local pen scopes instead of a global last-request flag | Nested/concurrent cleanup tests, MPP build and historical 21.20.405 DEX inspection | 3.1.0 still needs two-APK coverage; full Web styling parity is not claimed |
| Signed YouTube requests | Replace only the actual `tlang` value; retain the rest of the opaque URL | Encoded key, fragment, duplicate, HTTPS/host tests | A valid URL is not proof of a nonempty server response |

## Current runtime architecture

The pinned Morphe preferred-caption patch identifies the active-audio translation source. Our Kotlin adapter exposes this source, and Java validates its signed timedtext URL, video and VSS identity. Existing native translated objects are preserved, and only missing languages from a fixed **156-code Web snapshot** are synthesized. The missing native Auto-translate row is restored only when a safe original source exists.

`TranslatedCaptionPensPatch` Java/Kotlin scopes styling to the actual network/cache parser request. Morphe's pinned preferred-language routing can synthesize one missing preferred target through the shared `createTranslation()` helper; it is not strictly selection-only.

The candidate neither reads Android's target-language catalog nor makes separate Web player or third-party translation requests.

## Important open engineering questions

### Native target-language metadata is not a Web availability gate

Earlier implementations relied on Android metadata target codes to decide if Web languages should be shown. Protobuf repeated fields cannot distinguish omitted from explicitly empty values, and Web has its own `translationLanguages` array. The 3.1.0 candidate therefore does **not** read Android target-language metadata: a source-only native list cannot block extra Web-snapshot choices when a safe translation source exists. This still does not guarantee that YouTube's backend returns text for every requested `tlang`.

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

The current parser patch does not infer the parser's identity from the process's
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

YouTube Web player responses expose track-specific `isTranslatable` flags. Our current Android interface does not. A 156-code list cannot prove that timedtext responds with nonempty captions; any future native translatability accessor requires direct DEX evidence on both APK versions.

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

## Morphe integration status

The candidate builds a **full replacement Morphe MPP**, not an additional source to stack on official Morphe. [Morphe PR #3891](https://github.com/MorpheApp/morphe-patches/pull/3891) is still based on the earlier 3.0.1 port, not the current 3.1.0 cleanup. Sync upstream after testing both advertised YouTube versions, without duplicating pen/caption hooks or replacing unrelated CC/cookie settings.

## Validation status

[3.1.0 CI](https://github.com/itolkien/universal-captions/actions/runs/39544149551) passed JVM/Kotlin tests and MPP packaging. The user reported no visible problems in an unversioned 3.1.0 smoke test. Earlier 3.0 builds had tests on both advertised versions; this candidate still needs independent clean-APK tests on 21.20.405 and 21.41.525, including decoder error paths.
