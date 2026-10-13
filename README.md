# Morphe Universal Captions

Adds missing auto-translation languages to the native YouTube caption menu on Android.

[Add to Morphe Manager](https://morphe.software/add-source?github=itolkien%2Fmorphe-universal-captions&name=Universal%20Captions) · [Releases](https://github.com/itolkien/morphe-universal-captions/releases) · [Issues](https://github.com/itolkien/morphe-universal-captions/issues)

## Installation

1. Open **[Add to Morphe Manager](https://morphe.software/add-source?github=itolkien%2Fmorphe-universal-captions&name=Universal%20Captions)** on your Android device.
2. Confirm the patch source in Morphe Manager.
3. Patch YouTube **21.20.405** with **Universal Captions - unlock translation languages (experimental)** enabled.
4. Open a video with captions and select a language under **Captions → Auto-translate**.

Alternatively, download the `.mpp` file from [Releases](https://github.com/itolkien/morphe-universal-captions/releases/latest) and add it as a local patch source.

## Compatibility

| | |
| --- | --- |
| Application | YouTube for Android |
| Tested version | 21.20.405 |
| Distribution | Morphe Manager patch source |
| Other app versions | Not verified |

The patch has been tested successfully on YouTube 21.20.405. Availability of a particular translation depends on the video's captions and YouTube's translation service. Other versions and all language combinations have not been tested.

## How it works

The patch extends YouTube's existing auto-translation track list. It creates additional entries using YouTube's caption-track structure and sets the target language, display name, track identifier, and timed-text request parameter (`tlang`).

Translation and subtitle rendering remain handled by YouTube. The patch does not include a separate translation service or subtitle overlay.

## Issues

If you encounter a problem, [open an issue](https://github.com/itolkien/morphe-universal-captions/issues) and include your YouTube, Morphe Manager, and Android versions along with steps to reproduce it. Do not share account tokens or private URLs.

## License

[GPL-3.0-or-later](LICENSE). Based on native caption-track work from [morphe-ai-caption-translator](https://github.com/itolkien/morphe-ai-caption-translator).

This is an independent community project and is not affiliated with YouTube or Morphe. No modified YouTube APKs are distributed.
