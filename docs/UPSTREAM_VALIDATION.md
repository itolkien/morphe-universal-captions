# Upstream Morphe caption validation

## Purpose

Universal Captions is an independent, caption-only research and regression
project, **not** a Morphe fork or alternative patch distribution. The
integrated runtime is owned by
[Morphe's official `dev` branch](https://github.com/MorpheApp/morphe-patches/tree/dev)
after [PR #3891](https://github.com/MorpheApp/morphe-patches/pull/3891)
merged on 2026-10-21. This repository does not vendor, mirror or maintain
the entire Morphe tree.

## Normal workflow

[CI](../.github/workflows/ci.yml):

1. Checks out our test and documentation files.
2. Checks out the official `MorpheApp/morphe-patches` repository at
   the current `dev` revision.
3. Prints the **exact tested upstream commit SHA** in the job summary and
   writes it to `morphe-commit.txt`.
4. Adds only [`MorpheCaptionUpstreamRegressionTest.java`](../tests/upstream/MorpheCaptionUpstreamRegressionTest.java)
   to Morphe's Java **test** directory. A name-collision check prevents
   overwriting upstream tests, and a `git diff --exit-code` guard prevents
   silently modifying any tracked upstream files.
5. Executes Morphe's `:patches:test` and
   `:extensions:youtube:testDebugUnitTest`, including our extra tests.

**Normal CI does not replace production sources, build an MPP, publish a
package or modify Morphe's other patches.** The older Java/Kotlin sources
under `extensions/youtube/src/main` and `patches/src/main` remain in
this repository for comparison and experimental work, not as CI overlays.

### Triggers and reproducibility

Pushes and pull requests for branch `refactor/morphe-native-source`
trigger CI. You can also start a manual run against the current
Morphe `dev` revision. The exact tested SHA is recorded for every run.

**An upstream Morphe commit alone does not trigger a run here.**
GitHub's scheduled workflows run from the repository's default branch;
we keep the 3.0.1 `main` untouched, so this experimental branch does
not claim automatic time-based monitoring. Use manual dispatch or a
branch update to revalidate the moving `dev` tip.

For regression reports, record and compare the exact tested SHA rather
than only the moving branch name.

## What the tests cover

Official Morphe tests already exercise translation-list expansion,
preferred-language routing, source and signed URL validation, original
track preservation, caption pen isolation and regional language behavior.

Our **8 upstream-compatible additional tests** target:

- Opaque signed URL suffixes and fragments after `tlang`.
- Distinct source signatures and unsafe or malformed target query fields.
- Preservation of native-only language codes and legacy source aliases.
- Rejection of other-video source candidates or inconsistent translated VSS.
- Failure of a native caption style getter after a translated document.

Java unit tests do **not** prove that a supported APK is successfully
patched or that YouTube's timedtext backend returns translated text.

## Optional upstream MPP

Use **Actions → `ci.yml` → Run workflow**, select
`refactor/morphe-native-source`, and start the manual run. Because the
workflow file also exists on the repository's default branch, the GitHub
UI can dispatch it to this experimental branch. Manual runs test the
current Morphe `dev` and then invoke Morphe's Android MPP packaging tasks.
It verifies the archive and uploads a short-lived artifact containing
the generated MPP, Morphe patch metadata and tested commit SHA.

This **official Morphe test MPP** contains no locally overlaid
experimental production classes. It is not a second Universal Captions
distribution and must not be stacked with another Morphe MPP.

For actual compatibility, patch and install clean APKs at the minimum
and maximum versions advertised by that exact Morphe revision. Check
real translations, native and missing menus, several audio tracks,
manual subtitles, CC-off, cached captions and creator-defined styling.

## Future contributions

1. Reproduce a problem against current official Morphe first.
2. Add a focused upstream regression that demonstrates it.
3. Find the smallest fix consistent with Morphe's fingerprints and old
   supported YouTube versions.
4. Submit a targeted Morphe PR instead of maintaining divergent
   production implementations in this repository.

The earlier 3.1.0 sources and larger experimental tests are retained
as reference material, **not** as a replacement runtime.
