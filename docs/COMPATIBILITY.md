# Compatibility and validation

## Supported versions

- Universal Captions **3.0.1** (maintenance release candidate).
- YouTube **21.20.405** and **21.41.525**.
- Android package: `com.google.android.youtube`.

The owner tested the 3.0 development builds on both declared YouTube
versions, including expanded menus and translated text. A 21.20.405
video previously stuck at 17 choices displayed 156. The dev.7 build,
including parser-scoped styling, was also reported working.

Those are functional tests, not a guarantee for every video, subtitle
source, target language or Android version. The request-specific parser
cleanup added in 3.0.1 still needs a separate device smoke matrix.
Do not combine the standalone addon with Morphe's older auto-translate
or translated-pen hooks.

## Automated tests

`scripts/test-core.sh` runs the Java menu tests and Kotlin resolver tests
(JDK 17, Gradle 9.0.0). The MPP workflow builds the extension against
a pinned official template, verifies Android DEX integrity and compares
the generated metadata with the checked-in version.

Extra APK-specific checks run locally only if these environment variables
point to real files:

| Environment variable | Check |
| --- | --- |
| `YOUTUBE_CAPTIONS_APK` | ABI resolution against a clean supported YouTube APK |
| `CAPTIONS_BASELINE_APK` and `CAPTIONS_PATCHED_APK` | Caption policy isolation |
| `CAPTIONS_COMBINED_APK` | Native coexistence wiring |
| `CAPTIONS_FALLBACK_APK` and `YOUTUBE_CAPTIONS_APK` | Native fallback wiring |

Hosted CI skips the APK-dependent checks because no proprietary YouTube
APK is distributed here. A green CI result is not equivalent to testing
a fully patched APK on a device.

## Remaining limits

YouTube may return an empty translation despite listing the language.
The active audio's eligible caption source and a manually chosen subtitle
are not always the same. The patch does not override CC Off/On, preferred
languages or the player's selection decisions. Creator styling on
translated captions uses a default pen for legibility.

See [architecture](ARCHITECTURE.md) and the
[Web behavior audit](WEB_PARITY_AUDIT.md). Historical 2.x validation
remains available in the release tags.
