#!/usr/bin/env python3
"""Guard the thin Morphe-dependent experiment against stale or incomplete inputs.

This checks packaging and metadata only. It cannot replace a clean-APK/device test.
"""
import json
import subprocess
import sys
import zipfile
from pathlib import Path

PATCH = "Universal Captions (Morphe integration)"
YOUTUBE = "com.google.android.youtube"

def source(root: Path, expected: str) -> None:
    actual = subprocess.check_output(
        ["git", "-C", str(root), "rev-parse", "HEAD"], text=True
    ).strip()
    assert actual == expected, f"Unexpected Morphe base: {actual}"
    captions = root / "patches/src/main/kotlin/app/morphe/patches/youtube/layout/captions"
    assert (captions / "CaptionsPatch.kt").exists()
    assert (captions / "AutoTranslateCaptionsPatch.kt").exists()
    assert (captions / "TranslatedCaptionPensPatch.kt").exists()
    assert (root / "extensions/youtube/src/main/java/app/morphe/extension/youtube/patches/AutoTranslateCaptionsPatch.java").exists()

def bundle(root: Path, version: str) -> None:
    data = json.loads((root / "patches-list.json").read_text())
    assert data["version"] == version, (data.get("version"), version)
    patches = [patch for patch in data["patches"] if patch["name"] == PATCH]
    assert len(patches) == 1, f"Expected one integration entry, got {len(patches)}"
    assert any(patch["name"] == "Captions" for patch in data["patches"])
    packages = patches[0]["compatiblePackages"]
    yt = [p for p in packages if p["packageName"] == YOUTUBE]
    assert len(yt) == 1
    supported = {target["version"] for target in yt[0]["targets"]}
    assert {"21.20.405", "21.41.525"}.issubset(supported), supported
    archive = root / "patches/build/libs" / f"patches-{version}.mpp"
    with zipfile.ZipFile(archive) as z:
        names = z.namelist()
        assert z.testzip() is None, "Corrupt MPP"
        assert "classes.dex" in names, "No Android DEX in MPP"
        assert "NOTICE" in names and z.read("NOTICE") == (root / "NOTICE").read_bytes()
        extensions = [name for name in names if name.startswith("extensions/") and name.endswith(".mpe")]
        assert extensions, "No Morphe extension packaged"
        assert not any("dev/itolkien/captions/extension/" in name for name in names)
    print(f"OK: {PATCH} / {version} / {len(data['patches'])} upstream patches; {len(extensions)} extensions")

if __name__ == "__main__":
    if len(sys.argv) != 4:
        sys.exit("Usage: verify.py source|bundle <morphe-checkout> <expected-sha|version>")
    try:
        if sys.argv[1] == "source":
            source(Path(sys.argv[2]), sys.argv[3])
        elif sys.argv[1] == "bundle":
            bundle(Path(sys.argv[2]), sys.argv[3])
        else:
            raise ValueError(f"Unknown mode: {sys.argv[1]}")
    except (AssertionError, OSError, KeyError, ValueError, zipfile.BadZipFile) as ex:
        sys.exit(f"Morphe integration verification failed: {ex}")
