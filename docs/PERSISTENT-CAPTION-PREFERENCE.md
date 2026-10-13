# Persistent caption language preference

## User behavior

A user selects a caption language once. That language remains the preferred target across video changes and is independent of the video's original spoken language.

For every new video, prefer:

1. An existing **manual track in the preferred target language**, played directly with no translation.
2. Otherwise, a **manual source track** translated by YouTube into the preferred target language. Prefer the video's original spoken language where it can be identified reliably; do not arbitrarily prefer English.
3. Otherwise, an **automatic speech recognition (ASR) track** translated by YouTube into the preferred target language, preferably matching the original spoken language.
4. Otherwise, leave subtitles off/unavailable. Never fabricate text or pretend translation succeeded.

Use the same policy for every supported language (normalize BCP-47 tags and avoid duplicate tracks).

## Intent and state requirements

- Record explicit user caption-language selection in durable app preferences.
- When the user explicitly turns captions **Off**, keep them off across video changes until they explicitly enable captions. Never override an explicit Off.
- Do not confuse system-selected/default caption tracks with deliberate user choices.
- Avoid switching back repeatedly while the user manually changes caption tracks on a particular video; explicit user selections take precedence.
- On a new video, wait until its caption model is ready and apply a preferred target only once for that video/model revision, preventing flicker and selection loops.
- Keep state scoped to video identity. Switching videos must not reuse the prior video's track URL or signing tokens.
- Selecting a native manual track for the requested language is preferred over adding another translated entry to a menu.
- Use YouTube's native player API to commit selected tracks. Editing only the auto-translate options list is **not enough** to implement persistent selection.
- Account for locale aliases (for example `iw`/`he`, `in`/`id`), region variants and explicit user preference before attempting broad primary-language fallback.

## Current branch state

The branch now contains a **pure Java selection policy** in `src/main/java/dev/itolkien/captions/CaptionResolver.java`, with deterministic policy tests. It prioritizes target-language manual and ASR tracks, then original-language manual and ASR tracks, then other manual and ASR tracks. Explicit Off yields no selection. The code is **not yet integrated with the YouTube player**; preference persistence, automatic per-video application, and explicit Off restoration need a version-validated selection lifecycle hook and device testing. The current installed experimental `.mpp` does not automatically remember and apply a preferred language.

## Acceptance tests

1. Select Turkish; next video has manual Turkish: select it directly.
2. Next video has no Turkish but has manual Japanese: translate Japanese to Turkish.
3. Next video only has Japanese ASR: translate that to Turkish.
4. Next video has no tracks: do not display invented subtitles.
5. Repeat cases 1–4 with German and Japanese as user-selected targets.
6. Disable subtitles explicitly; verify next videos remain off.
7. Change target while watching a video; manual selection must win.
8. Rapidly switch between two videos; no stale track URL, flickering or selection loops.
9. Verify automatic YouTube player selections do not overwrite explicit saved target.
10. Compare regression cases with the published main branch before merge.

## Integration safety gate

`CaptionPreferenceStore` now implements durable Android preference storage, but **it has no native selection callback wired yet**. The resolver is a pure-Java module, and neither class is enough to provide video-to-video persistence in an MPP. A correct build must hook the actual YouTube 21.20.405 explicit selection dispatcher, distinguish explicit Off from automatic selection, hook the model-ready/default selection path, and bind selection against the current video's model. Until this is implemented and device-tested, do not label any generated MPP as containing persistent automatic selection.

## Native-hook prototype (October 2026)

The experiment now attempts to hook the YouTube 21.20.405 native manual selection entry point and native default caption selector. Explicit target language and Off are persisted by `CaptionPreferenceStore`; a model-ready default attempts a matching manual/ASR track first, then a generated translated target. This is a **device-test prototype**, not a proven complete implementation of the seven-stage policy. It does not yet feed the pure `CaptionResolver` with a reliably verified original spoken language, and the callback's classification of explicit intent must be confirmed on-device. If the exact bytecode signatures cannot be matched, patching must fail rather than silently delivering a partial feature. Keep the public main release unchanged.
