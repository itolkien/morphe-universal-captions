# Upstream Morphe caption validation

## Scope

Universal Captions is an independent, subtitle-only research and regression repository. It does not vendor, mirror or fork the full Morphe sources. After [PR #3891](https://github.com/MorpheApp/morphe-patches/pull/3891) was merged on 2026-10-21, the integrated runtime is maintained in [Morphe `dev`](https://github.com/MorpheApp/morphe-patches/tree/dev).

## Automatic checks

[CI](../.github/workflows/ci.yml) runs for pushes and pull requests targeting **`main`**. Automatic runs always use **`MorpheApp/morphe-patches` at `dev`**, regardless of a previous manual source selection.

1. Check out Universal Captions and the selected Morphe revision in separate directories.
2. Record the exact tested **repository, requested ref and resolved commit SHA** in the Actions summary and `morphe-source.txt` / `morphe-commit.txt`.
3. Copy [`MorpheCaptionUpstreamRegressionTest.java`](../tests/upstream/MorpheCaptionUpstreamRegressionTest.java) into Morphe's Java test directory only. Refuse to overwrite an existing file and assert no tracked upstream production code was modified.
4. Run `:patches:test` and `:extensions:youtube:testDebugUnitTest`, including the eight additional caption regressions.
5. Do not replace production Java/Kotlin, build an MPP or publish a release on an automatic run.

**Morphe updates alone do not trigger this repository's CI.** A manual run can validate newer upstream commits at any time.

## Test your own Morphe branch or PR commit

The [Validate upstream Morphe captions workflow](https://github.com/itolkien/universal-captions/actions/workflows/ci.yml) has three manual inputs. Open **Actions → Validate upstream Morphe captions → Run workflow**, leaving the workflow branch on **`main`**:

| Input | Official Morphe | Your development branch |
| --- | --- | --- |
| `morphe_repository` | `MorpheApp/morphe-patches` | `itolkien/morphe-patches` |
| `morphe_ref` | `dev` | e.g. `fix/caption-fallback` or a commit SHA |
| `build_mpp` | Off for tests only | On if you also need an Android MPP |

Select the exact branch **in the chosen repository**. For a reproducible result, paste a full commit SHA instead of a moving branch name. If the ref is missing, inaccessible or lacks the expected Morphe test layout, the job fails rather than silently falling back to `dev`.

A manual run with **`build_mpp = false`** tests that source only. Set it to **true** to build a complete MPP from the *same selected revision* after tests pass, validate the bundle and upload the artifact. The downloaded artifact includes `morphe-source.txt` and `morphe-commit.txt` to show precisely which commit was built.

Only the official Morphe repo and `itolkien/morphe-patches` are selectable. That is intentional: running the selected Gradle build executes code from that source, so use trusted branches and commits. The workflow has read-only repository/package permissions, does not persist checkout credentials, and does not import experimental production code.

## Additional regression coverage

The supplementary tests cover:

- Signed `tlang` URL suffixes, encoded parameters and preserved fragments.
- Invalid hosts, unsafe and duplicate target query fields.
- Distinct native source signatures, source/VSS consistency and other-video rejection.
- Native-only language targets and legacy source aliases.
- Previous translated style state after a failing native caption getter.

Morphe's own tests additionally cover translation-list expansion, preferred captions and region/script behavior. **Passing JVM tests does not prove Android patchability or device behavior.**

## Optional Android MPP

Enable **`build_mpp`** on a manual run to invoke the selected Morphe source's own metadata and Android MPP packaging tasks. This **complete Morphe test bundle** is neither a Universal Captions add-on nor a patched YouTube APK; do not stack it with another Morphe MPP.

For live validation, patch a clean APK compatible with that exact revision. Check native and missing translation menus, real translated text, dubbed audio/source changes, manual and CC-off selections, cached captions and original/translated styling.

## Legacy and future contributions

- [`v3.0.1`](https://github.com/itolkien/universal-captions/tree/v3.0.1) preserves the original standalone release and full source.
- [`experiments/3.1.0/`](../experiments/3.1.0/README.md) preserves the older unmerged prototype for research; it is not injected into CI.
- Reproduce an issue against Morphe, write a failing upstream-compatible regression, implement and test a focused fix in the Morphe fork, and submit a targeted PR upstream.
