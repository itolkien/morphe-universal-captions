# Architecture: project-owned sources with Morphe adapters

**Canonical development source:** the Java runtime and Kotlin caption hooks
under `extensions/youtube/src/main` and `patches/src/main` in *this repository*.

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

A known trade-off: these Morphe-native Java sources are **not fully equivalent**
to the standalone v3.0.1 implementation. In particular, current Morphe pen
detection is thread-local rather than stack-scoped, and native target expansion
assumes a common source for the list. Do not describe this branch as a completed
behavior-preserving refactor without clean-APK parity checks.

The stable pre-migration architecture and studies are recorded in
[the v3.0.1 documentation](https://github.com/itolkien/universal-captions/blob/v3.0.1/docs/ARCHITECTURE.md).
