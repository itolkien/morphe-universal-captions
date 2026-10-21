# Universal Captions

Caption translation research, regression tests and upstream contributions for [Morphe](https://github.com/MorpheApp/morphe-patches). **This is a subtitle-focused project, not a fork of Morphe.**

**Universal Captions is now integrated into official Morphe.** [PR #3891](https://github.com/MorpheApp/morphe-patches/pull/3891) was merged into [Morphe `dev`](https://github.com/MorpheApp/morphe-patches/tree/dev) on 21 October 2026. For the current caption features, **use the official Morphe patch source**; do not add this repository as a second Morphe Manager source.

The upstream integration provides additional auto-translate choices from a reviewed 156-code YouTube Web snapshot, signed caption URL and source-validation safeguards, missing Auto-translate menu recovery and a readability fix for styled translated captions. YouTube's servers still determine which translations work for each video.

## Purpose of this repository

- **Caption regressions:** [additional upstream-compatible tests](tests/upstream/MorpheCaptionUpstreamRegressionTest.java) alongside Morphe's own Java/Kotlin test suite.
- **Research and contributions:** source selection, audio-track routing, regional language codes, signed timedtext URLs and caption styling.
- **Experimental development:** our own 3.1.0 Java/Kotlin prototype and its tests are available under [`experiments/3.1.0/`](experiments/3.1.0/README.md). They are research implementations, **not the active Morphe runtime or CI source**.
- **Previous standalone releases:** [v3.0.1](https://github.com/itolkien/universal-captions/releases/tag/v3.0.1) is preserved with all its original source at the immutable [`v3.0.1` tag](https://github.com/itolkien/universal-captions/tree/v3.0.1). It is historical, not the recommended installation path.

## Browse the code

Our Java/Kotlin work is accessible directly, without copying the whole Morphe repository:

| Area | Code |
| --- | --- |
| Native translation source selection and missing language targets | [AutoTranslateCaptionsPatch.java](experiments/3.1.0/extensions/youtube/src/main/java/app/morphe/extension/youtube/patches/AutoTranslateCaptionsPatch.java) |
| Caption styling, nested decoder scopes | [TranslatedCaptionPensPatch.java](experiments/3.1.0/extensions/youtube/src/main/java/app/morphe/extension/youtube/patches/TranslatedCaptionPensPatch.java) |
| Bytecode integration | [Translation Kotlin hooks](experiments/3.1.0/patches/src/main/kotlin/app/morphe/patches/youtube/layout/captions/AutoTranslateCaptionsPatch.kt) · [Styling Kotlin hooks](experiments/3.1.0/patches/src/main/kotlin/app/morphe/patches/youtube/layout/captions/TranslatedCaptionPensPatch.kt) |
| Experimental tests | [3.1.0 test suite](experiments/3.1.0/README.md#experimental-tests) |
| Current Morphe regression tests | [MorpheCaptionUpstreamRegressionTest.java](tests/upstream/MorpheCaptionUpstreamRegressionTest.java) |

See [Experiments](experiments/README.md) for status and upstream differences. The shipped implementation remains in [official Morphe](https://github.com/MorpheApp/morphe-patches/tree/dev).

## CI: validate the official implementation

[GitHub Actions](.github/workflows/ci.yml) temporarily checks out `MorpheApp/morphe-patches` at the current `dev` commit, records its SHA and copies in **one supplemental caption test file only**. Normal `main` pushes and pull requests run upstream tests plus our **8 extra caption regressions**.

**No Morphe production file is overwritten. No MPP is built on ordinary pushes.** Morphe's own commits do not trigger our CI automatically; run the workflow manually to recheck the latest upstream revision.

## Manual Android test MPP

For a device compatibility check, open [GitHub Actions](https://github.com/itolkien/universal-captions/actions), choose **Validate upstream Morphe captions → Run workflow**, select `main` and start the run. A manually dispatched run first executes the same tests, then builds and uploads a complete **official Morphe** MPP artifact with the tested upstream commit ID.

This is an optional **test build**, not a separate Universal Captions patch source or an APK download. Do not combine it with another Morphe MPP. A successful JVM test or MPP build does not prove compatibility with every YouTube APK or that translations are available from YouTube's backend.

## Documentation

- [Upstream CI and test procedure](docs/UPSTREAM_VALIDATION.md)
- [Architecture, historical versions and experiments](docs/ARCHITECTURE.md)
- [APK compatibility guidance](docs/COMPATIBILITY.md)
- [Historical experimental 3.1.0 parity report](docs/PARITY_VALIDATION.md)

## License and attribution

See [LICENSE](LICENSE) and [NOTICE](NOTICE). Prior release source, authorship and commit history remain available via tags and Git history. No patched YouTube APK is distributed.
