# Architecture

Universal Captions adds missing translation choices to YouTube's native caption
model. It does not select the user's language, turn captions on or fetch text
from another translation service. The project was developed independently
by itolkien; Morphe's tooling is used to package and distribute the patch.

## Patch-time resolution

`CaptionFingerprints.kt` locates the native caption track and menu getters.
`CaptionTranslationAbi.kt`, `CaptionMetadataAbi.kt` and
`CaptionFallbackAbi.kt` resolve the builder, original sources, target list
and native Auto-translate row. These checks run before native bytecode is
modified. Ambiguous layouts fail patching rather than guessing names.

`UniversalCaptionsProbePatch.kt` is the production patch despite its old
filename. It adds `universalCaptions_` accessors and hooks the direct and
translated menu getters. The unused boolean on saved original getters is
intentional: it keeps them out of upstream parameterless fingerprints.
There is no diagnostic probe logger in the shipped extension.

## Menu runtime

- `TimedTextUrl` validates signed YouTube timedtext URLs, preserving all
  unrelated query bytes.
- `OriginalCaptionSource` holds one validated caption source and video.
- `CaptionTranslationList` preserves native translated entries and fills
  missing targets. Its fallback catalog is a dated YouTube player snapshot,
  not proof that the server will serve each translation.
- `EmptyTranslationFallback` restores YouTube's native Auto-translate row
  where a safe original exists. It intersects the native current-audio caption
  indices with eligible source indices and rejects ambiguous fallback choices.
- `NativeTranslationBridge` supplies the current app locale and menu hooks.

No source URL is shared across caption managers or videos. The extension
does not persist or change caption preferences, visibility or the selected
subtitle track.

## Caption styling

Some translated srv3 documents carry creator-defined pens that make text
invisible. `CaptionPenHooks.kt` finds synchronous parser calls in the network
and file-cache paths. `CaptionPenStyles` tracks each parser in a nested
thread-local scope; `endCaption(request)` only closes that request's scope.
The original subtitle keeps its creator styling, while translated subtitles
use the default pen for readability.

See [caption pen handling](CAPTION_PEN_STYLES.md). This is not a claim of
pixel-perfect Web-style reproduction.

## Checks and history

`scripts/test-core.sh` runs the Kotlin ABI and Java caption regressions.
The MPP workflow compiles against a pinned Morphe template, then checks
the Android DEX, embedded extension, NOTICE and release metadata.
Some structural tests require local YouTube APKs and are skipped on CI,
which does not include copyrighted app binaries.

Earlier 2.x behavior is in the v2.1.0 tag and
[historical fallback notes](EMPTY_TRANSLATION_EXPERIMENT.md).
