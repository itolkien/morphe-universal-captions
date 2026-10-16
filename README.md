# Morphe Universal Captions

Restores missing YouTube **Auto-translate** menus and offers translation languages sorted by their displayed names.

[Add to Morphe Manager](https://morphe.software/add-source?github=itolkien%2Fmorphe-universal-captions&name=Universal%20Captions) · [Download v2.1.0](https://github.com/itolkien/morphe-universal-captions/releases/tag/v2.1.0) · [Report an issue](https://github.com/itolkien/morphe-universal-captions/issues)

## Features

- Uses YouTube's per-video target-language metadata; no video or source-language allowlist.
- Uses a dated 156-language fallback catalog only when native language metadata is empty.
- Supports original automatic speech captions (ASR), uploaded/manual captions and named tracks.
- Restores the native Auto-translate option when original captions exist but the translation menu is missing.
- Preserves existing native translations and signed source URLs. With multiple originals, uses the current audio's native default, or native direct-list order if no eligible default exists.
- Localized alphabetical language ordering, including distinct regional targets.
- No external translation service, overlay or separate caption preference UI.

Translation availability depends on the video, account and YouTube's backend. A language appearing in the menu does not guarantee translated text for every video.

## Using it with Morphe

**Morphe owns preferred language, caption visibility, Off and explicit user selections.** This addon extends language menus and supplies the missing-menu fallback.

Use it alongside Morphe's **Captions** patch. If your Morphe build includes **Add additional auto-translate languages**, the addon respects that setting when supplementing existing native menus. The missing-menu fallback is enabled by selecting this addon and works independently of that toggle.

## Installation

1. Install [Morphe Manager](https://morphe.software/) and [add this repository as a source](https://morphe.software/add-source?github=itolkien%2Fmorphe-universal-captions&name=Universal%20Captions), or import the release `.mpp` locally.
2. Select a clean YouTube **21.20.405** or **21.41.525** APK.
3. Select **Universal Captions - additional translation languages** alongside Morphe **Captions**. Select only one Universal Captions version; replace earlier experimental imports.
4. Apply the patches, install the patched app, and open **Captions → Auto-translate**.

Updating the patch source does not modify an already installed YouTube app; reapply the patches to use the new version.

## Compatibility and validation

| Item | Validated scope |
| --- | --- |
| Patch version | **2.1.0** |
| App | YouTube for Android |
| Declared APK targets / local APK validation | **21.20.405**, **21.41.525** |
| User-reported device compatibility | **20.21.37–21.42.241**, including intermediate versions |
| Installation | Morphe Manager (`.mpp`) |

The release uses the same runtime as device-tested experiment.4. Automated checks cover all 156 × 155 catalog source/target pairs for ASR and manual source shapes in four display locales, native Morphe coexistence in both patch orders, and unchanged caption-policy classes. User device testing on **21.20.405** reported working translations, Off/on, source/audio changes and video switching; captured process logs showed no crash.

On 2026-10-16, the user additionally confirmed successful device testing of **20.21.37**, **21.42.241** and intermediate versions across that range, with no observed problems. This is user-reported compatibility evidence; no per-version test matrix or additional logs were supplied for this follow-up. Local automated APK validation and the shipped target metadata remain limited to the two versions listed above.

**Known limits:** Morphe's existing automatic regional matching can choose generic `pt` for preference `pt-PT`; manual menu targets remain separate. The fallback's source is the current-audio default/order; exact tracking of every manual source selection is not guaranteed. Server restrictions and absent original captions remain outside the addon's control.

See [2.1.0 release evidence](docs/RELEASE_2.1.0.md), [fallback design and experiment history](docs/EMPTY_TRANSLATION_EXPERIMENT.md) and [historical compatibility evidence](docs/COMPATIBILITY.md). The native additional-language feature was integrated through [MorpheApp/morphe-patches#3754](https://github.com/MorpheApp/morphe-patches/pull/3754).

## Feedback

[Report an issue](https://github.com/itolkien/morphe-universal-captions/issues) with YouTube, Android, Manager and patch versions, the video, source/target language, and reproduction steps. Remove personal information and signed caption URLs before publishing logs.

## License

[GPL-3.0-or-later](LICENSE). Community project. This repository does not distribute modified YouTube APKs. Test-only upstream fixtures retain their own license and NOTICE files.
