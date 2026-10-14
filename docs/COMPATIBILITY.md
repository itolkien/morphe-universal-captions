# Compatibility and validation

## Release evidence

| YouTube version | Evidence | Support status |
| --- | --- | --- |
| 21.20.405 | Previously device-tested baseline; legacy selector regression retained | Supported |
| 21.41.525 | Exact-APK selector regression and standalone CLI patch/dex rebuild passed; user confirmed 1.3.2-dev and the final 1.3.3-dev alphabetical menu working on Waydroid / Android 13 | Supported |
| Other releases | No APK/device evidence | Not advertised |

Release 1.4.0 declares these two versions. The user approved release after confirming the final development bundle works. This is a user-confirmed device result, not an exhaustive certification of every video, account, language or patch combination. Structural discovery remains strict on both versions; unresolved ambiguity stops patching.

The release asset, generated `patches-list.json` and Manager `patches-bundle.json` use the same version. CI only uploads build artifacts; publication is performed deliberately after validation.

## Structural resolution

The explicit selection dispatcher still requires the existing constructor/signature evidence. Within that dispatcher owner, a default-track candidate must be an implemented instance method with no arguments, the discovered caption return type, a spare local register, an object return, and exactly one distinct read of the discovered caption model from `this`. A plain current-track getter, static helper, field write, or read on another receiver is not sufficient. Repeated reads of the same model field count once. Zero qualifying methods fail with descriptors and candidate evidence. Multiple model readers require the additional relationship evidence below; unresolved ambiguity still fails.

The actual 21.41.525 APK contains a default-calculation helper and a committing wrapper that both read the model. To distinguish them, require the helper to call Android's `CaptioningManager.isEnabled()` and `getLocale()`, every other candidate to invoke that helper on `this` (without reciprocal calls), and a separate model-initialization method to write the same model field before invoking the helper on `this`. These are method/field/call relationships; obfuscated names, access visibility and register counts do not choose a winner. If any required evidence is absent, ambiguity remains an error.

For the reported APK only, these relationships identify `Lasku;->s()Lasmo;`; `a()` delegates to it and `j(...)` calls it during model initialization. Those names occur in diagnostic evidence and the opt-in exact-APK test, never as production fingerprints. APK SHA-256: `e470d11a48be533f607677edeaebdd937982a16e12e6d7ef4e30d7f0fafab2f2` (version code 1561303420, minAPI29, universal nodpi variant). No APK or extracted proprietary implementation is committed.

The native source-list getter is tied to the discovered direct-list string anchor and model class, rather than selected as the only list getter other than the translation getter. A changed owner or inaccessible getter fails safely. Other fingerprints remain strict; model/builder/URL layout changes can still require updates.

## Automated checks

```sh
scripts/test-core.sh
gradle -p tests/fingerprints test --no-daemon
```

Use JDK 17 and Gradle 9.0.0 for the regression suite. The fingerprint tests use actual dexlib immutable methods/instructions with synthetic class names. They cover old and extra-getter shapes, reordered candidates, static/write/wrong-receiver decoys, repeated field reads, ambiguity, missing evidence and insufficient registers. These synthetic tests are not APK tests. An additional opt-in test checks selector identity against the exact reported APK, with its SHA-256 pinned:

```sh
YOUTUBE_2141525_APK=/path/to/clean.apk gradle -p tests/fingerprints test --rerun-tasks --no-daemon
```

Without this environment variable, the exact-APK test is explicitly skipped. It runs locally; CI must not download or upload the APK. Selector identity, complete patch/dex rebuild, and device behavior are three separate checks. Core tests exercise the shipped translation-target policy, including Greek, native regional Greek, legacy aliases and distinct Chinese scripts. Menu-order tests reproduce the reported English screenshot order, Turkish I/İ collation, mixed native/generated labels, stable ties, unchanged input/track identity, and fixed positions for sentinel/unknown/unlabelled entries.

Pull requests also build the complete Kotlin patch and Android extension against the pinned Morphe template (JDK 21). CI uploads the versioned MPP and generated patch-list metadata; it does not publish a release or update the Manager feed. Template upgrades should be explicit commits and pass the same checks.

## Auto-translate menu ordering

The menu sorts all language rows in the augmented translation menu by their actual `CharSequence` display label, using the application's configured locale and `java.text.Collator` (case-insensitive, canonical decomposition). It sorts existing and generated rows together, preserving track objects and stable order for equal labels. Off, Auto-translate and other `_OPTION` sentinel rows, null/unknown rows and rows without labels keep their original slots. The source track is selected before sorting; the direct-track list and preference ranking are unchanged. Lists are sorted even when no new target can be added. If ordering fails, return the unsorted list.

Confirm alphabetical order under English and Turkish app interfaces, selecting a row after reordering, and switching videos while retaining the chosen language. Public CI tests the shipped ordering helper and compiles the bytecode display getter. The user confirmed the alphabetical menu on-device with 1.3.3-dev; release 1.4.0 keeps that runtime behavior.

## APK and device checks before release

For each version considered for support:

1. Use a clean, legally obtained APK. Record its version, variant, SHA-256, Morphe Manager version, development commit and bundle checksum locally. Do not commit the APK.
2. Apply Universal Captions with Morphe Manager. Save sanitized fingerprint diagnostics if resolution fails; inspect both candidate methods before changing an ambiguity rule.
3. Install the patched app and check videos with manual captions, ASR, only translated tracks, and no captions. Test Greek under both English and Greek app interfaces. Labels are localized, so look for **Greek / Ελληνικά**.
4. Verify that an absent Greek target appears once when a usable source exists, an existing native/direct Greek track is retained, selecting Greek yields native captions when YouTube serves them, and changing videos/restarting preserves the selection.
5. Verify explicit Off across video changes and opening the captions menu, explicit turn-on after Off, choosing another language, seeking, rapid video changes, and manual versus ASR preference. The committing wrapper can perform its own fallback after a null default, so its callers require this device check. Test both Chinese scripts and at least one legacy alias language to catch menu deduplication regressions.
6. Record patch success separately from runtime behavior. Use the recorded evidence and maintainer release approval to decide which versions to advertise, then publish a new versioned bundle/feed together. Do not overwrite an existing release asset with different code.

The target catalogue is curated, not an authoritative list of every YouTube interface or translation language. Interface availability does not prove timed-text backend support for a video/account. Greek (`el`) was already present in v1.3.0; the reported follow-up says it was found. No universal missing-Greek defect or server support is inferred from that exchange.
