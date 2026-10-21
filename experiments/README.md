# Experiments

This directory contains Universal Captions **Java/Kotlin research implementations** and supporting tests. It is not a fork or mirror of Morphe's complete repository.

| Experiment | Focus | Status |
| --- | --- | --- |
| [Universal Captions 3.1.0](3.1.0/README.md) | Active-audio source validation, 156 translation languages, signed URL handling and decoder-scoped styling | Experimental; not used by main CI or released separately |

Our code is available for comparing approaches and developing focused improvements. The actual integrated runtime lives in [Morphe dev](https://github.com/MorpheApp/morphe-patches/tree/dev), following [PR #3891](https://github.com/MorpheApp/morphe-patches/pull/3891).

For ongoing compatibility monitoring, see our [additional upstream tests](../tests/upstream/MorpheCaptionUpstreamRegressionTest.java). For the old standalone release and all its source, see [v3.0.1](https://github.com/itolkien/universal-captions/tree/v3.0.1).

**These experiments are not a patch download or an additional Morphe Manager source.**
