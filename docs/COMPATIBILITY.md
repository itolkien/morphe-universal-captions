> Current stable validation: [2.1.0](RELEASE_2.1.0.md). On 2026-10-16, the user confirmed successful device testing across **20.21.37–21.42.241**, including intermediate versions. This is user-reported evidence, separate from local APK validation on 21.20.405 and 21.41.525. Earlier experimental and stable evidence below is historical.

# Compatibility and validation

Release version: **2.0.0**. Declared APK targets: **21.20.405**, **21.41.525**. No other version is verified.

## Automated checks

Run `scripts/test-core.sh` with Gradle 9.0.0 and JDK 17. Tests cover the shipped menu runtime, Greek coverage, aliases, localized sorting, preserved source identity and signed URL parameters, malformed prototypes, and field/getter builder layouts.

Set `YOUTUBE_CAPTIONS_APK` to a clean local APK to test production model discovery and complete ABI resolution. APKs are not included in the repository or CI.

For a standalone patched APK, set `CAPTIONS_BASELINE_APK` and `CAPTIONS_PATCHED_APK` to compare the native CaptioningManager-related policy classes byte for byte. This check should run against this addon alone, since Morphe itself legitimately modifies those classes.

Build through the pinned official template in `.github/workflows/build-mpp.yml`. Generate JVM metadata first, run the Android build last, then validate the outgoing MPP with `scripts/verify_android_bundle.py`. Later JVM tasks can overwrite the Android archive, so verify the exact outgoing file.

## Scope of evidence

The corresponding native Morphe integration passed device testing on **21.20.405** and APK/SDK validation on both targets. That evidence does not replace independent addon testing. The user confirmed successful device testing of **2.0.0-dev.1**. The APK version for that independent addon device test was not specified; this does not establish device coverage for both targets. The v1.4.0 release remains available in release history; the 2.0.0 Manager feed is published with its verified release asset.

## Local development validation

- The menu runtime and resolver suite passed on both clean APK targets (15 tests plus the APK isolation check).
- Both standalone patched APKs passed SDK DEX verification, cross-DEX hierarchy checks, alignment, signing and APK verification.
- On 21.20.405, combined patching with Captions succeeded using both official 1.48.0-dev.7 and the native integration in PR #3754. The user subsequently confirmed successful independent addon device testing of 2.0.0-dev.1, without specifying the APK version.

## Native coexistence update

The dev.2 update detects the native track interface introduced by the maintainer changes in PR #3754 and defers menu ownership to Morphe. Regression tests cover delegation, independent fallback when only extension classes are bundled, missing classes and empty lists. Device testing reported above covers dev.1; the user subsequently confirmed dev.2 working on-device. The upstream fix for 20.21.37 does not add that APK to this independent addon’s supported targets.

For the combined APK wiring regression, set `CAPTIONS_COMBINED_APK` to an APK patched with both this addon and the native integration. It verifies that the actual native caption class implements both interfaces and its translation-list method invokes both hooks. A test-only exact-name interface fixture also checks reflective discovery without shipping Morphe extension code in this addon.

Dev.2 local validation passed: all 20 tests with supplied APKs; standalone and combined-native patching, SDK DEX/hierarchy verification, alignment, signing and APK verification on both 21.20.405 and 21.41.525. Combined wiring was checked on both versions. Native off/on ownership is covered by JVM regression checks; the user subsequently confirmed dev.2 working on-device.

## Stable 2.0.0

The user confirmed device operation of dev.2 and authorized stable publication. The stable release uses the same runtime and patch implementation; only version/metadata and release documentation change. The APK version of that independent addon device confirmation was not specified, so device coverage of both declared targets is not claimed. Publication verifies the final Android archive and keeps generated compatibility metadata and the Manager download URL aligned with the actual release asset.
