# Universal Captions

YouTube caption translation research, regressions and upstream contributions for [Morphe](https://github.com/MorpheApp/morphe-patches). **This repository is not a Morphe fork.**

The core Universal Captions improvements were integrated into [Morphe `dev` via PR #3891](https://github.com/MorpheApp/morphe-patches/pull/3891). Morphe now maintains the integrated auto-translate, native caption-source and translated-caption styling patches.

## What stays here

- **Upstream caption quality:** research and additional regression tests focused exclusively on YouTube subtitles and translation.
- **Experimental sources:** our previous Java/Kotlin 3.1.0 implementation and its tests are kept on `refactor/morphe-native-source` for reference. **Normal CI never substitutes them into Morphe.**
- **Historical standalone release:** [Universal Captions 3.0.1](https://github.com/itolkien/universal-captions/releases/tag/v3.0.1) and its `main` sources are untouched.

The integrated work includes 156 candidate auto-translate language codes, native translated track preservation, signed timedtext URL and caption-source checks, Preferred Caption Language integration and readable translated subtitle styling. A language in the menu does not guarantee a successful translation response from YouTube's backend.

## CI: official Morphe, caption tests only

[GitHub Actions](.github/workflows/ci.yml) checks out **official `MorpheApp/morphe-patches` `dev`**, records the exact upstream commit and installs only our [supplementary caption regression test](tests/upstream/MorpheCaptionUpstreamRegressionTest.java) in its temporary **test directory**.

Normal runs execute Morphe's own Kotlin/Java tests plus our additional caption regressions. **No upstream production file is replaced and no MPP is generated or published during normal CI.**

A push or pull request targeting this experimental branch triggers a run. You can also start one manually. **Upstream Morphe commits alone do not trigger this workflow**; use *Run workflow* to check newer upstream revisions. See [Upstream validation](docs/UPSTREAM_VALIDATION.md).

## Optional manual MPP

When actual Android testing is needed:

1. Open [GitHub Actions](https://github.com/itolkien/universal-captions/actions), select **Validate upstream Morphe captions** and click **Run workflow**.
2. Select branch `refactor/morphe-native-source` and click **Run workflow**. A manual run tests current Morphe `dev` and automatically builds the MPP after the tests pass.
3. Download the `official-morphe-captions-<run-id>` artifact. It includes Morphe's MPP and the exact tested upstream commit.

That optional MPP contains **unmodified official Morphe production code**, not the experiment retained here. It is a test artifact, not a separate Universal Captions release; patch a clean compatible YouTube APK using only one Morphe bundle. No patched YouTube APK is distributed.

## Documentation

- [Upstream CI, manual MPP and regression scope](docs/UPSTREAM_VALIDATION.md)
- [Architecture and retained experimental code](docs/ARCHITECTURE.md)
- [Compatibility and real-device checks](docs/COMPATIBILITY.md)
- [Historical 3.1.0 parity and release investigation](docs/PARITY_VALIDATION.md)

## License

Original authorship and attribution headers are preserved. See the included [LICENSE](LICENSE) and [NOTICE](NOTICE). Official Morphe sources are only checked out temporarily by CI.
