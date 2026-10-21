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

The adapter reads YouTube's video-specific language catalog and validated
current-audio source, rather than copying Morphe's first-track fallback.
Expanded tracks remain separate from existing native tracks. Native source
and catalog access occurs inside a guarded Java boundary instead of eagerly
in injected DEX. Translated caption pens use request-scoped state on each
decoder thread, with a lightweight linked stack and exception cleanup.

The candidate is still experimental. Successful JVM/MPP builds do not prove
DEX compatibility or rendering behavior on either supported APK.

The stable pre-migration architecture and studies are recorded in
[the v3.0.1 documentation](https://github.com/itolkien/universal-captions/blob/v3.0.1/docs/ARCHITECTURE.md).
