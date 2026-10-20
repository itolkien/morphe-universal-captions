# Universal Captions 3.0.1

A maintenance update to 3.0.0. The translation menu and source-selection
behavior are unchanged.

- Caption parser cleanup is now request-specific. A native exception
  handler no longer closes a different decoder's styling scope.
- Removed two test-only `augment()` overloads from the Android extension.
- Added tests for nested exception cleanup and for the recorded
  156-language fallback catalog.
- Corrected the Morphe Manager bundle URL. The release workflow now reads
  the version from `patches-list.json` and skips versions already published.
- Updated architecture and compatibility documentation.

## Install

Download `universal-captions-3.0.1.mpp` or refresh the custom source in
Morphe Manager, then patch a clean YouTube **21.20.405** or **21.41.525** APK.
The standalone addon must not be combined with the older Morphe auto-translate
or translated-caption pen hooks.

The 3.0 development builds were tested manually on both declared YouTube
versions. This update has automated regression coverage; a separate
itemized on-device test of the new parser cleanup has not been recorded.
YouTube can still withhold translations for individual tracks/languages.

The release distributes the MPP, patch metadata and its SHA-256 checksum,
not a modified YouTube APK.
