# Compatibility — Morphe-native prototype

The `experiment/morphe-native-captions` branch is **not released**.
The packaged Morphe fork currently advertises YouTube 21.20.405 and 21.41.525
among its targets; APK patching is not proved by the metadata alone.

Use a clean APK with a **single** Morphe integration source, never the old
Universal Captions 3.0.1 bundle and another Morphe source at the same time.

The old standalone release remains at [v3.0.1](https://github.com/itolkien/universal-captions/releases/tag/v3.0.1).
Its historical source/pen tests are not tests of the new Morphe-owned implementations.
The experiment runs Morphe's own tests and must also pass real APK and device
smoke tests before release.
