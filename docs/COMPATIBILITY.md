# Compatibility — Morphe-ready source experiment

The experimental code is not a published new release. This branch builds a
*complete Morphe replacement source*, not a standalone independently loadable
patch. At present, the pinned Morphe fork advertises YouTube 21.20.405 and
21.41.525, but actual APK compatibility requires clean-device smoke tests.

The original installable standalone Universal Captions 3.0.1 is preserved on
`main` and at the [v3.0.1 release](https://github.com/itolkien/universal-captions/releases/tag/v3.0.1).

The new branch adds a regression for the stale translated-caption pen state when
a native getter fails. Other behavior differs from standalone 3.0.1 and needs
separate real-device comparisons. CI results check compilation, not Android UX.
