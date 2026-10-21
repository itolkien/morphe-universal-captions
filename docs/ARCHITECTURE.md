# Architecture: Universal Captions after Morphe integration

## Responsibility split

Universal Captions is **not a Morphe fork**. Since
[PR #3891](https://github.com/MorpheApp/morphe-patches/pull/3891),
Morphe maintains the integrated YouTube captions implementation.

| Responsibility | Source of truth |
| --- | --- |
| Shipped caption Java/Kotlin patches, native fingerprints and preferences | Official Morphe `dev` |
| Supplemental subtitle-focused regression tests | `tests/upstream/` in this repository |
| Historical stable standalone v3.0.1 | Unchanged `main` and immutable `v3.0.1` tag |
| Experimental translation/decoder variants | Retained sources in this branch |

[Current CI](../.github/workflows/ci.yml) downloads Morphe's official
`dev` tree and copies in **only a supplementary Java test file**.
The regular job runs tests without replacing a single tracked Morphe
production source or building an MPP. Optional manual MPP generation
uses the same official Morphe checkout, not our experimental classes.
See [Upstream validation](UPSTREAM_VALIDATION.md).

## Retained experimental 3.1.0 implementation

These source files remain available for research but are no longer
automatically installed or built by this repository's CI:

| Research area | Retained file |
| --- | --- |
| Translation language expansion and verified source selection | `extensions/youtube/src/main/java/app/morphe/extension/youtube/patches/AutoTranslateCaptionsPatch.java` |
| Linked request-scoped caption pen lifecycle | `extensions/youtube/src/main/java/app/morphe/extension/youtube/patches/TranslatedCaptionPensPatch.java` |
| Native translation adapters | `patches/src/main/kotlin/app/morphe/patches/youtube/layout/captions/AutoTranslateCaptionsPatch.kt` |
| Network/cache decoder hooks | `patches/src/main/kotlin/app/morphe/patches/youtube/layout/captions/TranslatedCaptionPensPatch.kt` |

The experimental approach uses a guarded active-audio manager bridge
and `beginCaption/endCaption` decoder scopes. The merged Morphe code
uses simpler fingerprint-based hooks and per-thread caption styling
state, while incorporating core source identity and URL safeguards.

The more intrusive experimental decoder handler hooks were not merged
because older YouTube DEX compatibility needs separate evidence.
Both architectures are retained for comparison; only actual
reproducible bugs should justify upstream changes.

The larger experimental test files under
`extensions/youtube/src/test` likewise remain reference material.
Upstream-compatible additional tests are in [`tests/upstream/`](../tests/upstream/).

The [v3.0.1 architecture](https://github.com/itolkien/universal-captions/blob/v3.0.1/docs/ARCHITECTURE.md)
documents the earlier standalone code.
