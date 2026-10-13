# Morphe Universal Captions

Experimental, open-source research project to make **YouTube's native auto-translated subtitles** available in all languages supported by YouTube, including languages that may be missing from the Android caption selector.

> **Status: Research / prototype.** This repository does not yet contain a working Morphe patch or an installable `.mpp`. Do not confuse YouTube's advertised translation languages with tracks actually obtainable for a video.

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

## Development milestones

1. Collect reproducible examples (video ID, YouTube APK version, available mobile languages, desktop language, result).
2. Inspect native caption track objects and identify whether target language is suppressed by UI, metadata, or the backend.
3. Prototype an opt-in test patch that requests one missing target through YouTube's own caption infrastructure; do not forge unverified track identifiers.
4. Test on device, with regression checks for off/manual captions and consecutive videos.
5. Generalize to all verified target languages and package as a Morphe add-on.

## Attribution

Independent community project, **not affiliated with YouTube or Morphe**. The Morphe PR above is a research reference; any reuse of upstream code must retain its applicable notices and license. No patched YouTube APKs are distributed.
