# Morphe Universal Captions

YouTube-native subtitles in your preferred language, automatically.

Unlock additional **Auto-translate** languages, remember your caption language across videos and app restarts, and prefer existing captions over machine translation — all inside YouTube's native caption player.

[Add to Morphe Manager](https://morphe.software/add-source?github=itolkien%2Fmorphe-universal-captions&name=Universal%20Captions) · [Download v1.3.0](https://github.com/itolkien/morphe-universal-captions/releases/tag/v1.3.0) · [Report an issue](https://github.com/itolkien/morphe-universal-captions/issues)

## Features

- **Remember your language.** Choose a subtitle language once. New videos automatically use it when a suitable source is available.
- **Prefer original caption tracks.** Use existing manual captions in the selected language first, then automatic speech-recognition captions.
- **Translate when needed.** If the selected language has no direct captions, choose an available source and use YouTube's native auto-translation.
- **Respect Off.** An explicit subtitle-off choice persists until you turn captions back on.
- **Unlock more languages.** Extend YouTube's built-in Auto-translate menu with additional target languages.
- **No separate translator.** No AI models, external API keys, subtitle overlays, or background caption services.

## Installation

1. Install [Morphe Manager](https://morphe.software/) and [add Universal Captions as a patch source](https://morphe.software/add-source?github=itolkien%2Fmorphe-universal-captions&name=Universal%20Captions).
2. Patch **YouTube 21.20.405**, selecting **Universal Captions**.
3. Install the patched YouTube app and play a video with captions.
4. Open **Captions → Auto-translate**, choose your preferred language, and continue watching. The selection is remembered.

You can also install the `.mpp` patch source from [Releases](https://github.com/itolkien/morphe-universal-captions/releases).

## Caption selection

For each new video, the patch attempts to choose:

1. Manual captions in your preferred language.
2. Automatically generated captions (ASR) in your preferred language.
3. Manual captions in the inferred original language, translated to your language.
4. Original-language ASR, translated.
5. Other available manual captions, translated.
6. Other available ASR captions, translated.
7. No usable captions: leave YouTube's normal fallback behavior.

The original spoken language is **inferred from available caption tracks**, not reliably detected from audio. Therefore source ranking in multilingual videos is best-effort; manual vs. ASR and your selected target language are prioritized explicitly.

## Compatibility

| | |
| --- | --- |
| App | YouTube for Android |
| Supported/tested APK | **21.20.405** |
| Patch version | **1.3.0** |
| Installation | Morphe Manager (`.mpp`) |
| Tested environment | Waydroid (Android 13, x86_64); earlier features also tested on a Samsung Android phone |
| Other YouTube releases | Not verified |

The patch changes version-specific YouTube internals. YouTube may change or remove caption tracks, and native translation availability varies by video. A successful patch build does not guarantee compatibility with future versions.

## Troubleshooting

To inspect diagnostic messages on a Waydroid installation:

```bash
sudo waydroid shell -- logcat -d -s MorpheUniversalCaptions:V
```

For bugs, [open an issue](https://github.com/itolkien/morphe-universal-captions/issues) with the YouTube version, Android version, patch version, reproduction steps, and relevant logs. Please remove personal information and signed caption URLs before posting logs.

## License

[GPL-3.0-or-later](LICENSE). Independent community project; not affiliated with YouTube or Morphe. This repository does not distribute modified YouTube APKs.
