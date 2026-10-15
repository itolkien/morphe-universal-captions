# Morphe Universal Captions

More languages in YouTube’s native **Auto-translate** menu, sorted by their displayed names.

This branch contains **2.0.0-dev.1**, a development version designed to work alongside Morphe’s existing **Captions** patch. It only expands and sorts the language menu. Morphe handles preferred languages, caption selection, visibility and remembering Off.

[Stable v1.4.0](https://github.com/itolkien/morphe-universal-captions/releases/tag/v1.4.0) remains available with its original behavior. The Manager source continues to serve that stable release while this development version is tested.

## Features

- Additional native translation targets, including Greek.
- Alphabetical ordering by localized display name, using Morphe’s app locale when available.
- Existing language entries and special menu options are retained.
- Preserves YouTube’s selected translation source and native caption playback.
- No AI service, external translation API, overlay or separate caption preferences.

Language availability depends on the video, account and YouTube’s backend. A menu entry does not guarantee that YouTube will serve a translation.

## Development installation

1. Import the development `.mpp` as a local source in Morphe Manager.
2. Select a clean YouTube **21.20.405** or **21.41.525** APK.
3. Select **Universal Captions - additional translation languages** alongside Morphe’s **Captions** patch.
4. Apply the patches and inspect **Captions → Auto-translate**.

Use one Universal Captions version per patching session. Development builds do not update the stable Manager feed.

## Compatibility

The development build targets **21.20.405** and **21.41.525**. Structural discovery validates the caption model and builder before adding the menu hook; unsupported or ambiguous layouts stop with a diagnostic error. Other APK versions are unverified, and support for every past or future YouTube release is not promised.

The corresponding native Morphe integration is under review in [MorpheApp/morphe-patches#3754](https://github.com/MorpheApp/morphe-patches/pull/3754). It was device-tested on **21.20.405**; both versions passed APK patching and SDK verification. The independent development build also passed APK/SDK checks on both targets and combined patching with Morphe Captions on 21.20.405. The user also confirmed successful device testing of **2.0.0-dev.1**. The APK version used for that independent addon device test was not specified.

See [developer checks](docs/COMPATIBILITY.md) and [architecture](docs/ARCHITECTURE.md).

## Feedback

[Report an issue](https://github.com/itolkien/morphe-universal-captions/issues) with the YouTube APK version, Android and Manager versions, patch version, reproduction steps and relevant diagnostic logs. Remove personal information and signed caption URLs before posting logs.

## License

[GPL-3.0-or-later](LICENSE). Community project. This repository does not distribute modified YouTube APKs.
