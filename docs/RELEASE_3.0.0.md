# Universal Captions 3.0.0

This is the rewrite I've been working toward. The old patch could restore Auto-translate and offer extra languages on many videos, but some still showed just 16–18 choices. Those videos often had several original caption tracks, and copying a translation from the wrong one wasn't a safe fix.

I reverse-engineered the original YouTube caption manager and followed how its metadata connects caption sources to the active audio track. In 3.0 the patch uses that native source-index routing, keeps YouTube's original translated tracks, and adds missing language choices from the correct source. It no longer depends on guessing which caption happens to be the default.

The translated subtitle styling fix has also been rewritten. Instead of using a global flag for the last network request, it identifies the caption during parsing, including cached captions. Original creator styling stays on original subtitles, while affected translations use visible default text styles.

### What's included

- A wider Auto-translate menu, typically around 155–156 languages when an eligible source exists.
- Restored Auto-translate menus for videos with original captions but no translated choices.
- Support for auto-generated, uploaded and named captions, including videos with multiple audio/caption sources.
- Native signed caption URLs and existing per-language selections preserved.
- Caption styling handling scoped to the actual XML parser rather than a process-wide request flag.
- More tests for source identity, signed URLs, multi-audio routing, regional language aliases, cache parsing and concurrent requests.

### Install or update

Download **`universal-captions-3.0.0.mpp`** or refresh this repository in Morphe Manager. Patch a clean **YouTube 21.20.405** or **21.41.525** APK and select **Universal Captions - additional translation languages**.

**Important for 2.1.0 users:** 3.0 has its own translated-caption pen and Auto-translate hooks. Do **not** apply it alongside an older Morphe **Captions** patch that already supplies those same hooks. Use the standalone Universal Captions patch until the old Morphe implementation is replaced. Repatch YouTube after updating; updating a patch source alone doesn't update an installed APK.

### Testing and limits

Both listed YouTube versions were patched and tested manually during development. The previously limited menu on YouTube 21.20.405 expanded from 17 to 156 choices, and the user confirmed that selecting Turkish showed actual translated captions. The later dev builds, including the parser-scoped styling refactor, were tested without reported problems. Automated regression tests and Android bundle checks passed.

A visible language choice is not a guarantee that YouTube's backend will translate that particular video. The patch also doesn't replace YouTube/Morphe's caption preference or visibility settings, and exact handling of every manually selected source remains subject to the native player.

The release contains the MPP bundle, generated `patches-list.json` and its SHA-256 checksum. No modified YouTube APKs or private device logs are distributed.

The patch is built using Morphe's tooling. Upstream copyright and license notices are preserved in [NOTICE](https://github.com/itolkien/universal-captions/blob/main/NOTICE).
