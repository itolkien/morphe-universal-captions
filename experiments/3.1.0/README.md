# Universal Captions 3.1.0 — experimental code

Our earlier Morphe-dependent 3.1.0 prototype is kept here for **active research, source comparison and future targeted contributions**. Files retain their original package-relative paths under `experiments/3.1.0/` so the implementation and tests can be inspected together.

**Not currently shipped or tested by the main CI.** The code merged in [Morphe PR #3891](https://github.com/MorpheApp/morphe-patches/pull/3891) is maintained by [official Morphe dev](https://github.com/MorpheApp/morphe-patches/tree/dev). Do not install these files as a separate Morphe patch source.

## Java implementation

| Component | Source |
| --- | --- |
| Source selection, native tracks, language targets, signed URLs | [AutoTranslateCaptionsPatch.java](extensions/youtube/src/main/java/app/morphe/extension/youtube/patches/AutoTranslateCaptionsPatch.java) |
| Styling and nested decoder lifecycle (`beginCaption/endCaption`) | [TranslatedCaptionPensPatch.java](extensions/youtube/src/main/java/app/morphe/extension/youtube/patches/TranslatedCaptionPensPatch.java) |

## Kotlin bytecode integration

| Component | Source |
| --- | --- |
| Native translation-manager hooks | [AutoTranslateCaptionsPatch.kt](patches/src/main/kotlin/app/morphe/patches/youtube/layout/captions/AutoTranslateCaptionsPatch.kt) |
| Network/cache decoder hooks and exception cleanup | [TranslatedCaptionPensPatch.kt](patches/src/main/kotlin/app/morphe/patches/youtube/layout/captions/TranslatedCaptionPensPatch.kt) |

## Experimental tests

| Scope | Source |
| --- | --- |
| Language target and source/URL safety | [AutoTranslateCaptionsPatchTest.java](extensions/youtube/src/test/java/app/morphe/extension/youtube/patches/AutoTranslateCaptionsPatchTest.java) |
| Caption style scope, concurrency and errors | [TranslatedCaptionPensPatchTest.java](extensions/youtube/src/test/java/app/morphe/extension/youtube/patches/TranslatedCaptionPensPatchTest.java) |
| Preferred language routing | [PreferredCaptionRoutingTest.java](extensions/youtube/src/test/java/app/morphe/extension/youtube/patches/PreferredCaptionRoutingTest.java) |

The historical [bundle verification script](scripts/verify_morphe_bundle.py) is retained but not invoked by the current CI.

## Relationship to Morphe

The prototype implements a guarded native source bridge and explicit decoder scopes that are **not identical to Morphe's smaller merged implementation**. The critical source-validation and signed URL safeguards were integrated upstream; some experimental decoder hooks were intentionally not merged due to uncertain compatibility with older supported YouTube versions.

Use the [current upstream regression tests](../../tests/upstream/MorpheCaptionUpstreamRegressionTest.java) to verify Morphe, then introduce any proven improvements through focused upstream PRs. Neither extra code nor extra tests alone prove real APK compatibility.

The original standalone 3.0.1 patch and sources remain at the immutable [v3.0.1 tag](https://github.com/itolkien/universal-captions/tree/v3.0.1).
