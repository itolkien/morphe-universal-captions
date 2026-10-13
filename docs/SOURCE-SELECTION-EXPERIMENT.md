# Source-track selection experiment

Branch: `experiment/source-track-selection`

The stable build chooses the first YouTube timed-text track as its source prototype. This experiment chooses an untranslated, real YouTube caption track, prefers manual captions to ASR when both are available, and preserves the first candidate when ranks match.

## Scope

- The selection affects which existing track is copied when creating additional native auto-translation entries.
- It does not change caption rendering, font size, positioning, line breaks or player timing.
- Virtual options and URLs with an existing `tlang` are excluded as source prototypes.
- If no safe original source is available, it leaves the native list unchanged.
- Only the experimental branch contains the change. The main branch and its published release remain unchanged.

## Device test checklist

1. Patch a clean YouTube 21.20.405 APK using the experimental branch MPP artifact (do not replace the public release source).
2. Check a video with manually supplied captions.
3. Check a video with auto-generated captions only.
4. Check a video with multiple original-language captions.
5. Check one video without captions: native caption behavior should remain unaffected.
6. Check switching caption language, switching videos, and disabling captions.
7. Compare translation text and formatting with the currently published main build.
8. Check logcat for `MorpheUniversalCaptions` and the source rank message, and report any crashes.

A successful GitHub Actions build only verifies compilation and packaging. Merge only after device testing shows no regressions.
