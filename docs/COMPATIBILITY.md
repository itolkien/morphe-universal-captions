# Morphe and YouTube compatibility

[Universal Captions PR #3891](https://github.com/MorpheApp/morphe-patches/pull/3891)
is merged. The integrated captions implementation now lives in
[Morphe's official `dev` source](https://github.com/MorpheApp/morphe-patches/tree/dev).

Normal CI runs Morphe's original Java/Kotlin tests plus our **additional
caption-only tests**. Its optional, manually dispatched MPP is a
complete official **Morphe test bundle**, not an addon to stack with
another Morphe MPP.

Supported YouTube versions depend on the exact tested Morphe commit.
Consult that build's patch metadata; don't assume the version targets
from our earlier experimental 3.1.0 build remain fixed.

A successful JVM test or MPP build is not proof of compatibility with
an actual APK, the YouTube translation server or device playback.
For the minimum and maximum advertised versions, test clean-APK patching,
installation, native and missing translation menus, real translated
text, multiple audio tracks, manual and CC-off selections, cached
documents and original/translated subtitle styling.

[3.0.1](https://github.com/itolkien/universal-captions/releases/tag/v3.0.1)
remains the published standalone reference. No second standalone 3.1.0
release is needed now that the main changes are upstream.

See [Upstream validation](UPSTREAM_VALIDATION.md) and the
[historical experimental checklist](PARITY_VALIDATION.md).
