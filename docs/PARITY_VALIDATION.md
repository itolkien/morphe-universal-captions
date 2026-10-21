# Release quality and regression validation

The next version must be **better than 3.0.1**, not merely equivalent. The
release gates prioritize reliable caption discovery, fewer false menus or
incorrect sources, faster recovery after video changes, safe failure modes,
readable styling, compatibility and a maintainable codebase. Code size by
itself is not a quality metric.

## Improvements in the current experimental implementation

- Read the video-specific native translation target list from the caption
  metadata using validated field relationships rather than obfuscated names.
  A source-only catalog does not force an Auto-translate row; native targets
  are retained before adding snapshot choices.
- Use the native active-audio source for menu eligibility, including when
  several original caption tracks exist (new Kotlin hook and menu tests).
- Reject ambiguous source identities or cross-video native translations
  without damaging the native list.
- Avoid stale caption styling on nested, cached, exceptional or concurrent
  native decoder calls.
- Fail closed on broken native accessors instead of crashing YouTube.
  The DEX adapter passes the manager to Java rather than eagerly invoking
  obfuscated source/catalog getters. Existing native menu rows avoid these
  reads entirely.
- Recognize native translated pens from VSS identity independently of
  translation URL-cloning rules, using linked decoder scopes without a deque.
- Run video switching and multiple app-locale stress tests; preserve explicit
  source, VSS and signed URL identity.
- Build synthetic translations from a once-validated original source:
  cache the signed URL prefix, fragment and source VSS for the full target
  catalog, instead of parsing the same native URL up to 156 times.
  A regression test bounds source getter calls independently of target count.
- Reuse one translated-track label constructor across native-list extension,
  source fallback and the independent preferred-language fallback.
- Remove the duplicate menu error-catching wrapper; test the public guarded
  manager entry point directly for broken native track accessors.
- Keep GitHub Actions on the latest development commit; older queued builds
  cannot obscure which version passed.

These improvements are supported by source-level tests. They are **not**
benchmarked speed improvements or proof of successful APK patching.

## Remaining release blockers

- Confirm the native target-list field resolver against real DEX layouts in
  both supported YouTube versions. Unit tests validate target decisions but
  cannot prove the metadata field mapping or JNI bytecode bindings.
- Verify that the new native menu-count hook and scoped pen decoder hook
  work against real DEX layouts in both supported YouTube versions.
- Perform regression comparisons on actual patched APKs, not just pure
  Java/Kotlin tests. A source-level test does not verify YouTube UI behavior,
  translation server availability or successful installation.



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
2. **Native catalog field access:** the experimental adapter now resolves
   YouTube's target-list metadata and passes it into both the translation menu
   gate and list expansion. The JVM tests verify source-only, missing,
   unsupported and mixed native-target lists. The actual obfuscated DEX field
   bindings remain **unverified on device** and must be checked on both APKs.
3. **Audio routing in the native menu-count gate:** the candidate
   now passes the active-audio translation source into the menu count hook,
   including ambiguous native lists. The Kotlin/Java build and JVM tests do
   not establish that the injected registers and target method are correct
   for the 21.20.405 and 21.41.525 DEX files. APK validation is mandatory.
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

**Release criterion:** keep the branch experimental until both APKs pass
and the remaining native-catalog field resolution and DEX hook questions are resolved.
Do not claim performance, feature superiority or reliable Android compatibility
based only on lines of code, JVM tests or a successful MPP build.
