# Experimental Universal Captions 3.1.0 — historical archive

These are the earlier Universal Captions Morphe-dependent Java/Kotlin sources, their supplementary tests, and the historical 3.1.0 MPP verification script. They were explored on `refactor/morphe-native-source` and are retained for code review, diffing and possible targeted upstream contributions.

**Not current production code. Not a standalone patch source. Not loaded by current CI.** The integrated code lives in official [Morphe `dev`](https://github.com/MorpheApp/morphe-patches/tree/dev), following [PR #3891](https://github.com/MorpheApp/morphe-patches/pull/3891).

The prototype used a guarded native active-audio source bridge, a larger set of signed URL and source validation tests, and `beginCaption/endCaption` nested decoder scopes. These extra hooks were not accepted as a replacement for Morphe's smaller upstream runtime without older YouTube APK compatibility evidence.

Files are stored with their original relative source paths under this archive for easy comparison; they are intentionally not compiled or installed by the upstream validation workflow.

Historical standalone **v3.0.1** remains fully preserved at [its tag](https://github.com/itolkien/universal-captions/tree/v3.0.1).
