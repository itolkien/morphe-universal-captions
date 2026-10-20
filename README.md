# Universal Captions

YouTube on Android sometimes shows only a short Auto-translate language list, even when the same video has many more choices on YouTube Web. Some videos have subtitles but no Auto-translate menu at all.

Universal Captions fixes those cases inside the YouTube app. It uses YouTube's own caption tracks and translation endpoint. No external translator, overlay or separate player.

I developed Universal Captions independently as a standalone patch. The Morphe maintainers later asked me to integrate it into their patch set. This repository is the original project; Morphe Manager is one way to install it.

[Add to Morphe Manager](https://morphe.software/add-source?github=itolkien%2Funiversal-captions&name=Universal%20Captions) · [Download 3.0.1](https://github.com/itolkien/universal-captions/releases/tag/v3.0.1) · [Issues](https://github.com/itolkien/universal-captions/issues)

## What changed in 3.0

The earlier versions worked for many videos, but some still stopped at YouTube's usual 16–18 languages. The problem wasn't the language list: YouTube was picking different source captions for different target languages, and the patch couldn't safely decide which one to use for the others.

I went through the original YouTube 21.20.405 DEX to understand its caption manager instead of adding another language-specific workaround. The useful part turned out to be YouTube's own metadata: which captions belong to the current audio track, which source indices are eligible for translation, and how those indices are used to build a translated track.

The patch now follows that native source-selection path. It keeps YouTube's existing translated tracks and their signed URLs, then fills in missing languages using a source tied to the current audio. The bytecode patch finds those relationships structurally rather than relying on obfuscated class names.

I also reworked translated subtitle styling. Some creators use custom caption pens that can make translated text invisible. The old workaround kept one global flag for the latest timedtext request, which wasn't safe when requests overlapped. In 3.0, styling is tied to the caption being parsed, including the network and cache paths. Original subtitles keep their original styling.

The result is a much cleaner separation between YouTube's native caption decisions and the extra language choices we add. It also means we can stop guessing a source just because two tracks happen to have the same language.

## 3.0.1 maintenance

This update tightens caption parser cleanup when a native exception handler runs
outside the matching decoder call. It also removes unused Android test wrappers
and fixes the version and download metadata used by Morphe Manager.
Source selection, translation language choices and caption visibility are unchanged.

## Features

- Broad Auto-translate language selection (roughly 155–156 choices when the source allows it).
- Restores missing Auto-translate menus for eligible caption tracks.
- Supports auto-generated, uploaded and named subtitles.
- Preserves YouTube's native translations, signed URLs and current-audio source selection.
- Keeps original subtitle styles while avoiding invisible translated text on affected videos.
- Sorts languages by their displayed names, with distinct regional options where available.

Showing a language in the menu doesn't guarantee YouTube will return translated text for every video. The server can still restrict individual languages or tracks.

## Installation

1. Install [Morphe Manager](https://morphe.software/) and add this repository as a custom patch source, or download `universal-captions-3.0.1.mpp` from the [3.0.1 release](https://github.com/itolkien/universal-captions/releases/tag/v3.0.1).
2. Start with a clean YouTube **21.20.405** or **21.41.525** APK.
3. Select **Universal Captions - additional translation languages** and patch the APK.
4. Open a video with captions, then go to **Captions → Auto-translate**.

**Upgrading from 2.1.0:** Version 3.0 includes its own auto-translate and translated-caption styling hooks. Do not stack it with an older Morphe **Captions** patch that contains the same hooks; that integration has not been deduplicated upstream. For the standalone 3.0 setup, use Universal Captions on its own. You will need to patch and reinstall YouTube to apply the new version.

The patch does not force captions on, change your selected language or provide an external translation service.

## Compatibility

| | |
| --- | --- |
| Release | **3.0.1** |
| YouTube targets | **21.20.405**, **21.41.525** |
| Package | `com.google.android.youtube` |
| Format | Morphe Manager `.mpp` |

Both declared APK versions were patched and tested manually with the 3.0 development builds, including expanded language lists and translated text. The 21.20.405 Waydroid tests also confirmed that a menu previously stuck at 17 languages now returned 156. The final release uses the same source-selection approach. These tests don't imply that every target language works on every video or that all other YouTube versions are compatible.

The patch intentionally leaves caption visibility and preferred-language policy to YouTube/Morphe. Matching every manually selected source track, automatic regional preference matching and backend language availability still have limitations.

For the technical details, see the [native caption metadata notes](docs/NATIVE_METADATA_21_20_405.md), [caption style handling](docs/CAPTION_PEN_STYLES.md) and [3.0.1 release notes](docs/RELEASE_3.0.1.md).

## Feedback and license

Please [open an issue](https://github.com/itolkien/universal-captions/issues) with the YouTube version, patch version and steps to reproduce. Remove personal details and signed caption URLs from logs before posting them.

Licensed under [GPL-3.0-or-later](LICENSE). Upstream copyright and license notices are preserved in [NOTICE](NOTICE). This repository doesn't distribute patched YouTube APKs.
