# Release quality and regression validation

> **Historical 3.1.0 experimental analysis.** The design and release checks
> below describe retained experimental code, **not** the current official
> Morphe runtime. Since [PR #3891](https://github.com/MorpheApp/morphe-patches/pull/3891)
> was merged, the maintained workflow tests official Morphe without
> replacing its production sources. See [upstream validation](UPSTREAM_VALIDATION.md).


The next version must be **better than 3.0.1**, not merely equivalent. The
release gates prioritize reliable caption discovery, fewer false menus or
incorrect sources, faster recovery after video changes, safe failure modes,
readable styling, compatibility and a maintainable codebase. Code size by
itself is not a quality metric.

## Improvements in the current experimental implementation

- Fill missing targets from the fixed 156-code Web snapshot. Android's native target-language metadata is neither read nor used to restrict choices; existing native translated objects are retained.
- Use the native active-audio source for menu eligibility, including when
  several original caption tracks exist (new Kotlin hook and menu tests).
- Reject ambiguous source identities or cross-video native translations
  without damaging the native list.
- Avoid stale caption styling on nested, cached, exceptional or concurrent
  native decoder calls.
- Fail closed on broken native accessors instead of crashing YouTube.
  The DEX adapter passes the manager into Java rather than eagerly invoking the native source getter. Existing native menu rows skip that lookup.
- Recognize native translated pens from VSS identity independently of
  translation URL-cloning rules, using linked decoder scopes without a deque.
- Run video switching and multiple app-locale stress tests; preserve explicit
  source, VSS and signed URL identity.
- Build synthetic translations from a once-validated original source: cache URL prefix, fragment and VSS for the 156-code Web snapshot rather than reparsing the source URL for each language.
  A regression test bounds source getter calls independently of target count.
- Reuse one translated-track label constructor across native-list extension,
  source fallback and the independent preferred-language fallback.
- Remove the duplicate menu error-catching wrapper; test the public guarded
  manager entry point directly for broken native track accessors.
- Keep GitHub Actions on the latest development commit; older queued builds
  cannot obscure which version passed.

These improvements are supported by source-level tests. They are **not**
benchmarked speed improvements or proof of successful APK patching.
[The 3.1.0 CI](https://github.com/itolkien/universal-captions/actions/runs/39544149551)
passed Java/Kotlin tests and Android MPP packaging. One user reported
no visible issue in a device smoke test; its exact APK version and
complete test scenarios were not recorded.

## Remaining release blockers

- Verify the active-audio source bridge and caption fingerprints on real DEX layouts in both supported YouTube versions; JVM tests do not establish injected-bytecode compatibility.
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
| Manual language / CC-off | Morphe's preferred-language routing remains host-owned and can synthesize one missing target through the shared Java helper; its routing tests run in the candidate suite |

## Limits — NOT verified as identical

1. **Live APK behavior:** metadata and a successful Android `.mpp` build
   do not prove that fingerprints resolve correctly against clean YouTube
   21.20.405 and 21.41.525 APKs. Neither version has been patch-tested on a
   device as part of this parity exercise. Request/catch handler hooks require
   those actual DEX layouts to be tested.
2. **Target availability:** the Android native target-language list is not consulted. The dated 156-code Web snapshot is a list of candidate choices, not proof every target returns text; empty-menu and source-only metadata cases still need device checks.
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

**Release criterion:** keep the branch experimental until both APKs pass, active-audio/menu-count/parser DEX hooks are verified and [Morphe PR #3891](https://github.com/MorpheApp/morphe-patches/pull/3891) is synchronized and reviewed.
Do not claim performance, feature superiority or reliable Android compatibility
based only on lines of code, JVM tests or a successful MPP build.
