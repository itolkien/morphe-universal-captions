# Translated caption pen handling (3.0.0-dev.7)

The styling fix from [Morphe PR #3837](https://github.com/MorpheApp/morphe-patches/pull/3837)
avoids invisible translated text caused by original creator-defined srv3 pens.
The prior version remembered the **last dispatched timedtext URL** in one
process-wide boolean. It could apply another video's style during concurrent
network or cached requests.

## Per-document decoder scope

Original YouTube 21.20.405 `classes2.dex` identifies two synchronous parsing
paths with access to the matching native caption request:

- **Network:** an asynchronous response handler's `run()` retains its original
  caption request while it invokes the response decoder. The decoder then reads
  the XML input stream on that same worker thread.
- **File cache:** the cached caption handler retains that same request while it
  opens a local caption file and parses the buffered input stream.

The patch finds both native decoder calls from **instruction structure**, not
hardcoded obfuscated class names. The request wrapper implements
`CaptionPenStyles.CaptionCarrier`, exposing its original caption track.
The network callback implements the same interface by retrieving its matching
request wrapper. Hooks call `beginCaption(context)` immediately before each
synchronous parser invocation and `endCaption()` immediately after and on
the native caught-exception paths.

`CaptionPenStyles` reads the **actual parsed track**, validating its VSS
target suffix and exact signed YouTube `tlang` query parameter, then stores the
result only in a **thread-local nested parse scope**. Original captions retain
creator styles; translated segments use the undeclared
`Integer.MIN_VALUE` pen ID, selecting YouTube's default caption style.
Nested decoder calls restore the outer context. Concurrent decoder workers do
not share or overwrite state. No raw URL, caption text or video ID is logged
or retained in process-global request state.

This is deliberately **not** a ThreadLocal at request construction: dispatch
and parsing can run on different threads. Source identity is obtained in the
same synchronous decoder call in which pen lookup executes.

## Safeguards and remaining limitations

- The native request wrapper must expose exactly one original caption-track
  field; ambiguous resolution fails during APK patching.
- Both parser fingerprints and the shared pen lookup must match the host
  version uniquely; do not silently fall back to a global request flag.
- A malformed/unknown caption request uses original pen IDs (fail closed).
- The native compiler's exception handlers are also hooked for cleanup;
  final validation should cover unexpected uncaught decoder exceptions.
- This is a readability-first fix; it resets **all** translated creator pens,
  which does not recreate every colored or highlighted Web caption style.
- The original 21.20.405 DEX was separately checked for unique network/cache
  method matches. The owner reports successful dev.7 playback, but an itemized
  per-APK test matrix for the new decoder hooks has not been recorded.
- The upstream Morphe `TranslatedCaptionPensPatch` and our replacement must
  not run together. The integration PR should replace the old hooks, not stack
  two copies.

## Validation

JVM tests exercise original and translated documents, malformed URL/track
identities, nested scopes, and concurrent worker threads with opposite caption
types. Build CI verifies Kotlin and Android DEX packaging. The owner reports
dev.7 working in a manual functional test. For the **public 3.0 release**,
record explicit results for each of these cases on both supported APKs,
with only Universal Captions selected:

1. Translate creator-styled MrBeast `RE3_PGwDwhw` into Arabic, Persian or Hebrew
   and confirm words remain visible.
2. Switch back to original English and verify its creator styling remains.
3. Switch videos and languages rapidly and confirm no style inheritance.
4. Reopen a previously played track from cache, confirm it is styled correctly.
5. Test Off/on, alternate audio, and the large Auto-translate menu.

No proprietary YouTube APK or DEX is committed to this repository.
