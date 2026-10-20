# Universal Captions — Morphe-native experiment

**This branch is an experimental architecture replacement, not a 3.0.1 update.**
For the released standalone addon, use [v3.0.1](https://github.com/itolkien/universal-captions/releases/tag/v3.0.1)
or the repository's `main` branch.

## Design

Instead of maintaining a second caption manager, hand-written DEX ABI resolvers,
a translated-caption parser, and a second Android extension, this experiment
builds from [itolkien/morphe-patches PR #3891](https://github.com/MorpheApp/morphe-patches/pull/3891)
at a **fixed SHA** and adds only the tiny
[`UniversalCaptionsMorphePatch.kt`](integration/UniversalCaptionsMorphePatch.kt).

The alias depends on Morphe's existing `Captions` patch, which already includes
its auto-translate, preferred-language and translated-pen dependencies. It does
**not** inject another set of native caption hooks.

**This is not a small, standalone Morphe Manager addon anymore.** The CI artifact
contains an entire derived Morphe patch bundle (including upstream extension and
other patches). Use it **instead of** the official Morphe source, never alongside
another version of that source. Do not stack the old Universal Captions 3.0.1
patch on top of it.

## Build

The [CI workflow](.github/workflows/ci.yml) checks out the pinned Morphe fork,
copies the ~20-line alias into Morphe's existing Kotlin patch module, runs the
upstream tests, generates new patch metadata and builds a complete Android
`.mpp` file. It does not publish a release or patch/distribute any YouTube APK.

After patching a clean YouTube APK using the generated custom Morphe bundle,
select **Universal Captions (Morphe integration)** and enable **Additional
auto-translate languages** under Morphe's Captions settings. The alias selects
the existing `Captions` dependency; it doesn't independently override that
setting's on/off state.

## Parity limits and release gates

Morphe PR #3891 is still under review. Its implementation is **not identical**
to the standalone 3.0.1 runtime: it uses different pen-request state handling,
and its translation expansion and native-source fallback have different validation
rules. A dramatic drop in locally maintained lines does not establish equivalent
behavior or fewer total lines across dependencies.

Before considering a release, patch and test clean YouTube 21.20.405 and
21.41.525 APKs, with native/manual captions, the missing Auto-translate menu,
expanded languages, original pen styling, cached and overlapping parser requests,
and videos with several audio/source tracks. A green MPP build alone is not enough.

Earlier implementations, tests and native analysis remain in the
[3.0.1 tag](https://github.com/itolkien/universal-captions/releases/tag/v3.0.1)
and Git history. Preserve Morphe's GPLv3 Section 7 user notice in downstream
binary distributions. No YouTube APK is distributed here.
