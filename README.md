# Morphe Universal Captions

**Unlock more of YouTube's native auto-translation languages on Android.**

Morphe Universal Captions adds language choices that may be missing from YouTube's Android **Captions → Auto-translate** menu. It uses YouTube's own caption playback and translation requests—**no AI subscription, external translation API, or subtitle overlay**.

[![Add to Morphe Manager](https://img.shields.io/badge/Add%20to-Morphe%20Manager-2563eb?style=for-the-badge)](https://morphe.software/add-source?github=itolkien%2Fmorphe-universal-captions&name=Universal%20Captions)
[![Latest release](https://img.shields.io/github/v/release/itolkien/morphe-universal-captions?style=for-the-badge&label=Release)](https://github.com/itolkien/morphe-universal-captions/releases/latest)

## Install

1. On your Android device, tap **[Add to Morphe Manager](https://morphe.software/add-source?github=itolkien%2Fmorphe-universal-captions&name=Universal%20Captions)**.
2. Confirm the new patch source in Morphe Manager.
3. Select **YouTube** (supported target: **21.20.405**) and enable **Universal Captions - unlock translation languages (experimental)**.
4. Patch and install YouTube using Morphe Manager.
5. Open a video with subtitles, then go to **Captions → Auto-translate** and select your language.

**Manual alternative:** Download the plain `.mpp` file from [Releases](https://github.com/itolkien/morphe-universal-captions/releases/latest) and add it as a local patch source in Morphe Manager. Do not use the `-sources` or `-javadoc` files.

## Features

- 🌐 Adds a broad set of translation language choices, not just Turkish.
- ▶️ Uses YouTube's native subtitle display and timing.
- 🔑 No API key, third-party translation provider, or separate overlay.
- 📦 Installable as a Morphe patch source from GitHub.
- 🔓 Free and open source.

## Compatibility

| Item | Status |
| --- | --- |
| App | YouTube for Android |
| Tested app version | **21.20.405** |
| Morphe Manager | Add as a third-party patch source |
| Other YouTube versions | Not yet verified |
| All videos / all languages | Not guaranteed; depends on YouTube caption availability |

> [!IMPORTANT]
> This is an **experimental, community-maintained patch**, not an official Morphe or YouTube feature. It has worked in a user test on YouTube 21.20.405, but broader compatibility and every language/video combination have not been verified. Keep a working backup of your patched YouTube before trying new versions.

## How it works

The patch extends YouTube's native auto-translation track list. For each additional target language, it builds a caption-track entry using YouTube's existing track structure and sets the language label, identifier, and `tlang` target in the timed-text URL. The captions are still requested and rendered by YouTube.

If YouTube does not provide captions or cannot serve a target translation, the patch cannot generate subtitles independently.

## Feedback and development

Found a missing language, a crash, or an incompatible YouTube version? [Open an issue](https://github.com/itolkien/morphe-universal-captions/issues) with your YouTube version, Morphe version, Android version, and reproduction steps. **Do not attach account tokens or private URLs.**

[Releases](https://github.com/itolkien/morphe-universal-captions/releases) · [Source code](https://github.com/itolkien/morphe-universal-captions) · [Report a bug](https://github.com/itolkien/morphe-universal-captions/issues)

## Credits & license

Based on native caption-track techniques developed in [morphe-ai-caption-translator](https://github.com/itolkien/morphe-ai-caption-translator), with Morphe community research as a reference. Independent project; not affiliated with Morphe or YouTube.

Licensed under [GPL-3.0-or-later](LICENSE). No modified YouTube APKs are distributed.
