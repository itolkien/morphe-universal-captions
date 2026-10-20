# Morphe Universal Captions

Restores missing YouTube **Auto-translate** menus and offers translation languages sorted by their displayed names.

[Add to Morphe Manager](https://morphe.software/add-source?github=itolkien%2Fmorphe-universal-captions&name=Universal%20Captions) · [Download v2.1.0](https://github.com/itolkien/morphe-universal-captions/releases/tag/v2.1.0) · [Report an issue](https://github.com/itolkien/morphe-universal-captions/issues)

## Features

- Uses YouTube's per-video target-language metadata; no video or source-language allowlist.
- **3.0 dev:** Preserves YouTube's native languages and signed subtitle objects, then adds remaining choices from a 156-language compatibility catalog when a compatible native translation target is established. Missing/empty catalogs still use the snapshot. The published 2.1.0 behavior is unchanged.
- Supports original automatic speech captions (ASR), uploaded/manual captions and named tracks.
- Restores the native Auto-translate option when original captions exist but the translation menu is missing.
- Preserves native translations and signed URLs; with multiple originals, uses YouTube's current-audio caption-index eligibility before considering a verified native default or an unambiguous source.
- Localized alphabetical language ordering, including distinct regional targets.
- No external translation service, overlay or separate caption preference UI.
- In **3.0.0-dev.7**: restores visible auto-translated text for channels with incompatible custom caption pens, while preserving the original track's creator styling ([Morphe #3837](https://github.com/MorpheApp/morphe-patches/pull/3837)).

Translation availability depends on the video, account and YouTube's backend; a menu entry is not a guarantee of text on every video. The owner has manually verified the expanded menu and translated subtitles on YouTube 21.20.405 and 21.41.525 using dev.5. The owner confirmed dev.6 remains functional on both YouTube versions and has now reported dev.7 working after its parser-scoped pen and fallback refactor. The precise dev.7 test matrix by APK version is not documented, so broader compatibility and complete Web parity are not claimed.

## Using it with Morphe

**Morphe owns preferred language, caption visibility, Off and explicit user selections.** This addon extends language menus and supplies the missing-menu fallback.

The released **2.1.0** addon works alongside Morphe's **Captions** patch, respects the native **Add additional auto-translate languages** setting in nonempty menus, and can restore an absent Auto-translate menu. The **unreleased 3.0 development branch** is a single-owner replacement: its legacy Morphe `CaptionIntegration` adapter has been removed, and it must **not** be used alongside the old Morphe auto-translate or translated-pen implementations. The existing preferred-language, explicit choice and CC visibility policy remains Morphe's responsibility.

### Caption styling

The **3.0.0-dev.7** development patch includes the translated-caption pen fix from [Morphe #3837](https://github.com/MorpheApp/morphe-patches/pull/3837) **inside Universal Captions itself**. The addon hooks YouTube's native network/cache XML decoders and shared srv3 pen lookup, using an identity-validated **per-parser** style scope for translated tracks. Original tracks keep their creator-defined styles. The published **2.1.0** release does not contain this change.

This adapted fix retains Morphe's [GPLv3 additional terms](NOTICE). This is the source-of-truth implementation for the planned Morphe port. The upstream port should **replace**, not retain alongside, Morphe's existing #3837 hooks. Running this development patch alongside a Morphe build that already includes #3837 is not yet device-verified for duplicate hooks. The previous global latest-request flag is removed; decoder-local track identity now prevents different in-flight caption requests from sharing style state. The owner reports functional dev.7 playback; confirm the exact dev.7 test coverage on each supported APK before making a public release. See [caption style design and verification](docs/CAPTION_PEN_STYLES.md).

## Installation

1. Install [Morphe Manager](https://morphe.software/) and [add this repository as a source](https://morphe.software/add-source?github=itolkien%2Fmorphe-universal-captions&name=Universal%20Captions), or import the release `.mpp` locally.
2. Select a clean YouTube **21.20.405** or **21.41.525** APK.
3. For the released **2.1.0** addon, select **Universal Captions - additional translation languages** alongside Morphe **Captions**. Do not deploy the experimental 3.0 branch with an unchanged Morphe #3837 patch until the integration is deduplicated.
4. Apply the patches, install the patched app, and open **Captions → Auto-translate**.

Updating the patch source does not modify an already installed YouTube app; reapply the patches to use the new version.


### 3.0 refactor validation

The dev.5 patch was tested on both original YouTube 21.20.405 and
21.41.525 APKs, including the previously limited 17-language menu.
The verified native caption source comes from the *current* audio
entry's eligible-caption indices; existing native translations are preserved.
Dev.6 removed temporary `UC3Probe` diagnostics without changing
the source-selection policy, and the owner successfully tested that release.
Dev.7 consolidates fallback selection into a lazy augmentation pass and replaces
the prior process-wide caption style flag with parser-scoped native track
association for both network and cache paths. The owner reports a successful dev.7 functional test. Confirm the precise
21.20.405 and 21.41.525 coverage before the public 3.0 release.

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

**Known limits:** Morphe's existing automatic regional matching can choose generic `pt` for preference `pt-PT`; manual menu targets remain separate. When available, source selection uses YouTube's active-audio caption-index eligibility, then an exact native default or an unambiguous direct-list source. Exact tracking of every manual source selection is not guaranteed. Server restrictions and absent original captions remain outside the addon's control.

For the ongoing comparison with the YouTube Web player and outstanding behavior gaps, see the [web parity audit](docs/WEB_PARITY_AUDIT.md) and [native metadata feasibility research](docs/METADATA_FIRST_RESEARCH.md). See [2.1.0 release evidence](docs/RELEASE_2.1.0.md), [fallback design and experiment history](docs/EMPTY_TRANSLATION_EXPERIMENT.md) and [historical compatibility evidence](docs/COMPATIBILITY.md). The native additional-language feature was integrated through [MorpheApp/morphe-patches#3754](https://github.com/MorpheApp/morphe-patches/pull/3754).

## Feedback

[Report an issue](https://github.com/itolkien/morphe-universal-captions/issues) with YouTube, Android, Manager and patch versions, the video, source/target language, and reproduction steps. Remove personal information and signed caption URLs before publishing logs.

## License

[GPL-3.0-or-later](LICENSE). Community project. This repository does not distribute modified YouTube APKs. Test-only upstream fixtures retain their own license and NOTICE files.
