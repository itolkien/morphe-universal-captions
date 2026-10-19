# Styled auto-translated captions (3.0.0 development)

This repository is the development source for Universal Captions, including the
styled-caption fix first merged as [Morphe PR #3837](https://github.com/MorpheApp/morphe-patches/pull/3837).

## Problem

Some YouTube channels use styled subtitle pen IDs for invisible padding.
YouTube auto-translation can move those IDs onto visible words, making translated
text black or invisible even though its caption data is present.

## Implementation

- `CaptionPenHooks.kt` independently resolves the Cronet timedtext URL builder
  and the srv3 segment parser's shared pen-lookup method. Both hooks are
  resolved before patching so an incompatible host layout fails visibly.
- `CaptionPenStyles.java` remembers whether the most recent valid YouTube
  timedtext request has a raw `tlang=` query parameter; it does not store
  or log signed URLs.
- For auto-translated tracks the shared pen lookup receives an undeclared ID
  (`Integer.MIN_VALUE`), yielding YouTube's default caption pen. Original
  caption tracks retain their native creator-defined pen IDs.
- The fix is independent of Morphe's own extension class. When porting it to
  Morphe, remove the old `TranslatedCaptionPensPatch` hooks rather than
  applying both copies.

## Web-style behavior versus this fallback

Web can apply or adapt styling for the translated caption document. Our
current workaround returns a default pen ID for **all** translated styles,
which fixes invisible subtitles but may also discard legitimate highlighting
or creator colors on translated words. Restoring those styles accurately
requires reading each document's pen definitions and associating them with
its translated segments. Numeric pen IDs are scoped to the document, so a
global "bad pen id" list would be incorrect.

## Safety and limitations

The request URL and the srv3 XML parser do not share a demonstrated per-document
identifier at these hook points. Consequently a process-wide volatile flag
matches Morphe #3837's proven behavior but **is not concurrency-safe**:
overlapping requests or cached captions may cause the last request's style
decision to be applied to a different document. `ThreadLocal` is not a fix,
because the network builder and parser may run on different threads.

The helper ignores unrelated URLs and accepts only the YouTube HTTPS timedtext
endpoint. It inspects the raw query, not fragments or opaque values, and
does not rewrite the signed URL.

## Verification

The JVM tests in `CaptionPenStylesTest` cover translated/original pen behavior,
query and fragment handling, unrelated requests and resets. The Android MPP
build checks hook compilation and inclusion of the new runtime class.

Still required before an upstream replacement or release:

1. Patch clean target APKs (21.20.405 and 21.41.525), validate DEX and confirm both fingerprints resolve.
2. On device, test MrBeast video `RE3_PGwDwhw` translated into Hebrew,
   Arabic and Persian, then switch back to original styled English captions.
3. Test rapid video, language and source switching and overlapping requests.
4. Verify coexistence with installed Morphe's Captions patch or test the
   planned clean replacement that removes the duplicate #3837 hooks.
