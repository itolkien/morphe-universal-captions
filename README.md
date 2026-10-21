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

## CI: test official Morphe or a development branch

[GitHub Actions](.github/workflows/ci.yml) checks out official `MorpheApp/morphe-patches` `dev` on every `main` push or pull request, then runs Morphe's tests plus our **8 additional caption regressions**. Only one supplemental Java **test file** is added; no Morphe production code is overwritten.

To validate a new caption fix before opening a PR, open [Validate upstream Morphe captions](https://github.com/itolkien/universal-captions/actions/workflows/ci.yml) → **Run workflow** on `main`:

1. Set **Morphe source** to `itolkien/morphe-patches`.
2. Enter your **branch name or commit SHA** (e.g. `fix/caption-fallback`).
3. Leave **Build MPP** off for a quick test, or turn it on for Android testing.

Manual runs record the selected repository and exact resolved commit. The optional MPP is a complete bundle of the **chosen Morphe revision**, not an additional Universal Captions patch. No MPP is built or released on normal pushes; upstream-only changes do not trigger an automatic run here.

See [Upstream validation](docs/UPSTREAM_VALIDATION.md) for inputs, safeguards and device test guidance. Passing unit tests does not prove APK or backend translation compatibility.

## Documentation

- [Upstream CI and test procedure](docs/UPSTREAM_VALIDATION.md)
- [Architecture, historical versions and experiments](docs/ARCHITECTURE.md)
- [APK compatibility guidance](docs/COMPATIBILITY.md)
- [Historical experimental 3.1.0 parity report](docs/PARITY_VALIDATION.md)

## License and attribution

See [LICENSE](LICENSE) and [NOTICE](NOTICE). Prior release source, authorship and commit history remain available via tags and Git history. No patched YouTube APK is distributed.
