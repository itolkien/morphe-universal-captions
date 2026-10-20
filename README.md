# Universal Captions

A standalone [Morphe](https://github.com/MorpheApp/morphe-patches) patch that expands YouTube's native **Auto-translate** language menu on Android. It uses YouTube's own caption tracks and translation endpoint—no external translation service, overlay or separate player.

[Add to Morphe Manager](https://morphe.software/add-source?github=itolkien%2Funiversal-captions&name=Universal%20Captions) · [Latest release](https://github.com/itolkien/universal-captions/releases/tag/v3.0.1) · [Report an issue](https://github.com/itolkien/universal-captions/issues)

## Features

- Adds missing translation languages while keeping YouTube's existing tracks and signed caption URLs.
- Restores the Auto-translate menu when a validated original caption source is available.
- Chooses a source associated with the current audio track instead of guessing from the first subtitle.
- Prevents creator-defined pen styles from hiding translated text, without changing original subtitle styling.
- Keeps caption visibility, manually chosen tracks and preferred-language policy under YouTube/Morphe's control.

Translation availability still depends on YouTube. A language shown in the menu is not guaranteed to return translated text for every video.

## Installation

1. In **Morphe Manager**, add this repository as a custom patch source or download the [v3.0.1 `.mpp`](https://github.com/itolkien/universal-captions/releases/tag/v3.0.1).
2. Use a clean YouTube **21.20.405** or **21.41.525** APK.
3. Select **Universal Captions - additional translation languages**, patch and install.
4. Open a captioned video and check **Captions → Auto-translate**.

**Compatibility:** This release is a standalone implementation. Do **not** combine it with Morphe's built-in Auto-translate or translated-caption pen hooks; both patch the same native methods. Other YouTube versions have not been verified for this release.

## Development

The patch is built as an independent Morphe Manager `.mpp`. The release workflow uses a pinned Morphe template, runs the Android bundle verifier and publishes patch metadata with a checksum.

Run the Java and Kotlin regression tests with `scripts/test-core.sh`. Native APK-specific checks require local APK files and are not performed by hosted CI. A passing build does not replace testing a patched APK on a device.

Technical notes: [Architecture](docs/ARCHITECTURE.md) · [Compatibility](docs/COMPATIBILITY.md) · [Caption styling](docs/CAPTION_PEN_STYLES.md) · [Release notes](docs/RELEASE_3.0.1.md).

## License

[GPL-3.0-or-later](LICENSE). Upstream attribution and notices are preserved in [NOTICE](NOTICE). No modified YouTube APK is distributed.
