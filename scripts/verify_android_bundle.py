#!/usr/bin/env python3
"""Reject JVM-only or incomplete MPP artifacts before Android distribution."""

import hashlib
import zlib
import re
import struct
import sys
import zipfile


def dex_classes(data):
    if len(data) < 112 or not re.fullmatch(rb"dex\n0[0-9]{2}\x00", data[:8]):
        raise ValueError("invalid DEX header")
    if struct.unpack_from("<I", data, 32)[0] != len(data):
        raise ValueError("invalid DEX file size")

    if hashlib.sha1(data[32:]).digest() != data[12:32]:
        raise ValueError("invalid DEX SHA-1 signature")
    if zlib.adler32(data[12:]) & 0xffffffff != struct.unpack_from("<I", data, 8)[0]:
        raise ValueError("invalid DEX Adler-32 checksum")

    def uint(offset):
        return struct.unpack_from("<I", data, offset)[0]

    strings = uint(60)
    types = uint(68)
    count, definitions = uint(96), uint(100)
    result = set()
    for index in range(count):
        type_index = uint(definitions + index * 32)
        string_index = uint(types + type_index * 4)
        offset = uint(strings + string_index * 4)
        # Skip the ULEB128 UTF-16 length preceding the descriptor string.
        for _ in range(5):
            byte = data[offset]
            offset += 1
            if byte < 128:
                break
        else:
            raise ValueError("invalid DEX string length")
        end = data.index(b"\x00", offset)
        descriptor = data[offset:end].decode("utf-8", errors="surrogatepass")
        if not descriptor.startswith("L") or not descriptor.endswith(";"):
            raise ValueError("invalid DEX class descriptor")
        result.add(descriptor[1:-1])
    return result


def verify(path):
    with zipfile.ZipFile(path) as bundle:
        names = bundle.namelist()
        if len(names) != len(set(names)) or bundle.testzip() is not None:
            raise ValueError("duplicate or corrupt ZIP entries")
        if "NOTICE" not in names or not bundle.read("NOTICE").startswith(b"Morphe NOTICE\n"):
            raise ValueError("missing original Morphe GPLv3 Section 7 user notice")
        if "classes.dex" not in names:
            raise ValueError("missing root classes.dex: this bundle cannot load patches on Android")
        dex = set()
        for name in names:
            if re.fullmatch(r"classes(?:[2-9]|[1-9][0-9]+)?\.dex", name):
                classes = dex_classes(bundle.read(name))
                if dex.intersection(classes):
                    raise ValueError("duplicate DEX class definitions")
                dex.update(classes)
        expected = {name[:-6] for name in names if name.endswith(".class")
                    and not name.startswith("META-INF/") and name != "module-info.class"}
        if not expected or not dex:
            raise ValueError("empty patch code")
        missing = expected - dex
        if missing:
            raise ValueError(f"{len(missing)} JVM classes missing from Android DEX: {sorted(missing)[:5]}")
        extension = bundle.read("extensions/extension.mpe")
        if b"UC3Probe" in extension or b"setTimedTextUrl" in extension:
            raise ValueError("stale global caption request/diagnostic hook in Android extension")
        if b"beginCaption" not in extension or b"endCaption" not in extension:
            raise ValueError("missing scoped native caption decoder hooks")
        runtime = dex_classes(extension)
        required = {"CaptionTranslationList", "CaptionTranslationList$CaptionTrack",
                    "OriginalCaptionSource", "EmptyTranslationFallback", "EmptyTranslationFallback$CaptionSource",
                    "NativeTranslationBridge", "CaptionPenStyles",
                    "CaptionPenStyles$CaptionCarrier", "TimedTextUrl"}
        missing_runtime = {"dev/itolkien/captions/extension/" + name for name in required} - runtime
        if missing_runtime:
            raise ValueError(f"missing extension runtime classes: {sorted(missing_runtime)}")
        if any(name.startswith("app/morphe/extension/youtube/patches/") for name in runtime):
            raise ValueError("native Morphe caption implementation must not be bundled")
        print(f"{path}: Android bundle verified ({len(expected)} JVM classes, {len(dex)} DEX classes)")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit("Usage: verify_android_bundle.py <bundle.mpp> [...]")
    try:
        for path in sys.argv[1:]:
            verify(path)
    except (ValueError, IndexError, struct.error, zipfile.BadZipFile, OSError) as error:
        sys.exit(f"Android bundle verification failed: {error}")
