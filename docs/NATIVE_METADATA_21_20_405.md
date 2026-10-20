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

## Direct DEX evidence and active audio switching

The developer subsequently supplied the original `classes2.dex` and
`classes3.dex`. Offline opcode decoding independently confirms the
above Java-level relationships:

| Method and instruction offset (DEX code units) | Verified use |
| --- | --- |
| `Lapdk;->g()Ljava/util/List;` at `0x0005–0x002b` | Reads metadata source/audio/target lists and returns empty if general source index list `Lbfuf;->f` is empty |
| `Lapdk;->g()` at `0x003f–0x004b` | Target `Lbfug;->f` excluded-audio indices compared to current manager audio index `Lapdk;->c` |
| `Lapdk;->g()` at `0x0054–0x007e` | Tries `Lbfug;->e` target-specific source indices against current `Lbfud;->d` caption membership |
| `Lapdk;->g()` at `0x007f–0x00a9` | Falls back to general `Lbfuf;->f` indices, again intersected with the current audio caption indices |
| `Lapdk;->g()` at `0x00b1–0x00f6` | Reads `Lbfug;->d:Lbbbt;`, invokes `Lapkp;->b(Lbbbt;)Landroid/text/Spanned;`, converts to String, builds URL and assigns the label |
| `Lapdk;->h()` at `0x0029–0x0058` | Builds original captions from the current audio's caption index list |
| `Lapdk;->h()` at `0x0059–0x0065` | Checks the general source list `Lbfuf;->f` and auto-menu suppression flag before adding Auto-translate |
| `Lapbq;->j(Lahzi;Lbfuf;)V` at `0x00a8–0x00ac` | Creates and stores a fresh caption manager when metadata is refreshed |
| `Laotl;->a(Ljava/lang/Object;)V` at `0x00a7–0x00d6` | On an audio-change event, gets the selected ID via `Lahwt;->v()`, matches it to `Lbfud;->c`, then updates **both** `Lapdk;->b` (audio entry) and `Lapdk;->c` (audio index) |

That final point resolves a major earlier uncertainty: **the native manager
does update its selected audio entry and index on a later audio change**.
It is not only a constructor-time snapshot. The same event handler then
tries to remap the previous caption selection using
`Lapdk;->c(Ljava/lang/String;)Lapdi;`; this is further evidence that
language alone is not the identity of the selected caption.

`Lapdk;->b()Lapdi;` still returns the **native default** caption
for the current audio, not necessarily the user's manually selected
track. The `Lapbq;->o:Lapdi;` field tracks a separate selected caption
at the playback manager level; do not infer manual selection from the
default getter.

### Safe changes supported by this evidence

- The direct `apdk.h` list already contains only caption indices
  associated with the **current** audio entry. Do not duplicate that
  filtering in Java.
- When the direct list contains several original tracks, **do not infer
  an eligible translation source merely because they have the same
  language**. They may have distinct caption indices and VSS IDs.
  Reuse the exact current native default when present; otherwise fail
  closed if distinct source identities coexist.
- `bfuf.f` cannot be casually ignored: it is a native menu and translated
  list **gate**, not just a convenience source candidate.
- Target-specific metadata `bfug.e/f` is the higher-value long-term
  bridge, but hooking it must retain the active-audio intersection,
  exclusion semantics and bounds checks.

### Structural hook plan for another YouTube version

Identify semantic references, never embed `apdk`, `bfuf`, `bfug`,
`bfud`, `aotl` or `apkp` names in the shipped patch. Starting from
the already resolved `&tlang=` translated-list method:

1. Identify the native target-language iteration and its code accessor
   through `CaptionMetadataAbi`.
2. Verify a target-owned integer list flowing to `List.contains` with
   the manager's boxed audio index (excluded audios).
3. Verify the other target-owned index list flowing to an iterator,
   and the same audio-entry caption-list field flowing to membership
   checks for those indices.
4. Identify the metadata-owned source index list used for **both** the
   initial nonempty guard and fallback iteration, and reject ambiguous
   matches.
5. Follow the target label's rich-text field to a converter returning
   `Spanned`, then `toString`, then the actual builder label assignment.
6. Check that the active audio manager's `b/c` fields are updated
   together on a separate audio-change path. Absent proof on a second
   version, do not ship an inferred new getter.

A register-level resolver should be exercised with synthetic DEX
fixtures, then against a second original YouTube version during the final
multi-version static verification. Do **not** force native eligibility
lists to appear nonempty: that could override deliberate backend
restrictions.

## Remaining evidence for multi-version native hooks

The 21.20.405 original DEX is now decoded and the above register-level
relationships are established. Next, build a conservative structural
resolver and compare its **semantic** matches with a second YouTube APK,
without depending on obfuscated class names.

Build synthetic regression fixtures for candidate structural
fingerprints. Inject only when all relationships are unique.
The later second-version comparison must show the same **semantics**
even if all obfuscated identifiers differ.

No APK patch, device installation, or runtime playback tests have
been performed. Those remain deferred by the owner.


## Native audio-eligible source bridge (3.0.0-dev.5)

The optional default getter `apdk.b()` returns null whenever native
`bfud.f` says there is no default caption, including videos that have
many valid original and translated captions. This was observed directly
in the Waydroid `UC3Probe` logs (`hasDefault=false` and
`mixed-sources` with 17–18 native targets).

The new bridge follows the native `apdk.g()` **general source fallback
branch**, preserving its order: iterate the general source indices
(`bfuf.f`) and find the first index belonging to the selected audio
entry's caption index list (`bfud.d`). Check nonnegative and
`< native source count`, obtain the corresponding original `bfue`
and convert via the same private `apdk.i(bfue)` used by the native
builder. It does not use the optional default flag.

The generated bytecode uses **only structurally resolved references**:
metadata source list (flow to the original-track converter), metadata's
general indices (repeated-int type matching audio-caption membership),
current audio entry field, current audio caption-index list, and the
conversion method called by both native direct/translated getters.
Ambiguous or missing matches fail the APK patch, rather than silently
shipping a nonfunctional new getter. Kotlin fixture tests cover valid,
ambiguous and missing patterns.

For mixed-source native translations, the Java layer retains all native
translated objects and uses this validated original for only absent
target codes. It still declines cross-video or unknown source identities
and records only reason/count flags in capped `UC3Probe` diagnostics.
This is a preview bridge for YouTube 21.20.405; cross-version stability
and actual subtitle text are not yet proven.
