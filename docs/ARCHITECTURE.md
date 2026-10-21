# Architecture: upstream-first subtitle development

Universal Captions is a **subtitle-focused regression and research repository**, not a Morphe fork. After [PR #3891](https://github.com/MorpheApp/morphe-patches/pull/3891) was merged, the official [Morphe `dev`](https://github.com/MorpheApp/morphe-patches/tree/dev) branch became the source of truth for shipping YouTube caption implementations.

| Responsibility | Source of truth |
| --- | --- |
| Runtime caption Java/Kotlin, fingerprints, Preferred Caption Language integration | Official Morphe `dev` |
| Supplemental caption regression tests | [`tests/upstream/`](../tests/upstream/) in this repository |
| Experimental 3.1.0 prototype, Kotlin hooks and decoder scopes | [`experiments/3.1.0/`](../experiments/3.1.0/README.md) |
| Historical standalone patch and complete sources | [`v3.0.1` tag](https://github.com/itolkien/universal-captions/tree/v3.0.1) |

The [current workflow](../.github/workflows/ci.yml) checks out the official Morphe repository transiently. It adds **only a new Java test file in the upstream test directory**, leaving all tracked Morphe files and production sources unchanged, then runs upstream Kotlin and Java tests. Ordinary pushes build no MPP. A manual workflow dispatch can produce an official Morphe MPP solely for Android testing.

## Experimental 3.1.0 research

The unmerged prototype is now easy to browse under `experiments/`, rather than active top-level `extensions/` and `patches/` source paths: [`experiments/3.1.0/`](../experiments/3.1.0/README.md). Its guarded active-audio bridge, per-decoder `beginCaption/endCaption` scopes and larger regression suite remain inspectable. The merged Morphe code intentionally uses simpler fingerprint-based hooks and a thread-local boolean for translated caption pens, while incorporating source identity and signed URL safety improvements.

The prototype is neither built by the main CI nor distributed as an independent patch source. Further improvements should have a reproducible upstream regression and use the smallest viable Morphe PR, with special care for older YouTube DEX layouts.

The [v3.0.1 architecture](https://github.com/itolkien/universal-captions/blob/v3.0.1/docs/ARCHITECTURE.md) and the old standalone implementation remain available in the release tag and repository history.
