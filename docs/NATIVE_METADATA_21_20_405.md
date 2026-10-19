# YouTube 21.20.405 native caption metadata: first static findings

Source: developer-supplied JADX extracts from the original, unpatched
YouTube 21.20.405 APK (classes2.dex), `defpackage/apdi.java` and
`defpackage/apdk.java`. This is **static code evidence**, not an
APK patch result, device test or proof that the same layout holds across
versions. Raw YouTube sources are not stored in this repository.

## Verified relationships

| Native member | Observed use | JADX source lines |
| --- | --- | --- |
| `apdk.a : bfuf` | Parent captions metadata | apdk:14, 24 |
| `bfuf.b : List<bfue>` | Caption source tracks | apdk:85, 98, 150, 163, 201 |
| `bfuf.c : List<bfud>` | Audio-track entries | apdk:30-34 |
| `bfuf.e : int` | Audio-track index when manager is created | apdk:28-34 |
| `apdk.b : bfud` | Active/default audio entry held by manager | apdk:33, 77-85, 135-166 |
| `bfud.d : List<int>` | Caption indices allowed for chosen audio | apdk:93-102, 145-163, 198-203 |
| `bfud.e : int`, `bfud.f : boolean` | Default caption index and availability flag | apdk:75-85 |
| `bfuf.d : List<bfug>` | Target translation entries | apdk:135-183 |
| `bfug.c`, `bfug.d` | Target language code, target display name | apdk:173-182 |
| `bfug.e : List<int>` | Per-target preferred eligible caption indices | apdk:144-153 |
| `bfug.f : List<int>` | Audio indices for which this target is skipped | apdk:136-137 |
| `bfuf.f : List<int>` | Alternative source indices intersected with current audio captions; also gates the auto-translate menu | apdk:154-166, 206 |
| `bfue.f / .e / .c / .d` | Source language, VSS, timedtext URL and label data | apdk:54-71 |
| `apdi.a / .k / .m / .p` | Built track's language, VSS, URL and display label | apdi:12, 22, 24, 27; apdk:54-71 |

`apdk.g()` is the host's translation list builder. For each target
entry (`bfug`), it first checks the target's excluded audio index list,
then selects a source caption whose index is both in the target-specific
source indices and in the current audio's caption indices; if none,
it checks a fallback index list `bfuf.f` against the current audio's
caption indices. If such a source exists it constructs a native caption
using the original signed source URL plus `&tlang=<target>`.

`apdk.h()` builds the direct caption menu. The original
`AUTO_TRANSLATE_CAPTIONS_OPTION` row appears only when `bfuf.f` is
nonempty and an additional manager boolean allows it.

### Consequences for Universal Captions 3.0

**This is a better metadata-first path** than selecting the first
original caption by its language: YouTube already stores audio membership
and target-specific source candidate indices in its own native objects.

However, some field *semantics* remain unverified until their defining
classes are examined. In particular, it is **not yet proved** that
`bfuf.f` is exactly the Web `isTranslatable` flag or that all
`bfug` entries are available when the original menu is missing.

Do **not** remove the compatibility fallback or write obfuscated
field-name-dependent patches based on these two decompiled files alone.
Follow the actual repeated-field types and assignments into these
objects first; build a structural DEX resolver and synthetic regression
fixtures before attempting a runtime hook.

## Next code to inspect

From the same JADX output directory:

- `bfuf.java`: caption manager protobuf/message and list fields.
- `bfud.java`: per-audio caption membership/default selection.
- `bfue.java`: source caption metadata.
- `bfug.java`: per-target language, title, caption and audio membership.
- `apdh.java`: caption builder fields for safe copying and labeling.

This stage does **not** require running YouTube or installing an APK.
The final multi-version APK patch/device playback tests remain deferred
at the owner's request.
