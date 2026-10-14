# Compatibility and validation

## Evidence, not an all-version promise

| YouTube version | Evidence | Development target |
| --- | --- | --- |
| 21.20.405 | Previously device-tested with v1.3.0; this development change needs APK/device revalidation | Recommended baseline |
| 21.41.525 | User reported two track-returning methods in the default selector owner; exact APK bytecode not available in the regression fixtures | Experimental, not confirmed working |
| Other releases | No APK/device evidence | Not advertised |

The development bundle declares 21.41.525 as experimental so it can be attempted without advertising blanket compatibility. Structural discovery runs for both declared versions. It never selects the first of multiple matching methods, never uses obfuscated names as cross-version identities, and never silently disables persistence if a required hook cannot be resolved.

The published v1.3.0 `.mpp`, `patches-list.json`, and `patches-bundle.json` still describe the published release. They are not rewritten to advertise an unpublished development build.

## Structural resolution

The explicit selection dispatcher still requires the existing constructor/signature evidence. Within that dispatcher owner, a default-track candidate must be an implemented instance method with no arguments, the discovered caption return type, a spare local register, an object return, and exactly one distinct read of the discovered caption model from `this`. A plain current-track getter, static helper, field write, or read on another receiver is not sufficient. Repeated reads of the same model field count once. Zero or multiple qualifying methods fail with descriptors and candidate evidence.

This addresses the reported assumption that every track-returning method is the default selector. It does not establish what either method in 21.41.525 actually does. If both read the model, patching deliberately remains unsupported until the real call paths are inspected.

The native source-list getter is tied to the discovered direct-list string anchor and model class, rather than selected as the only list getter other than the translation getter. A changed owner or inaccessible getter fails safely. Other fingerprints remain strict; model/builder/URL layout changes can still require updates.

## Automated checks

```sh
scripts/test-core.sh
gradle -p tests/fingerprints test --no-daemon
```

Use JDK 17 and Gradle 9.0.0 for the regression suite. The fingerprint tests use actual dexlib immutable methods/instructions with synthetic class names. They cover old and extra-getter shapes, reordered candidates, static/write/wrong-receiver decoys, repeated field reads, ambiguity, missing evidence and insufficient registers. They are not APK tests. Core tests exercise the shipped translation-target policy, including Greek, native regional Greek, legacy aliases and distinct Chinese scripts.

Pull requests also build the complete Kotlin patch and Android extension against the pinned Morphe template (JDK 21). CI uploads a `1.3.1-dev` bundle; it does not publish a release or update the Manager feed. Template upgrades should be explicit commits and pass the same checks.

## APK and device checks before release

For each version considered for support:

1. Use a clean, legally obtained APK. Record its version, variant, SHA-256, Morphe Manager version, development commit and bundle checksum locally. Do not commit the APK.
2. Apply Universal Captions with Morphe Manager. Save sanitized fingerprint diagnostics if resolution fails; inspect both candidate methods before changing an ambiguity rule.
3. Install the patched app and check videos with manual captions, ASR, only translated tracks, and no captions. Test Greek under both English and Greek app interfaces. Labels are localized, so look for **Greek / Ελληνικά**.
4. Verify that an absent Greek target appears once when a usable source exists, an existing native/direct Greek track is retained, selecting Greek yields native captions when YouTube serves them, and changing videos/restarting preserves the selection.
5. Verify explicit Off, choosing another language, seeking, rapid video changes, and manual versus ASR preference. Test both Chinese scripts and at least one legacy alias language to catch menu deduplication regressions.
6. Record patch success separately from runtime behavior. Only then promote a version from experimental and publish a new versioned bundle/feed together. Do not overwrite an existing release asset with different code.

The target catalogue is curated, not an authoritative list of every YouTube interface or translation language. Interface availability does not prove timed-text backend support for a video/account. Greek (`el`) was already present in v1.3.0; the reported follow-up says it was found. No universal missing-Greek defect or server support is inferred from that exchange.
