# Morphe Universal Captions

YouTube-native subtitles in your preferred language, automatically.

Extend the **Auto-translate** language menu, remember your caption language across videos and app restarts, and prefer available captions over machine translation — all inside YouTube's native caption player.

[Add to Morphe Manager](https://morphe.software/add-source?github=itolkien%2Fmorphe-universal-captions&name=Universal%20Captions) · [Download v1.4.0](https://github.com/itolkien/morphe-universal-captions/releases/tag/v1.4.0) · [Report an issue](https://github.com/itolkien/morphe-universal-captions/issues)

## Features

- **Remember your language.** Choose a subtitle language once. New videos use it when suitable captions or a translation source are available.
- **Prefer available captions.** Use manual captions in your selected language first, then automatically generated captions (ASR), before translating another source.
- **Translate with YouTube.** Use YouTube's native caption translation and player.
- **Respect Off.** Your explicit subtitle-off preference is remembered until you enable captions again.
- **More language choices.** A curated translation-target catalogue includes Greek and many other languages. Native entries are retained without redundant regional or legacy-code alternatives.
- **Alphabetical language menu.** Existing and additional Auto-translate languages are sorted by their displayed names using the app's language settings. Special menu options keep their positions.
- **No separate translator.** No AI models, external API keys, subtitle overlays, or background caption services.

## Installation

1. Install [Morphe Manager](https://morphe.software/) and [add Universal Captions as a patch source](https://morphe.software/add-source?github=itolkien%2Fmorphe-universal-captions&name=Universal%20Captions).
2. Select a clean **YouTube 21.41.525** or **21.20.405** APK.
3. Select **Universal Captions - unlock translation languages (experimental)** and apply your patches.
4. Install the patched app and play a video with captions.
5. Open **Captions → Auto-translate** and choose your language. The choice is remembered.

You can also import the `.mpp` file from [Releases](https://github.com/itolkien/morphe-universal-captions/releases) as a local patch source. When replacing a development build, use the current release source to avoid selecting an older copy of the patch.

## Caption selection

The patch prefers manual captions in your selected language, then ASR captions in that language. When translation is needed, it ranks available source tracks by their inferred original language and manual/ASR type. If no usable source exists, YouTube retains its normal fallback behavior.

The original spoken language is inferred from caption tracks rather than detected from audio, so source ranking in multilingual videos is best-effort. Translation availability depends on the video, language and YouTube account; a language appearing in the menu does not guarantee that YouTube will serve its translation.

## Compatibility

| | |
| --- | --- |
| App | YouTube for Android |
| Supported APK versions | **21.41.525**, **21.20.405** |
| Patch version | **1.4.0** |
| Installation | Morphe Manager (`.mpp`) |
| Current device validation | Waydroid, Android 13, x86_64 |
| Earlier testing | 21.20.405 baseline; earlier features also tested on a Samsung Android phone |
| Other YouTube versions | Not verified |

YouTube's internal caption code can change between releases. The patch discovers methods through their structure and relationships, and stops with a diagnostic error when the required integration cannot be identified safely. Compatibility with every previous or future YouTube version is not guaranteed.

See [compatibility evidence and developer checks](docs/COMPATIBILITY.md) for technical validation details.

## Troubleshooting

If a language seems missing, check its localized name, the installed patch version and whether the video provides usable captions.

For bugs, [open an issue](https://github.com/itolkien/morphe-universal-captions/issues) with your YouTube version/variant, Android and Morphe Manager versions, patch version, reproduction steps and relevant logs. Include candidate descriptors when reporting an ambiguous fingerprint error. Remove personal information and signed caption URLs before posting logs.

For Waydroid diagnostics:

```bash
sudo waydroid shell -- logcat -d -s MorpheUniversalCaptions:V
```

## License

[GPL-3.0-or-later](LICENSE). Independent community project; not affiliated with YouTube or Morphe. This repository does not distribute modified YouTube APKs.
