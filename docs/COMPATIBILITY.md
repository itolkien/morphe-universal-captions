# Compatibility and validation

Development version: **2.0.0-dev.1**. Declared APK targets: **21.20.405**, **21.41.525**. No other version is verified.

## Automated checks

Run `scripts/test-core.sh` with Gradle 9.0.0 and JDK 17. Tests cover the shipped menu runtime, Greek coverage, aliases, localized sorting, preserved source identity and signed URL parameters, malformed prototypes, and field/getter builder layouts.

Set `YOUTUBE_CAPTIONS_APK` to a clean local APK to test production model discovery and complete ABI resolution. APKs are not included in the repository or CI.

For a standalone patched APK, set `CAPTIONS_BASELINE_APK` and `CAPTIONS_PATCHED_APK` to compare the native CaptioningManager-related policy classes byte for byte. This check should run against this addon alone, since Morphe itself legitimately modifies those classes.

Build through the pinned official template in `.github/workflows/build-mpp.yml`. Generate JVM metadata first, run the Android build last, then validate the outgoing MPP with `scripts/verify_android_bundle.py`. Later JVM tasks can overwrite the Android archive, so verify the exact outgoing file.

## Scope of evidence

The corresponding native Morphe integration passed device testing on **21.20.405** and APK/SDK validation on both targets. That evidence does not replace independent addon testing. The user confirmed successful device testing of **2.0.0-dev.1**. The APK version for that independent addon device test was not specified; this does not establish device coverage for both targets. The v1.4.0 release and stable Manager feed remain unchanged.

## Local development validation

- The menu runtime and resolver suite passed on both clean APK targets (15 tests plus the APK isolation check).
- Both standalone patched APKs passed SDK DEX verification, cross-DEX hierarchy checks, alignment, signing and APK verification.
- On 21.20.405, combined patching with Captions succeeded using both official 1.48.0-dev.7 and the native integration in PR #3754. The user subsequently confirmed successful independent addon device testing of 2.0.0-dev.1, without specifying the APK version.
