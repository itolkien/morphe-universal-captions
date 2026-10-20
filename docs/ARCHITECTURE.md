# Architecture — Morphe-native prototype

This experiment replaces the standalone patch architecture with a pinned Morphe
fork checkout plus one Kotlin alias whose sole task is to select Morphe's
`Captions` patch and its transitive dependencies.

- **Own runtime code:** `integration/UniversalCaptionsMorphePatch.kt` (~19 lines).
- **Upstream runtime:** Morphe's `AutoTranslateCaptionsPatch`,
  `PreferredCaptionLanguagePatch`, `TranslatedCaptionPensPatch`, caption
  fingerprints, settings and Android extension. They are compiled unmodified
  from the pinned user fork at `74de25927da7e159db2501886a50b09964c425ca`.
- **Distribution:** a whole replacement Morphe `.mpp` source, not a separate
  extension that can be combined with the official source.
- **Verification:** upstream unit tests, Gradle Android bundling, patch metadata
  and archive checks; real APK validation remains mandatory before release.

This removes duplicative maintenance, but the resulting upstream dependency still
contains its own caption logic; the overall shipped codebase is not 19 lines.
See [legacy v3.0.1 architecture](https://github.com/itolkien/universal-captions/blob/v3.0.1/docs/ARCHITECTURE.md).
