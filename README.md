# Morphe Universal Captions

**[➕ Add directly to Morphe Manager](https://morphe.software/add-source?github=itolkien%2Fmorphe-universal-captions&name=Universal%20Captions)** · No manual MPP download, with remote updates once the release and source metadata are published.

**YouTube-only Morphe add-on.** [Download latest release](https://github.com/itolkien/morphe-universal-captions/releases/latest) | [Report an issue](https://github.com/itolkien/morphe-universal-captions/issues)

## One-tap installation

On Android with Morphe Manager installed, tap **[Add to Morphe Manager](https://morphe.software/add-source?github=itolkien%2Fmorphe-universal-captions&name=Universal%20Captions)**, confirm the source, and select its YouTube patch when patching. Morphe remote patch sources can update automatically. If Morphe reports missing metadata, the GitHub Release and `patches-bundle.json` publication have not finished; check [Actions](https://github.com/itolkien/morphe-universal-captions/actions).

## Manual installation (fallback)

1. Open [Releases](https://github.com/itolkien/morphe-universal-captions/releases) and download **`universal-captions-1.2.0.mpp`** (not sources or javadoc).
2. In Morphe Manager open **Settings → Patch sources → Add local source** and select the downloaded `.mpp`.
3. Select **YouTube** and patch a clean YouTube **21.20.405** APK (the version tested by the author). The patch is marked compatible with YouTube, not every app.
4. Enable **Universal Captions - unlock translation languages (experimental)** during patch selection.
5. Install and open the patched YouTube, then check **Captions → Auto-translate** on a video that has caption source tracks.

The YouTube target version and menu labels may vary by Manager build. Existing Morphe customizations may require a clean repatch. Keep a known-good APK for rollback.

**Compatibility:** Tested by the project user on YouTube 21.20.405. Other versions, Shorts, and every possible translation target have **not** been systematically validated. This modifies the native caption picker only; YouTube must serve a translated track for the chosen video. No Google Translate, AI service or API key is used.


Experimental, open-source research project to make **YouTube's native auto-translated subtitles** available in all languages supported by YouTube, including languages that may be missing from the Android caption selector.

> **Status: Research / prototype.** This repository contains an **experimental Morphe resource-patch probe** and a GitHub Actions workflow that builds an `.mpp`; it does **not** unlock missing translation languages. Do not confuse YouTube's advertised translation languages with tracks actually obtainable for a video.

## The problem

On desktop, YouTube often offers a broad range of auto-translation target languages, while the Android subtitle picker may expose fewer choices for some videos. A user observation suggests that selecting a language through another Morphe caption add-on can coincide with YouTube's own caption display showing translated captions, even when that add-on's translation overlay is stuck. The cause remains **unverified**.

## Goal

- Support all translation target languages that **YouTube itself supports**, not just Turkish.
- Reuse YouTube's native caption rendering, timing, and translation where technically possible.
- No third-party AI service, API key, or separate overlay.
- Keep the user's explicit caption-off and manually selected track choices intact.
- Fail safely: do not silently substitute an unrelated language.

## Technical investigation

Our starting reference is [Morphe PR #3566](https://github.com/MorpheApp/morphe-patches/pull/3566), which exposes `SubtitleManager` direct tracks and auto-translate tracks and selects a preferred *existing* track. **It does not add absent translation targets.**

The actual missing piece is determining whether Android can request a YouTube-supported target that is not present in `patch_getAutoTranslateCaptionTracks()`. See [docs/EXPERIMENT.md](docs/EXPERIMENT.md) for the test matrix and decision criteria.

## Native translation prototype 1.1.0 (experimental)

The source now implements an **opt-in** real YouTube translation-track augmentation patch:
`Universal Captions - unlock translation languages (experimental)`.
It clones a real caption track through YouTube's own builder, changes the target language,
native display label, `tlang` URL parameter and track ID, and returns the augmented list
to the native caption menu. No AI translation engine is involved.

**Device behavior has not been verified.** Compilation alone does not prove that YouTube
serves these translated subtitles. The broad target catalogue may contain languages
unsupported by individual videos. Expect possible patch incompatibilities or runtime
crashes; back up your working Morphe setup and test only if you accept the risk.
See [the MPP build workflow](../../actions/workflows/build-mpp.yml); use **1.1.0**,
not earlier probe packages. We have not published a stable release.

## Repository status

The language resolver, native caption-list probe and executable CI tests now live in `src/` and `patches/`; see [architecture](docs/ARCHITECTURE.md). This is **not yet an installable Morphe patch**: native Android/YouTube caption request injection still requires reverse engineering and device verification. A build workflow for an experimental `.mpp` exists at [Actions → Build experimental Morphe MPP](../../actions/workflows/build-mpp.yml). The resulting artifact is **only an opt-in integration probe**, not a caption unlocker. Do not install it as a working subtitle unlocker.

## Development milestones

1. Collect reproducible examples (video ID, YouTube APK version, available mobile languages, desktop language, result).
2. Inspect native caption track objects and identify whether target language is suppressed by UI, metadata, or the backend.
3. Prototype an opt-in test patch that requests one missing target through YouTube's own caption infrastructure; do not forge unverified track identifiers.
4. Test on device, with regression checks for off/manual captions and consecutive videos.
5. Generalize to all verified target languages and package as a Morphe add-on.

## Attribution

Independent community project, **not affiliated with YouTube or Morphe**. The Morphe PR above is a research reference; any reuse of upstream code must retain its applicable notices and license. No patched YouTube APKs are distributed.


## Experimental Morphe Manager installation (integration probe ONLY)

1. Open [Build experimental Morphe MPP](../../actions/workflows/build-mpp.yml), select **Run workflow**, and check that it succeeds.
2. Download the `universal-captions-integration-probe-mpp` Actions artifact, unzip, and obtain the `.mpp` file.
3. In Morphe Manager, add it as a **local patch source**. It exposes the optional **Universal Captions - integration probe (no language unlock)** patch.
4. The updated probe hooks YouTube's native auto-translate list and reports its candidate count in Android logcat under `MorpheUniversalCaptions`. **It does not yet change subtitle selection or create missing translation targets.**

Do not install this expecting translated captions. A real native-language unlock requires validated YouTube caption-manager bytecode integration and physical device tests.
