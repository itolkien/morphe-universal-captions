# Metadata-first feasibility: Universal Captions 3.0

> **Historical research (2026-10-20).** References below to a "current"
> native target-language ABI and standalone metadata gate reflect an
> earlier experiment. The 3.1.0 Morphe-dependent branch removed target
> catalog resolution while preserving current-audio source discovery.
> See [current architecture](ARCHITECTURE.md).

Research updated 2026-10-20. This is a source-code / public-schema audit,
**not an APK or device test**. The standalone repository remains the development
source of truth; Morphe's old runtime will be replaced only after validation.

## What YouTube actually exposes

The Web player response commonly includes
`captions.playerCaptionsTracklistRenderer` with:
- `captionTracks` (`baseUrl`, `vssId`, `languageCode`, `kind`,
  `isTranslatable`, and a localized name)
- `translationLanguages` (`languageCode` and `languageName`)
- `audioTracks[].captionTrackIndices` and `defaultAudioTrackIndex`

References:
- [YouTube player response schema](https://github.com/kkdai/youtube/blob/master/response_data.go)
- [PlayerCaptionsTracklistRenderer protobuf API](https://lighttube-org.github.io/InnerTube/api/InnerTube.Protobuf.PlayerCaptionsTracklistRenderer.html)
- [yt-dlp's active YouTube subtitle extractor](https://github.com/yt-dlp/yt-dlp/blob/master/yt_dlp/extractor/youtube/_video.py)

**Important client difference:** yt-dlp explicitly comments that
*only Web/MWeb player clients provide `translationLanguages` in the response
it consumes*. This is evidence of client-dependent responses, **not proof**
that no native Android playback component can ever receive target-language
metadata. Our Kotlin `CaptionMetadataAbi` already finds a native runtime
list of target-language codes in the Android caption manager.

## Android native bridge at the time of research

| Field / behavior | Current bridge | Confidence |
| --- | --- | --- |
| Current track code, VSS ID, URL and label | `CaptionTranslationAbi` / injected getters | Structurally resolved; unit and existing historical APK evidence |
| Available native translated track objects | Translated manager getter | Structurally resolved |
| Native video target-language codes | `CaptionMetadataAbi.targetCode` and `metadata.targets` | Structurally resolved; not a guarantee that a target returns text |
| Native default caption source | `CaptionMetadataAbi.defaultSource` | Structurally resolved; not proven identical to Web's currently selected audio index |
| Source track `isTranslatable` | **Not exposed by bridge** | Unknown native field or method |
| Target language's native `languageName` | **Not exposed by bridge** | Target entry object is located, label accessor not resolved |
| `audioTracks[].captionTrackIndices` / current audio index | **Not exposed** | Web schema known, native equivalent not established |
| Active UI caption selection | Owned by Morphe preferred-language/selection logic | Do not infer from alphabetical list order |
| Subtitle request ↔ parsed srv3 document | **Not linked** | Global latest-request flag remains racy |
| Translation server success / rejection | **Not exposed** | Metadata does not prove successful timedtext response |

### Why the 156-target fallback cannot simply disappear

The project repairs videos where native Android produces *no translated
tracks/menu*. A missing native list may coexist with a working source and
Web translation support. The published 2.1.0 used native target codes
whenever provided, and a dated 156-code snapshot only for an empty native list.
An initial 21.20.405 device check of 3.0 showed ~17 native languages, because
the patch treated a nonempty native list as complete. In 3.0 development,
validated native choices are now preserved while the snapshot supplies
missing options **only when a valid source-compatible native target exists**.
Extra menu entries are best-effort and do not imply backend availability. Google's [Protobuf field-presence rules](https://protobuf.dev/programming-guides/field_presence/)
confirm that a repeated list cannot distinguish omitted from explicitly
empty. Removing fallback on this basis alone would reintroduce the bug.

A list of codes is not a list of guaranteed, successful translation results.
The server may refuse even a valid `tlang`. yt-dlp additionally documents
that same-source `tlang` may produce damaged subtitles and that some
signed requests need PO Tokens. The standalone augmentation previously
generated a target equal to its original source; the regression test
`neverReusesTheOriginalLanguageAsATranslationTarget` now covers this case.

### Why not request the Web player's metadata again?

Morphe's [YouTube Music CaptionsFetcher](https://github.com/MorpheApp/morphe-patches/blob/dev/extensions/music/src/main/java/app/morphe/extension/music/patches/lyrics/requests/CaptionsFetcher.java)
demonstrates Web Innertube requests, token/cookie support, parsing, retries
and failures. It is a separate HTTP pipeline and *does not* prove that
a second network request matches the current Android playback session.
The independent patch should first consume the existing in-process
Android caption objects and avoid redundant requests.

## Single-owner cleanup in the 3.0 development branch

The final Morphe port is intended to **replace** the old AutoTranslateCaptions
implementation, so the draft removed the runtime `CaptionIntegration` class,
its native-interface reflection and obsolete compatibility tests. The empty
menu and native target metadata still flow through `EmptyTranslationFallback`
and `CaptionTranslationList` directly. This intentionally removes coexistence
with the **old** Morphe auto-translate toggle: do not test this development
bundle with both implementations selected. The published 2.1.0 release
remains unchanged and compatible with its historical host.

No user caption preference/Off behavior was moved into this module; the
integrated Morphe patch must continue to leave that policy to the existing
preferred-caption settings.

## Native model discovery from YouTube 21.20.405 (JADX)

The developer supplied the actual decompiled `apdk`, `apdi`, `apdh`,
`bfuf`, `bfue`, `bfud`, and `bfug` classes. These are source-level
relationships, not bytecode-hook verification.

The caption manager already has the ingredients for target-specific,
audio-aware source selection:
- `bfuf.e` selects an audio entry in `bfuf.c`; `bfud.d` lists captions
  valid for that audio entry, and `bfud.e` / `bfud.f` describe its default.
- Each target entry `bfug` has language code `c`, rich-text label `d`,
  target-preferred caption indices `e`, and excluded audio indices `f`.
- Manager `bfuf.f` is the general source-index fallback. The native method
  `apdk.g()` **also requires that list to be nonempty**, so an empty list
  can still suppress the menu even if a target's own source-index list
  is nonempty. This is not automatically equivalent to a Web
  `isTranslatable` flag.
- Native caption labels are **not** read directly from another
  `String` field: `bfug.d` is a `bbbt` rich-text protobuf, decoded by
  `apkp.b()` to `Spanned`, then converted to text before assignment to
  the builder's `CharSequence` label field.

The prior optional `targetLabel` resolver assumed a direct
`String`/ `CharSequence` field. That cannot match this confirmed source
path, was unused by runtime, and has therefore been removed rather than
maintaining a speculative fallback. A new resolver must follow the
**rich-text conversion and builder assignment** in the actual DEX.

Read the detailed, version-scoped mapping in
[Native metadata 21.20.405](NATIVE_METADATA_21_20_405.md).

**Static DEX inspection completed for 21.20.405:** the builder, caption
manager, audio-change event and protobuf field relationships are documented
in [Native metadata 21.20.405](NATIVE_METADATA_21_20_405.md). Next, compare
the same semantic relationships against the other supported APK version.
No installation or device testing has been performed.

### Source identity must be track-specific

The original native audio-specific caption list can contain two source
tracks in the **same language** with different VSS identities. Since
the native translation selector works with track indices, a first-track
fallback based solely on matching language is unsafe. The regression
test `twoSameLanguageSourcesRequireNativeDefaultToAvoidWrongTrack`
now requires an exact native default when different source identities
coexist. This is a conservative bridge until full per-target eligibility
is exposed.

## A measured refactor path

1. **Already usable:** Keep native target codes, original/native track identity,
   native default-source filtering, and unmodified native translated tracks.
   Never generate the original language as its own `tlang` target.
2. **Potential high value, requires native field evidence:** Follow the
   translated-track generator's data flow back to source
   `isTranslatable` / active audio and the target entry's localized name.
   Add bridge accessors only after identifying exact types and ownership.
   Reject ambiguous matches before bytecode mutation; prove them on both
   supported versions at the final APK verification stage.
3. **Do not remove yet:** the 156-language fallback, source identity guard,
   signed URL preservation, CC/manual-selection protection, or styled-caption
   document handling. None is made redundant by language codes alone.
4. **At Morphe migration:** share one runtime core and one native adapter.
   Remove the standalone/native coexistence bridge and redundant obfuscated
   ABI discovery **from the integrated target**, where Morphe's current
   semantic fingerprints can replace them. Keep a separate lightweight
   adapter only if independent standalone patching remains a supported build.

## Reality check on code size

This draft has approximately **561 Java + 591 Kotlin production lines**
(before this research/test change). Most Kotlin is version-tolerant bytecode
patching/ABI detection; the 156-language snapshot is a **single string
constant**. More metadata can improve behavior and simplify source guessing,
but *cannot itself guarantee a 50% line reduction*. A large reduction is
more plausible in the **integrated Morphe target**, once duplicate adapters
and fingerprints have been deleted and proven unnecessary.

## Verification gate

No clean APK patching or device playback for this phase, as requested.
Keep CI regression tests and MPP build green. For any newly claimed native
metadata field, document a reproducible native instruction/data-flow
derivation before replacing the working fallback; add synthetic regressions
and finally verify both YouTube targets when the project reaches its
APK/device validation phase.
