#!/usr/bin/env python3
"""Historical 3.1.0 overlay verifier. Not used by upstream-caption CI."""
import json
import sys
from pathlib import Path
from zipfile import ZipFile

def verify(root: Path, version: str) -> None:
    info = json.loads((root / "patches-list.json").read_text())
    assert info["version"] == version, (info.get("version"), version)
    assert any(p["name"] == "Captions" for p in info["patches"]), "Morphe captions patch missing"
    # This branch maintains the implementation, not a 19-line alias/second hook.
    assert not any(p["name"] == "Universal Captions (Morphe integration)"
                   for p in info["patches"]), "Duplicate alias must not be packaged"
    captions = next(p for p in info["patches"] if p["name"] == "Captions")
    youtube = next(x for x in captions["compatiblePackages"]
                   if x["packageName"] == "com.google.android.youtube")
    versions = {v["version"] for v in youtube["targets"]}
    assert {"21.20.405", "21.41.525"} <= versions, versions

    parser = (root / "patches/src/main/kotlin/app/morphe/patches/youtube/layout/captions/TranslatedCaptionPensPatch.kt").read_text()
    runtime = (root / "extensions/youtube/src/main/java/app/morphe/extension/youtube/patches/TranslatedCaptionPensPatch.java").read_text()
    assert "network.method.scopeDecoder(" in parser and "cache.method.scopeDecoder(" in parser
    assert "beginCaption(Ljava/lang/Object;)" in parser and "endCaption(Ljava/lang/Object;)" in parser
    assert "beginCaption(" in runtime and "endCaption(" in runtime
    assert "setParsedRequest(" not in runtime, "Old unscoped parser state unexpectedly restored"
    patch = (root / "patches/src/main/kotlin/app/morphe/patches/youtube/layout/captions/AutoTranslateCaptionsPatch.kt").read_text()
    # Use the full Web language snapshot; the Android native-target catalog ABI is deliberately absent.
    assert "resolveNativeTargets(" not in patch and "patch_getNativeTargetLanguages" not in patch, \
        "Removed native target-language resolver was reintroduced"
    assert "patch_getActiveTranslationSource" in patch
    assert "getTranslationCount(ILjava/util/List;$CAPTION_MANAGER_INTERFACE)I" in patch
    assert "findInstructionIndicesReversedOrThrow" not in patch, "Old duplicate native hook reintroduced"
    path = root / "patches/build/libs" / f"patches-{version}.mpp"
    with ZipFile(path) as bundle:
        names = bundle.namelist()
        assert len(names) == len(set(names)), "Duplicate archive names"
        assert bundle.testzip() is None, "Corrupted Android patch bundle"
        assert "classes.dex" in names, "Missing Android classes.dex"
        assert bundle.read("NOTICE") == (root / "NOTICE").read_bytes(), "Missing upstream notice"
        assert any(p.startswith("extensions/") and p.endswith(".mpe") for p in names), "Missing Morphe extension"
    print("Validated Android test MPP and Morphe caption metadata; clean-APK smoke tests still required.")

if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit("Usage: verify_morphe_bundle.py MORPHE_CHECKOUT VERSION")
    try:
        verify(Path(sys.argv[1]), sys.argv[2])
    except (AssertionError, ValueError, OSError, KeyError, StopIteration) as e:
        sys.exit(f"Bundle verification failed: {e}")
