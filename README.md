# Universal Captions

**Originally developed by [itolkien](https://github.com/itolkien) for YouTube on Android.**
Universal Captions extends the native Auto-translate language choices, recovers
missing translation menus when YouTube exposes an eligible source, and improves
the readability of translated subtitles.

## Development direction

This experimental branch contains the **real Morphe-ready caption implementation**
in `patches/src/main/kotlin/app/morphe/patches/youtube/layout/captions/` and
`extensions/youtube/src/main/java/app/morphe/extension/youtube/patches/`.
It is **not** a one-line dependency alias. Four production files are maintained
here, with Java regression tests also stored here.

The much larger standalone implementation from 3.0.1 included its own DEX
metadata resolvers, caption manager helpers and styling parser infrastructure.
Those are not duplicated in this branch because Morphe already provides those
interfaces, fingerprints, caption settings and Android extension infrastructure.
Morphe's upstream and maintainer contributions are credited in source headers
and NOTICE; Universal Captions remains the origin of the project and this
repository remains the development source.

**This branch builds a complete replacement Morphe patch source**, not a small
addon that can safely be stacked with the official Morphe bundle. Use only one
source at patch time. A clean YouTube APK is required.

## Status and installation

- **Stable standalone release:** [Universal Captions 3.0.1](https://github.com/itolkien/universal-captions/releases/tag/v3.0.1)
  on `main`, with the original 12 production classes, regression tests and
  documented clean-APK research preserved.
- **Experimental Morphe-native source:** this branch. The CI workflow overlays
  the four tracked production files and tracked JUnit regressions into a
  **pinned** Morphe fork and compiles its full Android MPP, rather than
  providing a second independent caption runtime.
- **Prerequisites:** Java 21 for Morphe build, clean YouTube APK at a supported
  version, Morphe Manager or equivalent patcher, and the "Captions" patch.
  Enable additional auto-translate languages in Morphe's Captions settings.

CI checks builds and Java tests, but successful device behavior is *not* implied.
Before releasing, verify original/manual subtitles, missing and normal translation
menus, signed URLs, multiple audio/source tracks, native language preference,
caption-off state, translated text, cached subtitles and concurrent pen parsing
on YouTube 21.20.405 and 21.41.525.

## Attribution and ownership

The original [Universal Captions 3.0.1](https://github.com/itolkien/universal-captions/tree/v3.0.1)
code, analysis and release history remain accessible unchanged in Git history.
The Morphe-compatible implementation also includes code and review contributions
from Morphe contributors; their copyright and Section 7 notices are preserved.
No patched YouTube APK is distributed.
