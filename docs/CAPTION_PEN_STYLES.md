# Translated caption pen handling

> **Historical 3.1.0 experimental analysis.** The design and release checks
> below describe retained experimental code, **not** the current official
> Morphe runtime. Since [PR #3891](https://github.com/MorpheApp/morphe-patches/pull/3891)
> was merged, the maintained workflow tests official Morphe without
> replacing its production sources. See [upstream validation](UPSTREAM_VALIDATION.md).


Some translated srv3 documents inherit creator-defined pens that make
the text invisible. Universal Captions handles this in the parser instead
of trying to infer the selected track from the latest network request.

## Parser scope

The previous implementation remembered the last network request in a
process-wide flag. A late network response or cached subtitle could
then inherit another request's style.

YouTube has two synchronous decoder paths with an accessible native
caption request: a network callback and the file-cache parser.
The patch resolves these structurally and uses their actual request,
rather than the latest dispatched URL.

- `beginCaption(request)` checks the native translated-track VSS identity
  (`t.<source>.<target>` or `ta.<source>.<target>`) and its language.
  URL cloning rules are deliberately not used for subtitle rendering.
  A linked `ThreadLocal` scope isolates nested decoders without a deque.
- `getPenId()` uses the default pen for translated documents. It
  leaves original caption styling unchanged.
- `endCaption(request)` only pops the scope associated with that
  request. A native exception handler may run before the decoder's
  `beginCaption()`; request matching prevents it from closing a
  different request's scope.

The patch injects cleanup after successful parsing and into native
catch handlers that cover the parser call. An exception escaping all
those handlers is still a limitation of the host layout. Unknown or
ambiguous parser fingerprints fail patching instead of falling back
to a global flag.

No caption text, video URL or request is cached globally.

## Verification and limits

JVM tests cover original/translated documents, malformed caption URLs,
nested and overlapping parsers, reused worker threads, exception exits
and unmatched cleanup calls. Android bundle CI checks packaged hooks.

The historical 3.0 dev.7 build was tested manually on both advertised YouTube versions. The 3.1.0 candidate passed CI and received one positive but unversioned device smoke test; both versions still need itemized checks for styled originals/translations, cached captions, rapid video/audio switching and CC Off/On.

The default pen is a readability workaround, not full Web styling
parity. Some colors or highlights intentionally revert to defaults.
