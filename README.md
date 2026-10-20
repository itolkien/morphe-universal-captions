# Morphe Universal Captions

Restores missing YouTube **Auto-translate** menus and offers translation languages sorted by their displayed names.

[Add to Morphe Manager](https://morphe.software/add-source?github=itolkien%2Fmorphe-universal-captions&name=Universal%20Captions) · [Download v2.1.0](https://github.com/itolkien/morphe-universal-captions/releases/tag/v2.1.0) · [Report an issue](https://github.com/itolkien/morphe-universal-captions/issues)

## Features

- Uses YouTube's per-video target-language metadata; no video or source-language allowlist.
- **3.0 dev:** Preserves YouTube's native languages and signed subtitle objects, then adds remaining choices from a 156-language compatibility catalog when a compatible native translation target is established. Missing/empty catalogs still use the snapshot. The published 2.1.0 behavior is unchanged.
- Supports original automatic speech captions (ASR), uploaded/manual captions and named tracks.
- Restores the native Auto-translate option when original captions exist but the translation menu is missing.
- Preserves existing native translations and signed source URLs. With multiple originals, uses the current audio's native default; falls back to direct-list order only if the original language is unambiguous.
- Localized alphabetical language ordering, including distinct regional targets.
- No external translation service, overlay or separate caption preference UI.
- In **3.0.0-dev.2**: restores visible auto-translated text for channels with incompatible custom caption pens, while preserving the original track's creator styling ([Morphe #3837](https://github.com/MorpheApp/morphe-patches/pull/3837)).

Translation availability depends on the video, account and YouTube's backend. A language appearing in the menu does not guarantee translated text for every video. An initial 21.20.405 device test exposed an earlier 3.0 dev menu limited to ~17 native languages; the draft now has a regression-tested fix to broaden nonempty catalogs pending device confirmation. This is still a development build, not a claim of complete YouTube Web parity.

## Using it with Morphe

**Morphe owns preferred language, caption visibility, Off and explicit user selections.** This addon extends language menus and supplies the missing-menu fallback.

The released **2.1.0** addon works alongside Morphe's **Captions** patch, respects the native **Add additional auto-translate languages** setting in nonempty menus, and can restore an absent Auto-translate menu. The **unreleased 3.0 development branch** is a single-owner replacement: its legacy Morphe `CaptionIntegration` adapter has been removed, and it must **not** be used alongside the old Morphe auto-translate or translated-pen implementations. The existing preferred-language, explicit choice and CC visibility policy remains Morphe's responsibility.

### Caption styling

The **3.0.0-dev.2** development patch includes the translated-caption pen fix from [Morphe #3837](https://github.com/MorpheApp/morphe-patches/pull/3837) **inside Universal Captions itself**. The addon hooks YouTube's timedtext requests and the shared srv3 pen lookup, using the default pen for auto-translated tracks. Original tracks keep their creator-defined styles. The published **2.1.0** release does not contain this change.

This adapted fix retains Morphe's [GPLv3 additional terms](NOTICE). This is the source-of-truth implementation for the planned Morphe port. The upstream port should **replace**, not retain alongside, Morphe's existing #3837 hooks. Running this development patch alongside a Morphe build that already includes #3837 is not yet device-verified for duplicate hooks. The latest-request flag can also misattribute overlapping caption requests; per-document correlation remains a known limitation. See [caption style design and verification](docs/CAPTION_PEN_STYLES.md).

## Installation

1. Install [Morphe Manager](https://morphe.software/) and [add this repository as a source](https://morphe.software/add-source?github=itolkien%2Fmorphe-universal-captions&name=Universal%20Captions), or import the release `.mpp` locally.
2. Select a clean YouTube **21.20.405** or **21.41.525** APK.
3. For the released **2.1.0** addon, select **Universal Captions - additional translation languages** alongside Morphe **Captions**. Do not deploy the experimental 3.0 branch with an unchanged Morphe #3837 patch until the integration is deduplicated.
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

**Known limits:** Morphe's existing automatic regional matching can choose generic `pt` for preference `pt-PT`; manual menu targets remain separate. The fallback's source is the native default, or an unambiguous direct-list source; exact tracking of every manual source selection is not guaranteed. Server restrictions and absent original captions remain outside the addon's control.

For the ongoing comparison with the YouTube Web player and outstanding behavior gaps, see the [web parity audit](docs/WEB_PARITY_AUDIT.md) and [native metadata feasibility research](docs/METADATA_FIRST_RESEARCH.md). See [2.1.0 release evidence](docs/RELEASE_2.1.0.md), [fallback design and experiment history](docs/EMPTY_TRANSLATION_EXPERIMENT.md) and [historical compatibility evidence](docs/COMPATIBILITY.md). The native additional-language feature was integrated through [MorpheApp/morphe-patches#3754](https://github.com/MorpheApp/morphe-patches/pull/3754).

## Feedback

[Report an issue](https://github.com/itolkien/morphe-universal-captions/issues) with YouTube, Android, Manager and patch versions, the video, source/target language, and reproduction steps. Remove personal information and signed caption URLs before publishing logs.

## License

[GPL-3.0-or-later](LICENSE). Community project. This repository does not distribute modified YouTube APKs. Test-only upstream fixtures retain their own license and NOTICE files.
