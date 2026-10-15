# Morphe Universal Captions

More languages in YouTube’s native **Auto-translate** menu, sorted alphabetically by their displayed names.

[Add to Morphe Manager](https://morphe.software/add-source?github=itolkien%2Fmorphe-universal-captions&name=Universal%20Captions) · [Download v2.0.0](https://github.com/itolkien/morphe-universal-captions/releases/tag/v2.0.0) · [Report an issue](https://github.com/itolkien/morphe-universal-captions/issues)

## Features

- Adds all languages supported by the patch to YouTube’s native Auto-translate menu.
- Alphabetical ordering by localized display name, using Morphe’s app locale when available.
- Existing language entries and special menu options are retained.
- Preserves YouTube’s selected translation source and native caption playback.
- No AI service, external translation API, overlay or separate caption preferences.

Translation availability depends on the video, account and YouTube’s backend. A language appearing in the menu does not guarantee that YouTube will serve its translation.

## Using it with Morphe

**Morphe manages your caption preferences, source selection, visibility and Off behavior.** Universal Captions only extends the translation menu.

If your Morphe build already includes the native additional-language feature, enable **Captions** and use its **Add additional auto-translate languages** setting. The standalone addon is optional in that case; selecting it alongside the native feature leaves the menu under Morphe’s control, including when you turn that setting off.

On older Morphe builds, the addon supplies the additional languages and alphabetical ordering itself.

## Installation

1. Install [Morphe Manager](https://morphe.software/) and [add this repository as a source](https://morphe.software/add-source?github=itolkien%2Fmorphe-universal-captions&name=Universal%20Captions).
2. Select a clean YouTube **21.20.405** or **21.41.525** APK.
3. Select **Universal Captions - additional translation languages** alongside Morphe’s **Captions** patch, or use the native Captions feature when available.
4. Apply the patches, install the patched app and open **Captions → Auto-translate**.

You can also import the `.mpp` from [Releases](https://github.com/itolkien/morphe-universal-captions/releases) as a local patch source. Select only one Universal Captions version per patching session.

## Compatibility

| | |
| --- | --- |
| Patch version | **2.0.0** |
| App | YouTube for Android |
| Validated APK targets | **21.20.405**, **21.41.525** |
| Installation | Morphe Manager (`.mpp`) |

Automated checks cover independent operation and coexistence with the native Morphe integration. The release implementation was also confirmed working in user device testing. Other APK versions are unverified; compatibility with every past or future YouTube version is not guaranteed.

The native integration has been merged into Morphe’s official patches: [MorpheApp/morphe-patches#3754](https://github.com/MorpheApp/morphe-patches/pull/3754). See [compatibility evidence](docs/COMPATIBILITY.md) and [architecture](docs/ARCHITECTURE.md) for developer details.

## Feedback

[Report an issue](https://github.com/itolkien/morphe-universal-captions/issues) with your YouTube APK version, Android and Manager versions, patch version, reproduction steps and relevant diagnostic logs. Remove personal information and signed caption URLs before posting logs.

## License

[GPL-3.0-or-later](LICENSE). Community project. This repository does not distribute modified YouTube APKs.
