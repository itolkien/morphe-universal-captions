# Architecture: Morphe-dependent caption implementation

The experimental branch tracks caption runtime and hook sources under
`extensions/youtube/src/main` and `patches/src/main`.

**Platform support:** a pinned Morphe patch fork supplies caption fingerprints,
preferred-language routing, extension injection, settings, dependencies and
native Android packaging. The adapter and other non-caption classes are
not reimplemented here.

| Component | Maintained in this repository |
| --- | --- |
| Translation list, source fallback, URL handling | `AutoTranslateCaptionsPatch.java` |
| Translated caption pen selection | `TranslatedCaptionPensPatch.java` |
| Native translation hooks | `AutoTranslateCaptionsPatch.kt` |
| Caption parser hooks | `TranslatedCaptionPensPatch.kt` |
| Behavior contracts | three Java test files copied into the platform build |

Unlike an alias-only experiment, these exact sources overwrite their Morphe
integration counterparts before Gradle compiles and tests the combined
implementation. The CI build is intentionally pinned to an audited fork commit;
changes to Morphe's caption infrastructure require an explicit update.

## Current 3.1.0 runtime

1. The pinned Morphe preferred-caption patch resolves the original translation source eligible for the **active audio**. The Kotlin adapter exposes `patch_getActiveTranslationSource()` under Java's error guard. It does **not** resolve Android's native target-language catalog.
2. `AutoTranslateCaptionsPatch.java` validates original language, video ID, VSS and signed YouTube timedtext URL. Existing native translations remain intact; missing choices are created using the fixed 156-code Web snapshot, excluding the original language and duplicates. If the original is ambiguous, only a consistent native translated list can supply a template.
3. The native Auto-translate menu count remains unchanged when nonzero. A missing row is restored only if a valid original source exists. Offered target codes do not guarantee a successful translation response.
4. `TranslatedCaptionPensPatch.java/.kt` apply a linked `ThreadLocal` scope per network/cache decoder invocation. Original styles remain untouched; translated captions use the default pen for readability.

Java guards native source access against runtime/linkage failures; Kotlin relies on pinned Morphe structural fingerprints, not hardcoded obfuscated class names. No Web player fetch or external translator is used.

**Preferred Caption Language:** Morphe's pinned preferred-language handler reuses tracks first, but can also call the shared `AutoTranslateCaptionsPatch.createTranslation()` for a missing preferred target. It is not purely selection-only. Manual selections and CC-off remain protected by the host policy.

The candidate is still experimental. Successful JVM/MPP builds do not prove
DEX compatibility or rendering behavior on either supported APK.

The stable pre-migration architecture and studies are recorded in
[the v3.0.1 documentation](https://github.com/itolkien/universal-captions/blob/v3.0.1/docs/ARCHITECTURE.md).
