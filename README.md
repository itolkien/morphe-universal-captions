# Universal Captions

Caption translation enhancements for YouTube on Android, built with Morphe.

## Features

- Restore the Auto-translate menu when YouTube exposes a usable original caption source.
- Preserve existing native translated tracks and add missing choices from a reviewed 156-code YouTube Web snapshot. Android's native target-language catalog is no longer read or used as a translation allowlist.
- Use a validated original caption source associated with the active audio, not an arbitrary first track.
- Keep signed timedtext URLs and regional language distinctions intact.
- Restore readable translated subtitles without changing original caption styling or overriding explicit CC-off/manual choices.

## How 3.1.0 works

1. Identify and validate YouTube's active-audio caption source (video ID, language, VSS identity and signed `/api/timedtext` URL).
2. Preserve original/native translated caption objects. Offer only missing targets from the fixed **156-code Web-language snapshot** recorded on 2026-10-16; never translate into the original source language.
3. Preserve signatures and script/region distinctions. An ambiguous original does not justify guessing; consistent native translated tracks may provide a safe template.
4. YouTube's own timedtext service supplies actual translations. The patch does not fetch the Web player response, call an external translator or promise 156 working targets per video.

## Versions

- **3.0.1:** [stable standalone release](https://github.com/itolkien/universal-captions/releases/tag/v3.0.1), with its source and tests on `main`.
- **3.1.0 test candidate (unreleased):** `refactor/morphe-native-source`. Owned Java/Kotlin hooks and tests live here; fingerprints, settings and preferred-caption routing come from the Morphe fork pinned in [CI](.github/workflows/ci.yml). [Morphe PR #3891](https://github.com/MorpheApp/morphe-patches/pull/3891) still contains the earlier port.

The experimental build produces a **full replacement Morphe MPP**, not an additional source to stack with the official Morphe patch bundle. Use a clean supported YouTube APK and only one Morphe source during patching.

## Tests and release status

[GitHub Actions](https://github.com/itolkien/universal-captions/actions) runs the immutable 3.0.1 reference tests, the candidate's regression tests, patch metadata generation and Android MPP packaging. The build artifact is for testing only.

**3.1.0 is not released.** The [last confirmed CI](https://github.com/itolkien/universal-captions/actions/runs/39544149551) passed Java/Kotlin tests, Android MPP build and bundle verification. A user smoke test found no visible problems, but its APK version and full scenario coverage were not recorded. Both `21.20.405` and `21.41.525` need independent testing of actual translations, audio switching, native/empty menus, manual/CC-off choices and cached/styled captions. See [release validation](docs/PARITY_VALIDATION.md).

## License

Source headers and the corresponding Morphe NOTICE are preserved. No patched YouTube APK is distributed.
