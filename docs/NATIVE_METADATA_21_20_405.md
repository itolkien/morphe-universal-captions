# YouTube 21.20.405 — native caption metadata (verified source mapping)

This mapping was derived from the developer-supplied JADX output of the
original, unpatched APK: `apdk.java` and `apdi.java` in `classes2.dex`,
and `bfuf.java`, `bfud.java`, `bfue.java`, `bfug.java` in
`classes3.dex`, plus caption builder `apdh.java`. These are **static
decompilation findings only**. Names are obfuscated and version-specific.
The raw decompiled YouTube source is not committed or redistributed.

## Definitive fields and how they are used

| Native object / field | Type in 21.20.405 | Verified role |
| --- | --- | --- |
| `apdk.a` | `bfuf` | Caption metadata object |
| `bfuf.b` | protobuf list of `bfue` | Available original caption tracks |
| `bfuf.c` | protobuf list of `bfud` | Available audio-track objects |
| `bfuf.e` | `int` | Index of audio entry chosen when `apdk` is constructed |
| `apdk.b`, `apdk.c` | `bfud`, `int` | Audio entry and audio index stored on the manager |
| `bfud.d` | protobuf repeated int | Caption indices associated with that audio entry |
| `bfud.e`, `bfud.f` | `int`, `boolean` | Default caption index plus its presence/validity flag |
| `bfuf.d` | protobuf list of `bfug` | Candidate target languages |
| `bfug.c` | `String` | Target language code |
| `bfug.d` | `bbbt` rich text | Native localized target display name, via `apkp.b` |
| `bfug.e` | protobuf repeated int | Target-preferred eligible source-caption indices |
| `bfug.f` | protobuf repeated int | Audio indices excluded for this target |
| `bfuf.f` | protobuf repeated int | General/fallback source-caption indices and translation-menu gate |
| `bfue.f` | `String` | Original caption source language |
| `bfue.e`, `bfue.c` | `String`, `String` | Original VSS ID and timedtext URL |
| `bfue.d` | `bbbt` rich text | Original caption display name |
| `apdi.a`, `apdi.k`, `apdi.m`, `apdi.p` | language, VSS, URL, label | Built caption track |

Protobuf field descriptors in the `dynamicMethod` implementations
confirm that `bfuf.b/c/d` are repeated message objects and `bfug.e/f`
are repeated integers. The `bfug.d` field is **not a String**.

## The exact native decision sequence (`apdk.g`, lines 131–189)

1. If original captions, audio entries, target entries, selected audio
   entry, **or the general source list `bfuf.f`** is empty, return an
   empty translated-caption list.
2. For each target `bfug`, skip it if its excluded audio list `f`
   contains the manager's audio index `apdk.c`.
3. Choose the first target-preferred caption index from `bfug.e` that
   also occurs in `apdk.b.d` (caption indices for the current audio).
4. If no match, choose the first general source index from `bfuf.f`
   also contained in `apdk.b.d`.
5. If either yields a source, create a native translated `apdi` using
   the source video/VSS/timedtext URL, the target language code
   `bfug.c`, and display name obtained through:
   `bfug.d (bbbt) → apkp.b(...) (Spanned) → toString() → apdh.c`.
6. Otherwise omit the target, even though its language code is present
   in the metadata.

The original caption menu `apdk.h` (191–221) similarly uses the
audio entry's caption-index list. It only adds its existing native
Auto-translate option when `bfuf.f` is nonempty and the manager's
boolean suppression flag is false. Thus **language codes alone cannot
replicate the native menu**.

`apdk.b()` chooses the audio entry's default caption using the
`bfud.e` index only when `bfud.f` is true. That is an initial/default
selection, not proof of the caption the viewer selected manually later.

## Consequences for the refactor

- The native model provides **target-specific audio-aware source
  eligibility**, richer than a guessed single `isTranslatable` boolean.
  A standalone Web Boolean of that name is not demonstrated in this
  Android data model.
- Native localized target names exist, but the correct path is a
  **rich-text conversion**, not a direct `String` protobuf field.
  The unused speculative `targetLabel` resolver was removed.
- Relying solely on `bfuf.f` would lose target-specific preferred
  sources. Ignoring it would contradict the native eligibility gate.
  We must investigate why/how that list becomes empty on the videos
  Universal Captions currently repairs.
- The manager stores an audio entry/index at construction. Whether
  a viewer's later audio change replaces or mutates that manager
  requires inspection of the **callers/assignment sites**.
- It is too early to remove the 156-code fallback: the native method
  explicitly returns no translations when `bfuf.f` is empty, and
  server behavior may still differ from the native list's availability.

## Next evidence: actual DEX instruction flow, not more guessed fields

The current Java JADX output is enough to map the semantics but not
enough to safely inject version-tolerant getter hooks. Next, inspect
`classes2.dex` and `classes3.dex` to identify register-level
relationships for the audio index, caption-index lists, target rich
text converter and current-track source selection.

Build synthetic regression fixtures for candidate structural
fingerprints. Inject only when all relationships are unique.
The later second-version comparison must show the same **semantics**
even if all obfuscated identifiers differ.

No APK patch, device installation, or runtime playback tests have
been performed. Those remain deferred by the owner.
