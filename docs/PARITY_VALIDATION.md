# Regression parity validation

Reference: the immutable `v3.0.1` tag. Candidate:
`refactor/morphe-native-source`, built with the Morphe infrastructure pinned
to `74de25927da7e159db2501886a50b09964c425ca`.

## What the automated checks establish

The workflow runs **both** the original 3.0.1 JVM/runtime + structural DEX
tests and the candidate Morphe Kotlin + Java unit tests. The candidate's four
maintained production files are copied into an otherwise pinned Morphe tree
before testing, metadata generation and Android MPP packaging.

| Contract | Evidence |
| --- | --- |
| Same complete language catalog | Exactly 156 identical language codes in both releases; candidate exhaustively creates 155 translated targets for each original code with manual and ASR subtitles |
| Native translations stay intact | Existing objects are retained, and missing languages are cloned only when the native translations resolve to a consistent signed source |
| Mixed target-specific native sources | Preserve different existing native tracks; add only absent choices from a verified active-audio original, never the first arbitrary native target |
| Ambiguous original sources | Reject conflicting signed URL / VSS pairs without a matching current-audio source; accept a matching native source |
| URL signature, host and target identity | Preserve opaque query and fragments; reject duplicate or encoded target/identity parameters, unrelated hosts, contradictory source and target VSS values |
| Regional target identity | Do not collapse Chinese Hans/Hant or native regional tracks; preserve aliases and Portugal/Brazil Portuguese distinctness |
| Missing Auto-translate list | Restore targets from a validated source when YouTube returns an empty native translated list |
| Existing translated label sorting | Locale-aware collation; stable native objects preserved |
| Caption styles | Scope per native parser invocation, including network/cache decoding, exception cleanup, nested parses, concurrent threads and bad native metadata |
| Manual language / CC-off | Morphe's preferred-language implementation is not replaced by these caption source hooks; the platform's own routing tests run in the candidate suite |

## Limits — NOT verified as identical

1. **Live APK behavior:** metadata and a successful Android `.mpp` build
   do not prove that fingerprints resolve correctly against clean YouTube
   21.20.405 and 21.41.525 APKs. Neither version has been patch-tested on a
   device as part of this parity exercise. Request/catch handler hooks require
   those actual DEX layouts to be tested.
2. **Native target catalog / menu availability:** the 3.0.1 standalone
   implementation reads the per-video native translation-target metadata.
   Morphe's current adapter exposes its default/eligible caption source, but
   does not pass the full native target catalog to the additional-language
   runtime. This can cause a different menu decision on videos where the
   native catalog contains only the source language or an invalid target.
   Byte-for-byte behavioral equivalence is **not** established for this case.
3. **Audio routing in the native menu-count gate:** the candidate's
   `getTranslationCount` gate is given direct tracks but not the eligible
   source. If there are several valid direct originals, the candidate fails
   closed without showing a new menu row, even though the later adapter can
   resolve the eligible source. Actual app behavior requires verification.
4. **YouTube translation backend:** selecting a language is not proof of
   successful translated text; backend availability is video-dependent.
5. **Runtime integration:** preferred-language visibility, off/manual
   selections, cached decoding and parser exception paths require on-device
   checks with the exact patch combination. JVM tests cover the algorithms,
   not the full user interface or Dex bytecode at runtime.

## Clean-APK acceptance checklist

For **each** of YouTube 21.20.405 and 21.41.525:

- Patch a clean APK with one Morphe source containing the candidate and
  confirm the patch succeeds, installs and launches without a verifier error.
- Test manual uploaded, named and auto-generated captions, both an already
  translated video and a video with no Auto-translate menu.
- Verify the native language rows and signed URL bytes remain unchanged.
  Compare expanded language choices, actual translated text and sorting
  against 3.0.1, including `tr`, `iw`, `zh-Hans`, `zh-Hant`,
  `pt` and `pt-PT`.
- Switch a video's audio tracks and caption sources repeatedly; ensure
  translated tracks remain bound to the active audio, not an old source.
- Test original creator-styled captions and auto-translated counterparts
  (including cached requests, repeated video switches and concurrent loads).
- Test captions explicitly disabled, manually selected captions and
  preferred-caption-language interactions.
- Confirm no crashes or subtitle style leakage after a failed caption
  request, interrupted network response or decoder error.

**Release criterion:** keep the branch experimental until both APKs pass,
and until the native target-catalog and menu-count differences are addressed
or consciously accepted as behavior changes. Do not claim 100% feature parity
based on code size, unit tests or a successful MPP build alone.
