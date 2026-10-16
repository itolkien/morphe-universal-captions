# Universal Captions 2.1.0

Promotes device-tested `2.0.1-experiment.4` to a stable minor release. Runtime Java sources and hook behavior are unchanged; public patch naming returns to **Universal Captions - additional translation languages**. Version, manifest, generated metadata, feed and documentation identify 2.1.0.

## Changes since 2.0.0

- Restore missing Auto-translate menus from validated original captions.
- Support ASR, uploaded/manual and named original sources, including multiple current-audio sources.
- Read YouTube target-language metadata dynamically, with a dated 156-code fallback only when metadata is empty.
- Preserve regional target distinctions, signed source URLs and native caption-policy ownership.

## Validation

The experiment.4 implementation passed 193,440 generated translation checks and real APK validation on 21.20.405 and 21.41.525. Native Captions coexistence on 21.41.525 passed in both patch application orders. All four experimental APKs passed Android DEX/hierarchy, alignment, signing and APK verification.

The user subsequently tested the runtime on Morphe YouTube **21.20.405**, reporting no observed problems in different target languages, captions Off/on, audio/source transitions and switching between the Arabic ASR, named English and multi-audio examples. A 6 minute 17 second capture contained 18,255 app-process log lines, one PID stream, and no observed process crash. Those logs did not expose individual caption operations; visual outcomes are user-reported. Unattributed YouTube network/UI warnings were present. Private device logs are not published in this repository or release.

Additional device feedback (2026-10-16): the user confirmed that **20.21.37–21.42.241**, including both endpoints and intermediate versions, worked in device testing with no observed problems. This follow-up is user-reported; a per-version scenario matrix and additional logs were not supplied. It extends the reported device compatibility without changing the release artifact or its declared target metadata.

Final 2.1.0 verification (2026-10-16):

- Full fresh suite against supplied native-combined fixtures: **45 passed, zero failures/skips**.
- Exact final MPP applied to clean **21.20.405** and **21.41.525** APKs. Both passed SDK DEX and cross-DEX hierarchy validation, alignment, disposable signing and APK verification.
- Embedded runtime MPE is byte-for-byte equal to device-tested experiment.4. No runtime algorithm change is included in stable promotion.
- Final ZIP integrity, root Android DEX coverage (**19 JVM / 22 DEX classes**), embedded runtime classes/checksums, manifest version, generated patch name and target metadata passed.
- Asset: `universal-captions-2.1.0.mpp`, **758,342 bytes**.
- SHA-256: `f25f28130260895efcaea4f267b26293d057b2401c0e643c53d0aa7275923622`.

Only MPP, generated patch metadata and checksum are distributed; modified APKs, signing keys and private device logs are not release assets.

## Remaining limits

Only 21.20.405 and 21.41.525 are declared targets and locally APK-validated versions. Device compatibility across 20.21.37–21.42.241 is user-reported as described above. Translation service availability is not guaranteed. Native automatic regional preference matching can select `pt` for `pt-PT`; manual menu entries are distinct. Translation-source selection follows the native current-audio default/direct order rather than guaranteeing every manual source choice.
