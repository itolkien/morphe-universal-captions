# Comprehensive caption fallback

Authorized scope: extend draft PR #5 and deliver a verified experimental MPP.

Design: read target codes from YouTube's native caption metadata rather than a fixed allowlist. When metadata is absent, use a dated, provenance-documented 156-language player-response snapshot. Preserve distinct regional targets. Resolve the native default caption source for the current audio; if absent, use the first valid original source in YouTube's current-audio direct list. Never combine sources or reuse a URL across managers. Native translated lists and Morphe preference/visibility policies retain ownership.

Plan:
1. Add failing regression coverage for dynamic targets, regional targets, multiple sources/default source, no metadata, and malformed data.
2. Resolve metadata fields and native default accessor structurally before mutations; inject per-manager helpers.
3. Implement runtime catalog and source selection; exercise all 156 source/target pairs and representative display locales.
4. Test the real upstream preference policy using its source with isolated platform stubs.
5. Build and patch both supported APKs, verify native coexistence in both orders and package contents. Device/backend verification remains separate.
6. Update documentation, draft PR, and downloadable artifact with hashes/evidence.

Completed: all six implementation/validation steps. Runtime and real APK evidence are in ../EMPTY_TRANSLATION_EXPERIMENT.md. Native regional preference matching remains upstream-owned and documented; no device/backend success is claimed.
