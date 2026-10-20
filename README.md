# Universal Captions

Caption translation enhancements for YouTube on Android, built with Morphe.

## Features

- Restore the Auto-translate menu when YouTube exposes a usable original caption source.
- Read YouTube's per-video native translation targets first; offer missing choices from the 156-code catalog only when the source is eligible, without replacing YouTube's native tracks and labels.
- Prefer YouTube's active-audio caption source rather than guessing between several originals.
- Keep signed timedtext URLs and regional language distinctions intact.
- Restore readable translated subtitles without changing original caption styling or overriding explicit CC-off/manual choices.

## Versions

- **3.0.1:** [stable standalone release](https://github.com/itolkien/universal-captions/releases/tag/v3.0.1), with its source and tests on `main`.
- **Next version (experimental):** `refactor/morphe-native-source`. The Java caption runtime, Kotlin hooks and regression tests are in this branch; shared caption fingerprints, preferences and extension infrastructure come from the Morphe revision pinned in [CI](.github/workflows/ci.yml).

The experimental build produces a **full replacement Morphe MPP**, not an additional source to stack with the official Morphe patch bundle. Use a clean supported YouTube APK and only one Morphe source during patching.

## Tests and release status

[GitHub Actions](https://github.com/itolkien/universal-captions/actions) runs the immutable 3.0.1 reference tests, the candidate's regression tests, patch metadata generation and Android MPP packaging. The build artifact is for testing only.

**The next version is not released.** Green CI does not establish that native DEX hooks work on-device. Testing must cover both YouTube `21.20.405` and `21.41.525`, including switching audio tracks, missing/native translation menus, caption preferences, signed URLs, cached subtitles, concurrent parsing and fallback errors. See the [quality and compatibility checklist](docs/PARITY_VALIDATION.md).

## License

Source headers and the corresponding Morphe NOTICE are preserved. No patched YouTube APK is distributed.
