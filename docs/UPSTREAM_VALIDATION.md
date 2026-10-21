# Upstream Morphe caption validation

## Scope

Universal Captions is an independent, subtitle-only research and regression repository. It does not vendor, mirror, or fork the full Morphe sources. After [PR #3891](https://github.com/MorpheApp/morphe-patches/pull/3891) was merged on 2026-10-21, the integrated runtime is maintained in [Morphe `dev`](https://github.com/MorpheApp/morphe-patches/tree/dev).

## Normal CI

[Workflow](../.github/workflows/ci.yml), triggered by pushes and pull requests targeting **`main`**:

1. Check out this repository and the official `MorpheApp/morphe-patches` `dev` branch in separate temporary directories.
2. Record the **exact upstream commit SHA** in the GitHub Actions job summary and `morphe-commit.txt`.
3. Copy [`MorpheCaptionUpstreamRegressionTest.java`](../tests/upstream/MorpheCaptionUpstreamRegressionTest.java) into upstream's Java **test** directory only. Refuse to overwrite an existing file and check that no tracked upstream code was modified.
4. Run `:patches:test` and `:extensions:youtube:testDebugUnitTest`. This includes upstream Morphe tests and our eight additional subtitle regressions.
5. **Do not** replace production Java/Kotlin, modify unrelated patches, create a release or build an MPP.

A new upstream Morphe commit **does not itself trigger this repository's CI**. Start a manual run to recheck a newer `dev` revision. Always use the recorded SHA to reproduce an issue rather than relying on the moving branch name.

## Additional regression coverage

The supplementary tests cover:

- Signed `tlang` URL suffixes, encoded parameters and preserved fragments.
- Invalid hosts, unsafe and duplicate target query fields.
- Distinct native source signatures, source/VSS consistency and other-video rejection.
- Native-only language targets and legacy source aliases.
- Previous translated style state after a failing native caption getter.

Morphe's own tests additionally cover native translation expansion, preferred captions and region/script behavior. These are **unit tests**, not direct Android DEX or device compatibility evidence.

## Optional manual MPP

Open [GitHub Actions](https://github.com/itolkien/universal-captions/actions), choose **Validate upstream Morphe captions → Run workflow**, select **`main`**, and run. The workflow tests current official Morphe first, then executes Morphe's own patch metadata and Android MPP build tasks, verifies the archive and uploads an artifact with the tested upstream SHA.

No local experimental runtime sources are copied into the MPP. This artifact is a **full official Morphe test bundle**, not a second add-on and must not be stacked with other Morphe patch sources.

For live validation, patch a clean APK compatible with the tested Morphe revision. Verify native/missing Auto-translate menus, actual translated text, dubbed audio and source changes, manual and CC-off selections, cached subtitles and original/translated caption styling.

## Legacy source and future contributions

- [`v3.0.1`](https://github.com/itolkien/universal-captions/tree/v3.0.1) preserves the historical standalone release, its tests and metadata.
- [`experiments/3.1.0/`](../experiments/3.1.0/README.md) keeps our unmerged Java/Kotlin source-selection and decoder-scoping prototype visible for comparison and future improvements.
- Reproduce new problems on upstream Morphe; write a failing upstream-compatible regression, implement a focused fix, verify older APKs as appropriate, then open a targeted Morphe PR. Do not maintain a parallel shipping implementation here.
